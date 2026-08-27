'use strict';

const crypto = require('crypto');
const { AgentSuite, MockReasoningEngine } = require('./agents');
const { MockToolGateway } = require('./mock-tools');
const { listCapabilities } = require('./capability-registry');

const AGENT_OWNERSHIP = {
  customer_context_agent: ['customerContext'],
  policy_agent: ['policyFindings'],
  transaction_draft_agent: ['transactionDraft'],
  risk_assistant: ['risk'],
};

const ALLOWED_DELEGATIONS = {
  policy_assistance: ['policy_agent'],
  domestic_transfer: ['customer_context_agent', 'policy_agent', 'transaction_draft_agent', 'risk_assistant'],
  cash_deposit: ['customer_context_agent', 'policy_agent', 'transaction_draft_agent', 'risk_assistant'],
  cash_withdrawal: ['customer_context_agent', 'policy_agent', 'transaction_draft_agent', 'risk_assistant'],
};

class OrchestratorError extends Error {
  constructor(message, code = 'ORCHESTRATOR_ERROR', status = 400) {
    super(message);
    this.name = 'OrchestratorError';
    this.code = code;
    this.status = status;
  }
}

class TellerOrchestrator {
  constructor({ toolGateway = new MockToolGateway(), reasoning = new MockReasoningEngine() } = {}) {
    this.toolGateway = toolGateway;
    this.reasoning = reasoning;
    this.agents = new AgentSuite(toolGateway);
    this.sessions = new Map();
  }

  createSession(input = {}) {
    const now = new Date().toISOString();
    const session = {
      sessionId: crypto.randomUUID(),
      transactionId: null,
      revision: 1,
      createdAt: now,
      updatedAt: now,
      branchId: input.branchId || 'CN-SGD-01',
      counterId: input.counterId || 'Q-07',
      tellerId: input.tellerId || 'TELLER-1024',
      customerRef: input.customerRef || 'CIF-0001842',
      workflow: null,
      workflowVersion: null,
      status: 'SESSION_OPEN',
      intent: null,
      messages: [],
      customerContext: {},
      policyFindings: {},
      transactionDraft: {},
      risk: {},
      approvals: { customer: false, teller: false, supervisor: false },
      control: { postingAllowed: false, missingGates: [], lastEvaluatedAt: null },
      execution: null,
      agentRuntime: {
        reasoningMode: 'MOCK_STRUCTURED_PLANNER',
        plan: null,
        completedSteps: [],
        delegations: [],
        toolCalls: [],
        budget: { maxDelegations: 6, maxToolCalls: 16, delegationsUsed: 0, toolCallsUsed: 0 },
      },
      events: [],
    };
    this.#event(session, 'SESSION', 'Mở phiên B.Smart', `${session.branchId} · ${session.counterId}`);
    this.sessions.set(session.sessionId, session);
    return clone(session);
  }

  getSession(sessionId) {
    const session = this.sessions.get(sessionId);
    if (!session) throw new OrchestratorError('Không tìm thấy phiên.', 'SESSION_NOT_FOUND', 404);
    return clone(session);
  }

