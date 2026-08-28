package com.dnse.teller.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class McpSecurityPolicyProvider {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, McpSecurityPolicy> policyCache = new ConcurrentHashMap<>();

    public McpSecurityPolicyProvider(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadOrSeedPolicies();
    }

    public synchronized void loadOrSeedPolicies() {
        // 1. Seed or Upsert defaults from mcp-policies.json
        try {
            ClassPathResource resource = new ClassPathResource("mcp-policies.json");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    List<McpSecurityPolicy> defaultPolicies = objectMapper.readValue(is, new TypeReference<List<McpSecurityPolicy>>() {});
                    Set<String> validIds = new HashSet<>();
                    for (McpSecurityPolicy policy : defaultPolicies) {
                        validIds.add(policy.getCapabilityId());
                        if (findPolicyInDb(policy.getCapabilityId()).isEmpty()) {
                            savePolicyToDb(policy);
                        }
                    }
                    deletePoliciesNotIn(validIds);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Không thể nạp default mcp-policies.json: " + e.getMessage());
        }

        // 2. Load all from SQLite DB into memory Cache
        loadAllFromDb();
    }

    private void deletePoliciesNotIn(Set<String> validIds) {
        if (validIds == null || validIds.isEmpty()) return;
        try {
            String placeholders = String.join(",", Collections.nCopies(validIds.size(), "?"));
            String sql = "DELETE FROM mcp_security_policies WHERE capability_id NOT IN (" + placeholders + ")";
            jdbcTemplate.update(sql, validIds.toArray());
        } catch (Exception e) {
            System.err.println("Lỗi dọn dẹp policies cũ: " + e.getMessage());
        }
    }

    /**
     * P0-5: trả về null khi không có policy tường minh, và người gọi phải coi
     * null là TỪ CHỐI.
     *
     * Bản cũ tạo ra một policy mặc định MỞ cho mọi capability lạ, tức là thêm
     * một tool mới mà quên khai báo policy thì tool đó tự động được phép chạy.
     */
    public McpSecurityPolicy getPolicy(String capabilityId) {
        if (capabilityId == null) return null;
        if (policyCache.containsKey(capabilityId)) {
            return policyCache.get(capabilityId);
        }

        Optional<McpSecurityPolicy> fromDb = findPolicyInDb(capabilityId);
        if (fromDb.isPresent()) {
            policyCache.put(capabilityId, fromDb.get());
            return fromDb.get();
        }

        return null;
    }

    public List<McpSecurityPolicy> getAllPolicies() {
        return new ArrayList<>(policyCache.values());
    }

    public synchronized McpSecurityPolicy updatePolicy(String capabilityId, List<String> allowedCallers, List<String> allowedWorkflows) {
        McpSecurityPolicy existing = getPolicy(capabilityId);

        if (existing == null) {
            // Không tạo policy mới qua API vận hành: policy phải đi kèm code của tool.
            throw new IllegalArgumentException(
                "Không tồn tại policy cho " + capabilityId + ". Policy mới phải được khai báo trong mcp-policies.json.");
        }

        // P0-5: capability ghi sổ tài chính không được nới quyền lúc runtime.
        if ("FINANCIAL_WRITE".equals(existing.getRiskLevel()) || existing.isSideEffect()) {
            throw new IllegalArgumentException(
                "Không được thay đổi policy của capability ghi sổ tài chính " + capabilityId + " khi đang vận hành.");
        }

        {
            if (allowedCallers != null) existing.setAllowedCallers(allowedCallers);
            if (allowedWorkflows != null) existing.setAllowedWorkflows(allowedWorkflows);
        }

        existing.setUpdatedAt(Instant.now().toString());
        savePolicyToDb(existing);
        policyCache.put(capabilityId, existing);
        return existing;
    }

    private void savePolicyToDb(McpSecurityPolicy policy) {
        try {
            String callersJson = objectMapper.writeValueAsString(policy.getAllowedCallers());
            String workflowsJson = objectMapper.writeValueAsString(policy.getAllowedWorkflows());
            String now = policy.getUpdatedAt() != null ? policy.getUpdatedAt() : Instant.now().toString();

            String sql = """
                INSERT INTO mcp_security_policies (capability_id, risk_level, is_side_effect, requires_idempotency, allowed_callers, allowed_workflows, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(capability_id) DO UPDATE SET
                    risk_level = excluded.risk_level,
                    is_side_effect = excluded.is_side_effect,
                    requires_idempotency = excluded.requires_idempotency,
                    allowed_callers = excluded.allowed_callers,
                    allowed_workflows = excluded.allowed_workflows,
                    updated_at = excluded.updated_at;
            """;

            jdbcTemplate.update(sql,
                policy.getCapabilityId(),
                policy.getRiskLevel(),
                policy.isSideEffect() ? 1 : 0,
                policy.isRequiresIdempotency() ? 1 : 0,
                callersJson,
                workflowsJson,
                now
            );
        } catch (Exception e) {
            System.err.println("Lỗi lưu policy vào SQLite: " + e.getMessage());
        }
    }

    private Optional<McpSecurityPolicy> findPolicyInDb(String capabilityId) {
        String sql = "SELECT * FROM mcp_security_policies WHERE capability_id = ?";
        List<McpSecurityPolicy> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            try {
                McpSecurityPolicy p = new McpSecurityPolicy();
                p.setCapabilityId(rs.getString("capability_id"));
                p.setRiskLevel(rs.getString("risk_level"));
                p.setSideEffect(rs.getInt("is_side_effect") == 1);
                p.setRequiresIdempotency(rs.getInt("requires_idempotency") == 1);
                p.setAllowedCallers(objectMapper.readValue(rs.getString("allowed_callers"), new TypeReference<List<String>>() {}));
                p.setAllowedWorkflows(objectMapper.readValue(rs.getString("allowed_workflows"), new TypeReference<List<String>>() {}));
                p.setUpdatedAt(rs.getString("updated_at"));
                return p;
            } catch (Exception e) {
                return null;
            }
        }, capabilityId);

        return list.stream().filter(Objects::nonNull).findFirst();
    }

    private void loadAllFromDb() {
        String sql = "SELECT * FROM mcp_security_policies";
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            try {
                McpSecurityPolicy p = new McpSecurityPolicy();
                p.setCapabilityId(rs.getString("capability_id"));
                p.setRiskLevel(rs.getString("risk_level"));
                p.setSideEffect(rs.getInt("is_side_effect") == 1);
                p.setRequiresIdempotency(rs.getInt("requires_idempotency") == 1);
                p.setAllowedCallers(objectMapper.readValue(rs.getString("allowed_callers"), new TypeReference<List<String>>() {}));
                p.setAllowedWorkflows(objectMapper.readValue(rs.getString("allowed_workflows"), new TypeReference<List<String>>() {}));
                p.setUpdatedAt(rs.getString("updated_at"));
                policyCache.put(p.getCapabilityId(), p);
                return p;
            } catch (Exception e) {
                return null;
            }
        });
    }
}
