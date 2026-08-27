package com.dnse.teller.orchestrator;

import com.dnse.teller.model.Intent;
import com.dnse.teller.model.Plan;
import com.dnse.teller.model.Session;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hybrid Reasoning Engine:
 * - Coordinates between DeepSeek AI (LLM Function Calling) and RuleBasedReasoningEngine (Deterministic slot-filling).
 * - Ensures high precision, zero hallucination risk, and seamless fallback.
 */
@Service
public class ReasoningEngine {

    private final DeepSeekClient deepSeekClient;
    private final RuleBasedReasoningEngine ruleBasedEngine;
    private final Map<String, DeepSeekClient.DeepSeekPlanResult> deepSeekPlanCache = new ConcurrentHashMap<>();

    public ReasoningEngine(DeepSeekClient deepSeekClient, RuleBasedReasoningEngine ruleBasedEngine) {
        this.deepSeekClient = deepSeekClient;
        this.ruleBasedEngine = ruleBasedEngine;
    }

    public Intent detectIntent(String text, Intent currentIntent) {
        // 1. DeepSeek AI Semantic / Function Calling (if configured and enabled)
        if (deepSeekClient.isConfiguredAndEnabled()) {
            Optional<DeepSeekClient.DeepSeekPlanResult> aiResult = deepSeekClient.reasonAndPlan(
                text,
                currentIntent != null ? currentIntent.getEntities() : Map.of()
            );
            if (aiResult.isPresent()) {
                DeepSeekClient.DeepSeekPlanResult res = aiResult.get();
                if (text != null) deepSeekPlanCache.put(text, res);
                return res.getIntent();
            }
        }

        // 2. Deterministic Rule-Based Fallback
        return ruleBasedEngine.detectIntent(text, currentIntent);
    }

    public Plan proposePlan(Session session) {
        String query = session.getIntent() != null ? session.getIntent().getQuery() : "";
        if (query != null && deepSeekPlanCache.containsKey(query)) {
            DeepSeekClient.DeepSeekPlanResult cached = deepSeekPlanCache.remove(query);
            if (cached != null && cached.getPlan() != null) {
                return cached.getPlan();
            }
        }

        // Delegate to Rule-Based Planner
        return ruleBasedEngine.proposePlan(session);
    }

    public RuleBasedReasoningEngine getRuleBasedEngine() {
        return ruleBasedEngine;
    }
}
