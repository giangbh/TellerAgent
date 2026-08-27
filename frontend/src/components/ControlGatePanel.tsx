import React from 'react';
import { ShieldCheck, ShieldAlert, Lock, CheckCircle2, User, Key, Check, Send } from 'lucide-react';
import { Session } from '../types/teller';

interface ControlGatePanelProps {
  session: Session;
  onApprove: (actor: 'customer' | 'teller' | 'supervisor') => void;
  onExecute: () => void;
  onConfirmAndExecuteCash: () => void;
  loading: boolean;
}

export const ControlGatePanel: React.FC<ControlGatePanelProps> = ({
  session,
  onApprove,
  onExecute,
  onConfirmAndExecuteCash,
  loading,
}) => {
  const isCash = session.workflow?.startsWith('cash_');
  const approvals = session.approvals;
  const control = session.control;
  const risk = session.risk;
  const draft = session.transactionDraft;
  const isPosted = session.status === 'POSTED';
  const supervisorRequired = Boolean(draft.limit?.supervisorRequired);
  const isRiskReview = session.status === 'RISK_REVIEW' || risk.decision === 'REVIEW';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      
      {/* 1. Policy Citations Box */}
      <div className="glass-panel" style={{ padding: '16px 20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
          <ShieldCheck size={16} color="#8b5cf6" />
          <h3 style={{ fontSize: '0.875rem', fontWeight: 700 }}>Chính Sách & Trích Dẫn Quy Chế</h3>
        </div>

        {session.policyFindings?.citations && session.policyFindings.citations.length > 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {session.policyFindings.citations.map((c, idx) => (
              <div key={idx} style={{
                padding: '8px 12px',
                borderRadius: '8px',
                background: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid var(--border-color)',
                fontSize: '0.775rem',
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2px' }}>
                  <strong style={{ color: '#c084fc' }}>{c.document} · Mục {c.section}</strong>
                  <span style={{ color: 'var(--text-dim)' }}>{c.effectiveDate}</span>
                </div>
                <div style={{ color: 'var(--text-muted)' }}>{c.title}</div>
              </div>
            ))}
          </div>
        ) : (
          <div style={{ fontSize: '0.775rem', color: 'var(--text-dim)' }}>
            Chưa có trích dẫn quy trình cho phiên này.
          </div>
        )}
      </div>

      {/* 2. Risk Evaluation Box */}
      <div className="glass-panel" style={{ padding: '16px 20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {isRiskReview ? <ShieldAlert size={16} color="#ef4444" /> : <ShieldCheck size={16} color="#10b981" />}
            <h3 style={{ fontSize: '0.875rem', fontWeight: 700 }}>Đánh Giá Rủi Ro & AML</h3>
          </div>
          <div>
            {risk.decision ? (
              <span className={`badge ${risk.decision === 'PASS' ? 'badge-success' : 'badge-danger'}`}>
                {risk.decision === 'PASS' ? 'PASS · AN TOÀN' : 'REVIEW · CẦN RÀ SOÁT'}
              </span>
            ) : (
              <span className="badge badge-neutral">CHƯA ĐÁNH GIÁ</span>
            )}
          </div>
        </div>

        {risk.alerts && risk.alerts.length > 0 && (
          <div style={{
            padding: '10px 12px',
            borderRadius: '8px',
            background: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            fontSize: '0.775rem',
            color: '#f87171',
          }}>
            {risk.alerts.map((al, i) => (
              <div key={i}>• {al}</div>
            ))}
          </div>
        )}
      </div>

      {/* 3. Maker-Checker & Action Submit Gate */}
      <div className="glass-panel" style={{ padding: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
          <Lock size={16} color="#3b82f6" />
          <h3 style={{ fontSize: '0.875rem', fontWeight: 700 }}>Control Gate & Phê Duyệt</h3>
        </div>

        {/* Cash Workflow Single Confirm */}
        {isCash && !supervisorRequired ? (
          <div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '14px' }}>
              GDV kiểm tra số tiền, chứng từ và số tài khoản tự điền trước khi submit sang Core Banking.
            </p>
            <button
              className="btn btn-primary"
              disabled={loading || session.status !== 'DRAFT_READY' || isPosted}
              onClick={onConfirmAndExecuteCash}
              style={{ width: '100%', padding: '14px', fontSize: '0.9rem' }}
            >
              <Check size={18} />
              <span>Xác Nhận & Submit Mock</span>
            </button>
          </div>
        ) : (
          /* Transfer or Supervisor-required Flow */
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {/* Step 1: Customer */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '10px 14px',
              borderRadius: '10px',
              background: approvals.customer ? 'rgba(16, 185, 129, 0.1)' : 'rgba(255, 255, 255, 0.03)',
              border: `1px solid ${approvals.customer ? 'rgba(16, 185, 129, 0.4)' : 'var(--border-color)'}`,
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <User size={16} color={approvals.customer ? '#34d399' : 'var(--text-dim)'} />
                <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>1. Khách hàng xác nhận</span>
              </div>
              <button
                className="btn btn-secondary"
                disabled={approvals.customer || isPosted || isRiskReview || session.status === 'SESSION_OPEN'}
                onClick={() => onApprove('customer')}
                style={{ padding: '6px 14px', fontSize: '0.775rem' }}
              >
                {approvals.customer ? <CheckCircle2 size={14} color="#34d399" /> : 'Xác nhận'}
              </button>
            </div>

            {/* Step 2: Teller */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '10px 14px',
              borderRadius: '10px',
              background: approvals.teller ? 'rgba(16, 185, 129, 0.1)' : 'rgba(255, 255, 255, 0.03)',
              border: `1px solid ${approvals.teller ? 'rgba(16, 185, 129, 0.4)' : 'var(--border-color)'}`,
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <ShieldCheck size={16} color={approvals.teller ? '#34d399' : 'var(--text-dim)'} />
                <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>2. GDV (Teller) verify</span>
              </div>
              <button
                className="btn btn-secondary"
                disabled={!approvals.customer || approvals.teller || isPosted || isRiskReview}
                onClick={() => onApprove('teller')}
                style={{ padding: '6px 14px', fontSize: '0.775rem' }}
              >
                {approvals.teller ? <CheckCircle2 size={14} color="#34d399" /> : 'Duyệt'}
              </button>
            </div>

            {/* Step 3: Supervisor if needed */}
            {supervisorRequired && (
              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '10px 14px',
                borderRadius: '10px',
                background: approvals.supervisor ? 'rgba(16, 185, 129, 0.1)' : 'rgba(245, 158, 11, 0.08)',
                border: `1px solid ${approvals.supervisor ? 'rgba(16, 185, 129, 0.4)' : 'rgba(245, 158, 11, 0.4)'}`,
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <Key size={16} color={approvals.supervisor ? '#34d399' : '#f59e0b'} />
                  <div>
                    <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>3. Kiểm soát viên (Supervisor)</span>
                    <div style={{ fontSize: '0.675rem', color: '#fbbf24' }}>Hạn mức &gt; 100tr VND</div>
                  </div>
                </div>
                <button
                  className="btn btn-secondary"
                  disabled={approvals.supervisor || isPosted}
                  onClick={() => onApprove('supervisor')}
                  style={{ padding: '6px 14px', fontSize: '0.775rem' }}
                >
                  {approvals.supervisor ? <CheckCircle2 size={14} color="#34d399" /> : 'Phê duyệt'}
                </button>
              </div>
            )}

            {/* Step 4: Execute Posting */}
            <div style={{ marginTop: '10px' }}>
              <button
                className="btn btn-primary"
                disabled={!control.postingAllowed || isPosted || loading}
                onClick={onExecute}
                style={{ width: '100%', padding: '14px', fontSize: '0.9rem' }}
              >
                <Send size={16} />
                <span>Gửi Hạch Toán Core Banking Mock</span>
              </button>
            </div>
          </div>
        )}

        {/* Missing Gates List if not allowed */}
        {!control.postingAllowed && control.missingGates && control.missingGates.length > 0 && (
          <div style={{ marginTop: '12px', fontSize: '0.725rem', color: 'var(--text-dim)' }}>
            <strong>Điều kiện còn thiếu: </strong>
            {control.missingGates.join(' · ')}
          </div>
        )}

        {/* 4. Execution Core Result Card */}
        {session.execution && (
          <div style={{
            marginTop: '16px',
            padding: '14px 18px',
            borderRadius: '12px',
            background: 'rgba(16, 185, 129, 0.12)',
            border: '1px solid rgba(16, 185, 129, 0.4)',
            textAlign: 'center',
          }}>
            <span className="badge badge-success" style={{ marginBottom: '8px' }}>
              CORE BANKING POSTED
            </span>
            <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#34d399', letterSpacing: '0.05em' }}>
              {session.execution.coreReference}
            </div>
            <div style={{ fontSize: '0.725rem', color: 'var(--text-muted)', marginTop: '4px' }}>
              Đã ghi nhận lúc: {new Date(session.execution.postedAt).toLocaleTimeString('vi-VN')}
            </div>
          </div>
        )}

      </div>
    </div>
  );
};