  async processMessage(sessionId, text) {
    const session = this.#session(sessionId);
    if (!text || !String(text).trim()) throw new OrchestratorError('Nội dung yêu cầu không được để trống.', 'EMPTY_MESSAGE');
    if (session.status === 'POSTED') throw new OrchestratorError('Giao dịch đã được posting. Hãy mở phiên mới.', 'SESSION_ALREADY_POSTED');

    session.messages.push({ role: 'user', text: String(text).trim(), at: new Date().toISOString() });
    this.#event(session, 'INPUT', 'Teller cung cấp yêu cầu', String(text).trim());

    session.intent = this.reasoning.detectIntent(String(text), session.intent);
    session.workflow = session.intent.workflow;
    session.workflowVersion = session.workflow === 'domestic_transfer'
      ? '3.2-poc'
      : session.workflow.startsWith('cash_') ? '1.0-poc' : '1.0-poc';
    session.status = 'INTENT_DETECTED';
    this.#event(session, 'AGENT', 'Intent Agent xác định ý định', `${session.intent.type} · confidence ${session.intent.confidence}`);

    const plan = this.reasoning.proposePlan(session);
    this.#validatePlan(session, plan);
    session.agentRuntime.plan = plan;
    session.agentRuntime.completedSteps = [];
    session.agentRuntime.delegations = [];
    session.agentRuntime.toolCalls = [];
    session.agentRuntime.budget.delegationsUsed = 0;
    session.agentRuntime.budget.toolCallsUsed = 0;
    this.#event(session, 'PLAN', 'Structured Planner lập kế hoạch', `${plan.steps.length} bước · ${plan.objective}`);

    if (session.workflow === 'policy_assistance') {
      const result = await this.#delegate(session, 'policy_agent', 'S1');
      this.#mergePatch(session, result);
      session.status = 'ASSISTANCE_READY';
      session.messages.push({ role: 'assistant', text: session.policyFindings.answer, at: new Date().toISOString() });
      this.#event(session, 'RESULT', 'Hoàn thành hỗ trợ nghiệp vụ', 'Câu trả lời có citation mock.');
      return this.#save(session);
    }

    // Evidence fan-out: hai agent độc lập chạy song song, sau đó mới mở barrier.
    const [customerResult, policyResult] = await Promise.all([
      this.#delegate(session, 'customer_context_agent', 'S1'),
      this.#delegate(session, 'policy_agent', 'S2'),
    ]);
    this.#mergePatch(session, customerResult);
    this.#mergePatch(session, policyResult);
    this.#event(session, 'BARRIER', 'Evidence Barrier đã mở', 'Customer context và policy evidence đã sẵn sàng.');

    const draftResult = await this.#delegate(session, 'transaction_draft_agent', 'S3');
    this.#mergePatch(session, draftResult);
    session.approvals = { customer: false, teller: false, supervisor: false };

    const missing = session.transactionDraft.validation?.missingFields || [];
    if (missing.length) {
      session.status = 'WAITING_HUMAN_INPUT';
      session.control = { postingAllowed: false, missingGates: missing.map(item => `FIELD:${item}`), lastEvaluatedAt: new Date().toISOString() };
      session.messages.push({ role: 'assistant', text: humanizeMissingFields(missing), at: new Date().toISOString() });
      this.#event(session, 'INTERRUPT', 'Cần Teller bổ sung thông tin', missing.join(', '));
      return this.#save(session);
    }

    const riskResult = await this.#delegate(session, 'risk_assistant', 'S4');
    this.#mergePatch(session, riskResult);
    session.status = session.risk.decision === 'PASS' ? 'DRAFT_READY' : 'RISK_REVIEW';
    session.messages.push({
      role: 'assistant',
      text: session.status === 'DRAFT_READY'
        ? session.workflow.startsWith('cash_')
          ? `${session.transactionDraft.screenTitle} đã được mở và tự điền. GDV kiểm tra lại rồi bấm Xác nhận & Submit Mock.`
          : 'Bản nháp đã hoàn tất. Vui lòng cho khách hàng và Teller kiểm tra trước khi gửi core mock.'
        : 'Giao dịch cần kiểm soát viên rà soát cảnh báo mock trước khi tiếp tục.',
      at: new Date().toISOString(),
    });
    this.#evaluateControl(session);
    this.#event(session, 'RESULT', 'Hoàn thành bản nháp', `${session.status} · không có posting tự động.`);
    return this.#save(session);
  }

  approve(sessionId, actor) {
    const session = this.#session(sessionId);
    if (!['DRAFT_READY', 'AWAITING_APPROVAL', 'READY_TO_POST'].includes(session.status)) {
      throw new OrchestratorError('Bản nháp chưa sẵn sàng để phê duyệt.', 'DRAFT_NOT_READY');
    }
    if (!['customer', 'teller', 'supervisor'].includes(actor)) throw new OrchestratorError('Actor phê duyệt không hợp lệ.', 'INVALID_APPROVER');
    if (actor === 'teller' && !session.approvals.customer) {
      throw new OrchestratorError('Khách hàng phải xác nhận trước Teller.', 'CUSTOMER_APPROVAL_REQUIRED');
    }
    if (actor === 'supervisor' && !session.transactionDraft.limit?.supervisorRequired) {
      throw new OrchestratorError('Giao dịch này không yêu cầu kiểm soát viên.', 'SUPERVISOR_NOT_REQUIRED');
    }
    session.approvals[actor] = true;
    this.#event(session, 'APPROVAL', `${labelActor(actor)} đã xác nhận`, 'Approval được ghi tại checkpoint.');
    this.#evaluateControl(session);
    session.status = session.control.postingAllowed ? 'READY_TO_POST' : 'AWAITING_APPROVAL';
    return this.#save(session);
  }

