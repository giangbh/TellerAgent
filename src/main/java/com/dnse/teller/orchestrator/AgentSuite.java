package com.dnse.teller.orchestrator;

import com.dnse.teller.mcp.McpServer;
import com.dnse.teller.model.*;
import com.dnse.teller.model.PolicyFinding.Citation;
import com.dnse.teller.model.TransactionDraft.ValidationResult;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgentSuite {
    private final McpServer mcpServer;

    public AgentSuite(McpServer mcpServer) {
        this.mcpServer = mcpServer;
    }

    public AgentResult run(String agent, Session session) throws Exception {
        if (agent.startsWith("dynamic_tool_agent:")) {
            String capabilityId = agent.substring("dynamic_tool_agent:".length());
            return runDynamicTool(capabilityId, session);
        }

        return switch (agent) {
            case "customer_context_agent" -> runCustomerContext(session);
            case "policy_agent" -> runPolicy(session);
            case "transaction_draft_agent" -> runTransactionDraft(session);
            case "risk_assistant" -> runRisk(session);
            default -> runDynamicTool(agent, session);
        };
    }

    @SuppressWarnings("unchecked")
    private AgentResult runDynamicTool(String capabilityId, Session session) throws Exception {
        Map<String, Object> entities = session.getIntent() != null ? session.getIntent().getEntities() : Map.of();
        Map<String, Object> args = new LinkedHashMap<>();
        String customerRef = entities.get("customerRef") != null ? String.valueOf(entities.get("customerRef"))
            : (session.getCustomerRef() != null ? session.getCustomerRef() : "CIF-0001842");

        // Map relevant entities to args based on capability
        if ("fx.rate.lookup".equals(capabilityId)) {
            args.put("currency", entities.getOrDefault("currency", "USD"));
            if (entities.get("amount") != null) args.put("amount", entities.get("amount"));
        } else if ("branch.directory.lookup".equals(capabilityId)) {
            args.put("city", entities.getOrDefault("city", "Hà Nội"));
        } else if ("customer.accounts.summary".equals(capabilityId) || "customer.profile.read".equals(capabilityId)
                || "customer.accounts.list".equals(capabilityId) || "customer.persona.analytics".equals(capabilityId)
                || "customer.credit.score.check".equals(capabilityId) || "recommendation.nbo.products".equals(capabilityId)
                || "card.service.manage".equals(capabilityId)) {
            args.put("customerRef", customerRef);
        } else if ("savings.product.advisor".equals(capabilityId)) {
            args.put("amount", entities.getOrDefault("amount", 100_000_000L));
            args.put("termMonths", entities.getOrDefault("termMonths", 6));
        } else if ("statement.transaction.history".equals(capabilityId)) {
            args.put("accountNumber", entities.getOrDefault("accountNumber", entities.getOrDefault("beneficiaryAccount", "3456789")));
            args.put("customerRef", customerRef);
        } else if ("pricing.transfer.fee".equals(capabilityId)) {
            args.put("amount", entities.getOrDefault("amount", 50_000_000L));
        } else if ("transfer.limit.check".equals(capabilityId)) {
            args.put("amount", entities.getOrDefault("amount", 50_000_000L));
        } else if ("bank.directory.lookup".equals(capabilityId)) {
            args.put("bankCode", entities.getOrDefault("bankCode", "VCB"));
        } else if ("knowledge.policy.search".equals(capabilityId)) {
            args.put("query", session.getIntent() != null ? session.getIntent().getQuery() : "quy trình");
        } else {
            args.putAll(entities);
            if (!args.containsKey("customerRef")) args.put("customerRef", customerRef);
        }

        Map<String, Object> call = mcpServer.executeDirect(
            "dynamic_tool_agent",
            session.getWorkflow() != null ? session.getWorkflow() : "dynamic_autonomous",
            capabilityId,
            args,
            null
        );

        Map<String, Object> res = (Map<String, Object>) call.get("result");
        String summary = res != null && res.get("summary") != null ? String.valueOf(res.get("summary")) : "Đã thực thi " + capabilityId;

        PolicyFinding finding = new PolicyFinding();
        finding.setAnswer(summary);

        Map<String, Object> writes = new LinkedHashMap<>();
        writes.put("policyFindings", finding);
        writes.put("dynamic_" + capabilityId.replace('.', '_'), res);

        return new AgentResult(
            "dynamic_tool_agent",
            writes,
            List.of("DYNAMIC-MCP-" + capabilityId.toUpperCase()),
            List.of(call)
        );
    }

    @SuppressWarnings("unchecked")
    private AgentResult runCustomerContext(Session session) throws Exception {
        Map<String, Object> entities = session.getIntent() != null ? session.getIntent().getEntities() : Map.of();
        String customerRef = entities.get("customerRef") != null ? String.valueOf(entities.get("customerRef"))
            : (session.getCustomerRef() != null ? session.getCustomerRef() : "CIF-0001842");

        Map<String, Object> profileCall = mcpServer.executeDirect(
            "customer_context_agent",
            session.getWorkflow(),
            "customer.profile.read",
            Map.of("customerRef", customerRef),
            null
        );

        List<Map<String, Object>> toolCalls = new ArrayList<>();
        toolCalls.add(profileCall);

        Map<String, Object> contextData = new LinkedHashMap<>((Map<String, Object>) profileCall.get("result"));

        if ("DOMESTIC_TRANSFER".equals(session.getIntent().getType())) {
            Map<String, Object> accountCall = mcpServer.executeDirect(
                "customer_context_agent",
                session.getWorkflow(),
                "customer.accounts.list",
                Map.of("customerRef", customerRef),
                null
            );
            toolCalls.add(accountCall);
            Map<String, Object> accResult = (Map<String, Object>) accountCall.get("result");
            if (accResult != null && accResult.containsKey("accounts")) {
                contextData.put("accounts", accResult.get("accounts"));
            }
        }

        return new AgentResult(
            "customer_context_agent",
            Map.of("customerContext", contextData),
            List.of("MOCK-CIF-SNAPSHOT"),
            toolCalls
        );
    }

    @SuppressWarnings("unchecked")
    private AgentResult runPolicy(Session session) throws Exception {
        String cashQuery = "CASH_DEPOSIT".equals(session.getIntent().getType())
            ? "quy trình nộp tiền mặt vào tài khoản tại quầy"
            : "quy trình rút tiền mặt từ tài khoản tại quầy";

        String query = session.getIntent().getQuery() != null ? session.getIntent().getQuery()
            : (session.getWorkflow().startsWith("cash_") ? cashQuery : "điều kiện và phí chuyển khoản tại quầy");

        Map<String, Object> policyCall = mcpServer.executeDirect(
            "policy_agent",
            session.getWorkflow(),
            "knowledge.policy.search",
            Map.of("query", query),
            null
        );

        List<Map<String, Object>> toolCalls = new ArrayList<>();
        toolCalls.add(policyCall);

        Map<String, Object> policyResult = (Map<String, Object>) policyCall.get("result");
        Object fee = null;

        Object amountObj = session.getIntent().getEntities().get("amount");
        if ("DOMESTIC_TRANSFER".equals(session.getIntent().getType()) && amountObj != null) {
            Map<String, Object> feeCall = mcpServer.executeDirect(
                "policy_agent",
                session.getWorkflow(),
                "pricing.transfer.fee",
                Map.of("amount", amountObj),
                null
            );
            toolCalls.add(feeCall);
            fee = feeCall.get("result");
        }

        PolicyFinding finding = new PolicyFinding();
        finding.setAnswer(String.valueOf(policyResult.get("answer")));
        finding.setFee(fee);

        List<Citation> citations = new ArrayList<>();
        List<String> evidence = new ArrayList<>();
        if (policyResult.get("citations") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Citation c = new Citation(
                        String.valueOf(m.get("document")),
                        String.valueOf(m.get("section")),
                        String.valueOf(m.get("title")),
                        String.valueOf(m.get("effectiveDate"))
                    );
                    citations.add(c);
                    evidence.add(c.getDocument() + "#" + c.getSection());
                }
            }
        }
        finding.setCitations(citations);

        return new AgentResult(
            "policy_agent",
            Map.of("policyFindings", finding),
            evidence,
            toolCalls
        );
    }

    @SuppressWarnings("unchecked")
    private AgentResult runTransactionDraft(Session session) throws Exception {
        if (session.getWorkflow() != null && session.getWorkflow().startsWith("cash_")) {
            return runCashDraft(session);
        }

        Map<String, Object> entities = session.getIntent().getEntities();
        List<Map<String, Object>> toolCalls = new ArrayList<>();

        String bankCode = entities.get("bankCode") != null ? String.valueOf(entities.get("bankCode")) : null;
        String bankName = null;
        if (bankCode != null) {
            Map<String, Object> bankCall = mcpServer.executeDirect(
                "transaction_draft_agent",
                session.getWorkflow(),
                "bank.directory.lookup",
                Map.of("bankCode", bankCode),
                null
            );
            toolCalls.add(bankCall);
            Map<String, Object> bankRes = (Map<String, Object>) bankCall.get("result");
            if (bankRes != null) bankName = (String) bankRes.get("bankName");
        }

        Long amount = entities.get("amount") instanceof Number ? ((Number) entities.get("amount")).longValue() : null;
        Map<String, Object> limitResult = null;
        if (amount != null) {
            Map<String, Object> limitCall = mcpServer.executeDirect(
                "transaction_draft_agent",
                session.getWorkflow(),
                "transfer.limit.check",
                Map.of("amount", amount),
                null
            );
            toolCalls.add(limitCall);
            limitResult = (Map<String, Object>) limitCall.get("result");
        }

        String sourceAccountRef = null;
        String sourceAccountMasked = null;
        if (session.getCustomerContext().get("accounts") instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map) {
                sourceAccountRef = (String) map.get("accountRef");
                sourceAccountMasked = (String) map.get("accountNoMasked");
            }
        }

        TransactionDraft draft = new TransactionDraft();
        draft.setSourceAccountRef(sourceAccountRef);
        draft.setSourceAccountMasked(sourceAccountMasked);
        draft.setBeneficiaryAccount((String) entities.get("beneficiaryAccount"));
        draft.setBeneficiaryName((String) entities.get("beneficiaryName"));
        draft.setBankCode(bankCode);
        draft.setBankName(bankName);
        draft.setAmount(amount);
        draft.setFee(session.getPolicyFindings() != null ? session.getPolicyFindings().getFee() : null);
        draft.setLimit(limitResult);
        draft.setCurrency("VND");

        Map<String, Object> draftMap = new HashMap<>();
        if (draft.getSourceAccountRef() != null) draftMap.put("sourceAccountRef", draft.getSourceAccountRef());
        if (draft.getBeneficiaryAccount() != null) draftMap.put("beneficiaryAccount", draft.getBeneficiaryAccount());
        if (draft.getBeneficiaryName() != null) draftMap.put("beneficiaryName", draft.getBeneficiaryName());
        if (draft.getBankCode() != null) draftMap.put("bankCode", draft.getBankCode());
        if (draft.getAmount() != null) draftMap.put("amount", draft.getAmount());

        Map<String, Object> validateCall = mcpServer.executeDirect(
            "transaction_draft_agent",
            session.getWorkflow(),
            "transaction.draft.validate",
            draftMap,
            null
        );
        toolCalls.add(validateCall);

        Map<String, Object> valRes = (Map<String, Object>) validateCall.get("result");
        ValidationResult validation = new ValidationResult(
            (Boolean) valRes.get("valid"),
            (List<String>) valRes.get("missingFields"),
            (List<String>) valRes.get("blockers")
        );
        draft.setValidation(validation);

        return new AgentResult(
            "transaction_draft_agent",
            Map.of("transactionDraft", draft),
            List.of("TRANSACTION-DRAFT-SCHEMA-V2"),
            toolCalls
        );
    }

    @SuppressWarnings("unchecked")
    private AgentResult runCashDraft(Session session) throws Exception {
        Map<String, Object> entities = session.getIntent().getEntities();
        List<Map<String, Object>> toolCalls = new ArrayList<>();

        String accountNumber = (String) entities.get("accountNumber");
        Map<String, Object> accRes = null;
        if (accountNumber != null) {
            Map<String, Object> accCall = mcpServer.executeDirect(
                "transaction_draft_agent",
                session.getWorkflow(),
                "account.resolve.by_number",
                Map.of("accountNumber", accountNumber),
                null
            );
            toolCalls.add(accCall);
            accRes = (Map<String, Object>) accCall.get("result");
        }

        String transactionType = session.getIntent().getType();
        Long amount = entities.get("amount") instanceof Number ? ((Number) entities.get("amount")).longValue() : null;
        Long availableBalance = accRes != null && accRes.get("availableBalance") instanceof Number
            ? ((Number) accRes.get("availableBalance")).longValue() : null;

        Map<String, Object> limitResult = null;
        if (amount != null) {
            Map<String, Object> limitArgs = new HashMap<>();
            limitArgs.put("amount", amount);
            if (availableBalance != null) limitArgs.put("availableBalance", availableBalance);
            limitArgs.put("transactionType", transactionType);

            Map<String, Object> limitCall = mcpServer.executeDirect(
                "transaction_draft_agent",
                session.getWorkflow(),
                "cash.limit.check",
                limitArgs,
                null
            );
            toolCalls.add(limitCall);
            limitResult = (Map<String, Object>) limitCall.get("result");
        }

        TransactionDraft draft = new TransactionDraft();
        boolean isDeposit = "CASH_DEPOSIT".equals(transactionType);
        draft.setScreenCode(isDeposit ? "CASH_DEPOSIT_SCREEN" : "CASH_WITHDRAWAL_SCREEN");
        draft.setScreenTitle(isDeposit ? "Màn hình Nộp tiền" : "Màn hình Rút tiền");
        draft.setTransactionType(transactionType);
        draft.setAccountRef(accRes != null ? (String) accRes.get("accountRef") : null);
        draft.setAccountNumber(accountNumber);
        draft.setAccountNoMasked(accRes != null ? (String) accRes.get("accountNoMasked") : null);
        draft.setAccountHolder(accRes != null ? (String) accRes.get("accountHolder") : null);
        draft.setAccountStatus(accRes != null ? (String) accRes.get("status") : null);
        draft.setAvailableBalance(availableBalance);
        draft.setAmount(amount);
        draft.setCurrency("VND");
        draft.setDescription(isDeposit ? "Nộp tiền mặt tại quầy" : "Rút tiền mặt tại quầy");
        draft.setLimit(limitResult);

        Map<String, Object> draftMap = new HashMap<>();
        if (draft.getAccountRef() != null) draftMap.put("accountRef", draft.getAccountRef());
        if (draft.getAccountNumber() != null) draftMap.put("accountNumber", draft.getAccountNumber());
        if (draft.getAmount() != null) draftMap.put("amount", draft.getAmount());
        if (draft.getTransactionType() != null) draftMap.put("transactionType", draft.getTransactionType());
        if (draft.getAccountStatus() != null) draftMap.put("accountStatus", draft.getAccountStatus());
        if (draft.getLimit() != null) draftMap.put("limit", draft.getLimit());

        Map<String, Object> valCall = mcpServer.executeDirect(
            "transaction_draft_agent",
            session.getWorkflow(),
            "cash.draft.validate",
            draftMap,
            null
        );
        toolCalls.add(valCall);

        Map<String, Object> valRes = (Map<String, Object>) valCall.get("result");
        ValidationResult validation = new ValidationResult(
            (Boolean) valRes.get("valid"),
            (List<String>) valRes.get("missingFields"),
            (List<String>) valRes.get("blockers")
        );
        draft.setValidation(validation);

        return new AgentResult(
            "transaction_draft_agent",
            Map.of("transactionDraft", draft),
            List.of("CASH-TRANSACTION-DRAFT-SCHEMA-V1"),
            toolCalls
        );
    }

    @SuppressWarnings("unchecked")
    private AgentResult runRisk(Session session) throws Exception {
        String capabilityId = session.getWorkflow().startsWith("cash_") ? "risk.cash.screen" : "risk.transfer.screen";

        Map<String, Object> riskArgs = new HashMap<>();
        if (session.getTransactionDraft().getAmount() != null) {
            riskArgs.put("amount", session.getTransactionDraft().getAmount());
        }
        if (session.getTransactionDraft().getBeneficiaryAccount() != null) {
            riskArgs.put("beneficiaryAccount", session.getTransactionDraft().getBeneficiaryAccount());
        }
        if (session.getTransactionDraft().getLimit() != null) {
            riskArgs.put("limit", session.getTransactionDraft().getLimit());
        }

        Map<String, Object> call = mcpServer.executeDirect(
            "risk_assistant",
            session.getWorkflow(),
            capabilityId,
            riskArgs,
            null
        );

        Map<String, Object> res = (Map<String, Object>) call.get("result");
        RiskAssessment risk = new RiskAssessment(
            (String) res.get("decision"),
            (List<String>) res.get("alerts"),
            (String) res.get("model")
        );

        return new AgentResult(
            "risk_assistant",
            Map.of("risk", risk),
            List.of(risk.getModel() != null ? risk.getModel() : "MOCK_RULESET"),
            List.of(call)
        );
    }

    public static class AgentResult {
        private final String agent;
        private final Map<String, Object> writes;
        private final List<String> evidence;
        private final List<Map<String, Object>> toolCalls;

        public AgentResult(String agent, Map<String, Object> writes, List<String> evidence, List<Map<String, Object>> toolCalls) {
            this.agent = agent;
            this.writes = writes;
            this.evidence = evidence;
            this.toolCalls = toolCalls;
        }

        public String getAgent() { return agent; }
        public Map<String, Object> getWrites() { return writes; }
        public List<String> getEvidence() { return evidence; }
        public List<Map<String, Object>> getToolCalls() { return toolCalls; }
    }
}
