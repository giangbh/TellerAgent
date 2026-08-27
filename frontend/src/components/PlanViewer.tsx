import React from 'react';
import { GitBranch, CheckCircle2, Clock } from 'lucide-react';
import { AgentRuntime } from '../types/teller';

interface PlanViewerProps {
  agentRuntime: AgentRuntime;
  workflow?: string | null;
}

export const PlanViewer: React.FC<PlanViewerProps> = ({ agentRuntime, workflow }) => {
  const plan = agentRuntime.plan;
  const completed = new Set(agentRuntime.completedSteps || []);

  const getAgentLabel = (target: string) => {
    switch (target) {
      case 'customer_context_agent': return 'Customer Context Agent';
      case 'policy_agent': return 'Policy & Pricing Agent';
      case 'transaction_draft_agent': return 'Transaction Draft Agent';
      case 'risk_assistant': return 'Risk & AML Assistant';
      default: return target;
    }
  };

  return (
    <div className="glass-panel" style={{ padding: '18px 20px', marginBottom: '20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px', flexWrap: 'wrap', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <GitBranch size={18} color="#10b981" />
          <h3 style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-main)' }}>
            Structured Plan & Capability Delegations
          </h3>
        </div>
        <div style={{ display: 'flex', gap: '8px', fontSize: '0.75rem' }}>
          <span className="badge badge-neutral">
            MCP Tool Calls: {agentRuntime.toolCalls.length}/{agentRuntime.budget.maxToolCalls}
          </span>
          <span className="badge badge-neutral">
            Workflow: {workflow || 'NONE'}
          </span>
        </div>
      </div>

      {plan ? (
        <div>
          <div style={{
            fontSize: '0.8rem',
            color: 'var(--text-muted)',
            marginBottom: '12px',
            padding: '8px 12px',
            background: 'rgba(255, 255, 255, 0.03)',
            borderRadius: '8px',
            border: '1px solid var(--border-color)',
          }}>
            <strong style={{ color: '#fff' }}>Mục tiêu: </strong>
            {plan.objective}
          </div>

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            {plan.steps.map((step) => {
              const isDone = completed.has(step.id);
              return (
                <div
                  key={step.id}
                  style={{
                    flex: '1 1 200px',
                    padding: '12px',
                    borderRadius: '10px',
                    background: isDone
                      ? 'rgba(16, 185, 129, 0.08)'
                      : 'rgba(255, 255, 255, 0.02)',
                    border: `1px solid ${isDone ? 'rgba(16, 185, 129, 0.3)' : 'var(--border-color)'}`,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '6px',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span style={{ fontSize: '0.7rem', fontWeight: 800, color: isDone ? '#34d399' : 'var(--text-dim)' }}>
                      BƯỚC {step.id}
                    </span>
                    {isDone ? (
                      <CheckCircle2 size={14} color="#34d399" />
                    ) : (
                      <Clock size={14} color="var(--text-dim)" />
                    )}
                  </div>
                  <div style={{ fontSize: '0.8rem', fontWeight: 600, color: isDone ? '#fff' : 'var(--text-muted)' }}>
                    {getAgentLabel(step.target)}
                  </div>
                  {step.dependsOn && step.dependsOn.length > 0 && (
                    <div style={{ fontSize: '0.675rem', color: 'var(--text-dim)' }}>
                      Phụ thuộc: {step.dependsOn.join(', ')}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)', textAlign: 'center', padding: '12px 0' }}>
          Chưa có kế hoạch nào được lập. Hãy nhập yêu cầu để khởi tạo.
        </div>
      )}
    </div>
  );
};