  async execute(sessionId) {
    const session = this.#session(sessionId);
    this.#evaluateControl(session);
    if (!session.control.postingAllowed) {
      throw new OrchestratorError(`Chưa đủ control gate: ${session.control.missingGates.join(', ')}`, 'CONTROL_GATES_INCOMPLETE');
    }
    if (session.execution?.status === 'POSTED') return clone(session);

    session.transactionId ||= crypto.randomUUID();
    session.status = 'POSTING_REQUESTED';
    this.#event(session, 'CONTROL', 'Business Orchestrator cho phép posting', 'Agent không trực tiếp gọi financial-write tool.');
    const capabilityId = session.workflow.startsWith('cash_') ? 'core.cash.execute' : 'core.transfer.execute';
    const call = await this.toolGateway.call({
      caller: 'business_orchestrator',
      workflow: session.workflow,
      capabilityId,
      args: session.transactionDraft,
      idempotencyKey: session.transactionId,
    });
    session.agentRuntime.toolCalls.push(call);
    session.execution = call.result;
    session.status = call.result.status;
    this.#event(session, 'CORE', 'Core banking mock phản hồi', `${call.result.status} · ${call.result.coreReference}`);
    session.messages.push({ role: 'assistant', text: `Giao dịch mock đã ghi nhận: ${call.result.coreReference}.`, at: new Date().toISOString() });
    return this.#save(session);
  }

  async confirmAndExecuteCash(sessionId) {
    const session = this.#session(sessionId);
    if (!session.workflow?.startsWith('cash_')) {
      throw new OrchestratorError('Thao tác này chỉ áp dụng cho màn hình nộp/rút tiền.', 'CASH_WORKFLOW_REQUIRED');
    }
    if (session.status !== 'DRAFT_READY') {
      throw new OrchestratorError('Bản nháp tiền mặt chưa sẵn sàng để GDV xác nhận.', 'DRAFT_NOT_READY');
    }
    if (session.transactionDraft.limit?.supervisorRequired) {
      throw new OrchestratorError('Giao dịch vượt hạn mức GDV và cần kiểm soát viên xác nhận riêng.', 'SUPERVISOR_APPROVAL_REQUIRED');
    }
    session.approvals.customer = true;
    session.approvals.teller = true;
    this.#event(session, 'APPROVAL', 'GDV đã verify và xác nhận Submit', 'Ghi nhận khách hàng đồng ý và GDV đã kiểm tra dữ liệu tự điền.');
    this.#evaluateControl(session);
    return this.execute(sessionId);
  }

  bootstrap() {
    return {
      product: 'B.Smart Teller Agent POC',
      mode: 'MOCK_ONLY',
      capabilities: listCapabilities(),
      scenarios: [
        {
          id: 'cash-deposit',
          name: 'Nộp tiền tự điền',
          prompt: 'Nộp tiền 50tr vào tài khoản 3456789',
        },
        {
          id: 'cash-withdrawal',
          name: 'Rút tiền tự điền',
          prompt: 'Rút tiền 50tr từ tài khoản 3456789',
        },
        {
          id: 'transfer-complete',
          name: 'Chuyển khoản hoàn chỉnh',
          prompt: 'Chuyển 50 triệu cho Nguyễn Văn An tại Vietcombank, số tài khoản 012345678901',
        },
        {
          id: 'transfer-missing',
          name: 'Thiếu thông tin',
          prompt: 'Tôi muốn chuyển 25 triệu sang Vietcombank',
        },
        {
          id: 'risk-review',
          name: 'Cảnh báo rủi ro mock',
          prompt: 'Chuyển 220 triệu cho Trần Thị Lan tại BIDV, số tài khoản 123456789999',
        },
        {
          id: 'policy',
          name: 'Hỏi quy trình',
          prompt: 'Quy trình và biểu phí chuyển khoản tại quầy như thế nào?',
        },
      ],
    };
  }

  async #delegate(session, agent, stepId) {
    const allowed = ALLOWED_DELEGATIONS[session.workflow] || [];
    if (!allowed.includes(agent)) throw new OrchestratorError(`Không được delegate sang ${agent}.`, 'DELEGATION_DENIED');
    if (session.agentRuntime.budget.delegationsUsed >= session.agentRuntime.budget.maxDelegations) {
      throw new OrchestratorError('Đã vượt ngân sách delegation.', 'DELEGATION_BUDGET_EXCEEDED');
    }
    session.agentRuntime.budget.delegationsUsed += 1;
    session.agentRuntime.delegations.push({ stepId, agent, status: 'RUNNING', startedAt: new Date().toISOString() });
    this.#event(session, 'DELEGATE', `Orchestrator gọi ${agent}`, `Bước ${stepId}`);
    const result = await this.agents.run(agent, clone(session));
    const delegation = session.agentRuntime.delegations.find(item => item.stepId === stepId);
    delegation.status = 'COMPLETED';
    delegation.completedAt = new Date().toISOString();
    session.agentRuntime.completedSteps.push(stepId);
    session.agentRuntime.toolCalls.push(...result.toolCalls);
    session.agentRuntime.budget.toolCallsUsed += result.toolCalls.length;
    if (session.agentRuntime.budget.toolCallsUsed > session.agentRuntime.budget.maxToolCalls) {
      throw new OrchestratorError('Đã vượt ngân sách tool call.', 'TOOL_BUDGET_EXCEEDED');
    }
    return result;
  }

  #mergePatch(session, result) {
    const allowed = AGENT_OWNERSHIP[result.agent] || [];
    for (const [key, value] of Object.entries(result.writes || {})) {
      if (!allowed.includes(key)) throw new OrchestratorError(`${result.agent} không sở hữu state.${key}`, 'STATE_OWNERSHIP_VIOLATION');
      session[key] = value;
    }
    this.#event(session, 'PATCH', `${result.agent} cập nhật state`, `${Object.keys(result.writes).join(', ')} · evidence ${result.evidence.join(', ')}`);
  }

  #validatePlan(session, plan) {
    const allowed = new Set(ALLOWED_DELEGATIONS[session.workflow] || []);
    if (!Array.isArray(plan.steps) || !plan.steps.length) throw new OrchestratorError('Plan không có bước thực hiện.', 'INVALID_PLAN');
    if (plan.steps.length > session.agentRuntime.budget.maxDelegations) throw new OrchestratorError('Plan vượt ngân sách.', 'INVALID_PLAN');
    for (const step of plan.steps) {
      if (step.kind !== 'DELEGATE' || !allowed.has(step.target)) {
        throw new OrchestratorError(`Plan chứa delegation không hợp lệ: ${step.target}`, 'INVALID_PLAN');
      }
    }
  }

  #evaluateControl(session) {
    const missing = [];
    if (session.transactionDraft.validation?.valid !== true) missing.push('DRAFT_VALID');
    if (session.risk.decision !== 'PASS') missing.push('RISK_PASS');
    if (!session.approvals.customer) missing.push('CUSTOMER_CONFIRMED');
    if (!session.approvals.teller) missing.push('TELLER_CONFIRMED');
    if (session.transactionDraft.limit?.supervisorRequired && !session.approvals.supervisor) missing.push('SUPERVISOR_CONFIRMED');
    session.control = { postingAllowed: missing.length === 0, missingGates: missing, lastEvaluatedAt: new Date().toISOString() };
  }

  #session(sessionId) {
    const session = this.sessions.get(sessionId);
    if (!session) throw new OrchestratorError('Không tìm thấy phiên.', 'SESSION_NOT_FOUND', 404);
    return session;
  }

  #event(session, type, title, detail) {
    session.events.push({ seq: session.events.length + 1, at: new Date().toISOString(), type, title, detail });
  }

  #save(session) {
    session.revision += 1;
    session.updatedAt = new Date().toISOString();
    this.sessions.set(session.sessionId, session);
    return clone(session);
  }
}

function humanizeMissingFields(fields) {
  const labels = {
    sourceAccountRef: 'tài khoản nguồn', beneficiaryAccount: 'số tài khoản người nhận', beneficiaryName: 'tên người nhận', bankCode: 'ngân hàng thụ hưởng', accountNumber: 'số tài khoản', amount: 'số tiền',
  };
  return `Cần Teller bổ sung: ${fields.map(field => labels[field] || field).join(', ')}.`;
}

function labelActor(actor) {
  return ({ customer: 'Khách hàng', teller: 'Teller', supervisor: 'Kiểm soát viên' })[actor];
}

function clone(value) {
  return structuredClone(value);
}

module.exports = { TellerOrchestrator, OrchestratorError, AGENT_OWNERSHIP };
