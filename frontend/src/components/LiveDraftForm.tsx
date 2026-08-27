import React from 'react';
import { FileEdit, CheckCircle2, AlertTriangle, Building, CreditCard, Banknote, ShieldAlert } from 'lucide-react';
import { TransactionDraft } from '../types/teller';

interface LiveDraftFormProps {
  draft: TransactionDraft;
  workflow?: string | null;
}

export const LiveDraftForm: React.FC<LiveDraftFormProps> = ({ draft, workflow }) => {
  const isCash = workflow?.startsWith('cash_');
  const validation = draft.validation;
  const missing = new Set(validation?.missingFields || []);
  const isValid = validation?.valid === true;

  const formatVnd = (val?: number | null) => {
    if (val == null) return '—';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  return (
    <div className="glass-panel" style={{ padding: '22px', height: '100%', display: 'flex', flexDirection: 'column' }}>
      
      {/* Title */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px', borderBottom: '1px solid var(--border-color)', paddingBottom: '14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            width: '32px',
            height: '32px',
            borderRadius: '8px',
            background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}>
            {isCash ? <Banknote size={18} color="#fff" /> : <FileEdit size={18} color="#fff" />}
          </div>
          <div>
            <h2 style={{ fontSize: '1rem', fontWeight: 700 }}>
              {draft.screenTitle || (isCash ? 'Màn hình Giao dịch Tiền mặt' : 'Bản nháp Chuyển khoản trong nước')}
            </h2>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              Live Action Draft · Auto-filled by Teller Copilot
            </p>
          </div>
        </div>

        <div>
          {isValid ? (
            <span className="badge badge-success">
              <CheckCircle2 size={13} /> HỢP LỆ (VALID)
            </span>
          ) : (
            <span className="badge badge-warning">
              <AlertTriangle size={13} /> {validation?.missingFields?.length ? `THIẾU ${validation.missingFields.length} TRƯỜNG` : 'CHƯA HOÀN THIỆN'}
            </span>
          )}
        </div>
      </div>

      {/* Validation Blocker Alert if any */}
      {validation?.blockers && validation.blockers.length > 0 && (
        <div style={{
          padding: '12px 16px',
          background: 'rgba(239, 68, 68, 0.15)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
          borderRadius: '10px',
          marginBottom: '16px',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          fontSize: '0.85rem',
          color: '#fca5a5',
        }}>
          <ShieldAlert size={18} color="#ef4444" />
          <span><strong>Cảnh báo:</strong> {validation.blockers.join(', ')} (Số dư khả dụng không đủ cho giao dịch)</span>
        </div>
      )}

      {/* Form Fields */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', flex: 1 }}>
        
        {isCash ? (
          <>
            {/* Cash Screen Code */}
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                Loại giao dịch tiền mặt
              </label>
              <div style={{
                padding: '10px 14px',
                borderRadius: '8px',
                background: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid var(--border-color)',
                fontSize: '0.875rem',
                fontWeight: 600,
                color: '#34d399',
              }}>
                {draft.transactionType === 'CASH_DEPOSIT' ? 'Nộp tiền mặt vào tài khoản' : 'Rút tiền mặt từ tài khoản'}
              </div>
            </div>

            {/* Account Number */}
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 600, color: missing.has('accountNumber') ? '#fbbf24' : 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                Số tài khoản đối chiếu {missing.has('accountNumber') && <span style={{ color: '#fbbf24' }}>(bắt buộc)</span>}
              </label>
              <input
                type="text"
                readOnly
                value={draft.accountNumber || ''}
                placeholder="Chưa có dữ liệu"
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  background: missing.has('accountNumber') ? 'rgba(245, 158, 11, 0.08)' : 'rgba(255, 255, 255, 0.03)',
                  border: `1px solid ${missing.has('accountNumber') ? '#f59e0b' : 'var(--border-color)'}`,
                  color: '#ffffff',
                  fontSize: '0.9rem',
                  fontWeight: 600,
                }}
              />
            </div>

            {/* Account Holder */}
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                Tên chủ tài khoản (Mock CIF)
              </label>
              <input
                type="text"
                readOnly
                value={draft.accountHolder || ''}
                placeholder="Tự động đối chiếu..."
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  background: 'rgba(255, 255, 255, 0.03)',
                  border: '1px solid var(--border-color)',
                  color: '#34d399',
                  fontSize: '0.9rem',
                  fontWeight: 600,
                }}
              />
            </div>

            {/* Available Balance */}
            {draft.availableBalance != null && (
              <div>
                <label style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                  Số dư khả dụng hiện tại
                </label>
                <div style={{
                  padding: '10px 14px',
                  borderRadius: '8px',
                  background: 'rgba(255, 255, 255, 0.03)',
                  border: '1px solid var(--border-color)',
                  fontSize: '0.9rem',
                  fontWeight: 700,
                  color: '#60a5fa',
                }}>
                  {formatVnd(draft.availableBalance)}
                </div>
              </div>
            )}

            {/* Cash Amount */}
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 600, color: missing.has('amount') ? '#fbbf24' : 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                Số tiền mặt giao dịch {missing.has('amount') && <span style={{ color: '#fbbf24' }}>(bắt buộc)</span>}
              </label>
              <div style={{
                padding: '10px 14px',
                borderRadius: '8px',
                background: missing.has('amount') ? 'rgba(245, 158, 11, 0.08)' : 'rgba(16, 185, 129, 0.08)',
                border: `1px solid ${missing.has('amount') ? '#f59e0b' : 'rgba(16, 185, 129, 0.4)'}`,
                fontSize: '1.15rem',
                fontWeight: 800,
                color: '#34d399',
              }}>
                {formatVnd(draft.amount)}
              </div>
            </div>
          </>
        ) : (
          <>
            {/* Source Account */}
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 600, color: missing.has('sourceAccountRef') ? '#fbbf24' : 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                Tài khoản trích nợ (Nguồn)
              </label>
              <div style={{
                padding: '10px 14px',
                borderRadius: '8px',
                background: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid var(--border-color)',
                fontSize: '0.875rem',
                color: '#ffffff',
              }}>
                <CreditCard size={15} style={{ display: 'inline', verticalAlign: '-2px', marginRight: '6px', color: '#60a5fa' }} />
                {draft.sourceAccountMasked ? `${draft.sourceAccountRef} (${draft.sourceAccountMasked})` : '—'}
              </div>
            </div>

            {/* Beneficiary Bank */}
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 600, color: missing.has('bankCode') ? '#fbbf24' : 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                Ngân hàng thụ hưởng {missing.has('bankCode') && <span style={{ color: '#fbbf24' }}>(bắt buộc)</span>}
              </label>
              <div style={{
                padding: '10px 14px',
                borderRadius: '8px',
                background: missing.has('bankCode') ? 'rgba(245, 158, 11, 0.08)' : 'rgba(255, 255, 255, 0.03)',
                border: `1px solid ${missing.has('bankCode') ? '#f59e0b' : 'var(--border-color)'}`,
                fontSize: '0.875rem',
                color: '#ffffff',
              }}>
                <Building size={15} style={{ display: 'inline', verticalAlign: '-2px', marginRight: '6px', color: '#c084fc' }} />
                {draft.bankName ? `${draft.bankCode} - ${draft.bankName}` : '—'}
              </div>
            </div>

            {/* Beneficiary Account */}
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 600, color: missing.has('beneficiaryAccount') ? '#fbbf24' : 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                Số tài khoản thụ hưởng {missing.has('beneficiaryAccount') && <span style={{ color: '#fbbf24' }}>(bắt buộc)</span>}
              </label>
              <input
                type="text"
                readOnly
                value={draft.beneficiaryAccount || ''}
                placeholder="Chưa có dữ liệu"
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  background: missing.has('beneficiaryAccount') ? 'rgba(245, 158, 11, 0.08)' : 'rgba(255, 255, 255, 0.03)',
                  border: `1px solid ${missing.has('beneficiaryAccount') ? '#f59e0b' : 'var(--border-color)'}`,
                  color: '#ffffff',
                  fontSize: '0.9rem',
                  fontWeight: 600,
                }}
              />
            </div>

            {/* Beneficiary Name */}
            <div>
              <label style={{ fontSize: '0.75rem', fontWeight: 600, color: missing.has('beneficiaryName') ? '#fbbf24' : 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                Người thụ hưởng {missing.has('beneficiaryName') && <span style={{ color: '#fbbf24' }}>(bắt buộc)</span>}
              </label>
              <input
                type="text"
                readOnly
                value={draft.beneficiaryName || ''}
                placeholder="Chưa có dữ liệu"
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  borderRadius: '8px',
                  background: missing.has('beneficiaryName') ? 'rgba(245, 158, 11, 0.08)' : 'rgba(255, 255, 255, 0.03)',
                  border: `1px solid ${missing.has('beneficiaryName') ? '#f59e0b' : 'var(--border-color)'}`,
                  color: '#ffffff',
                  fontSize: '0.9rem',
                  fontWeight: 600,
                }}
              />
            </div>

            {/* Transfer Amount */}
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={{ fontSize: '0.75rem', fontWeight: 600, color: missing.has('amount') ? '#fbbf24' : 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                Số tiền chuyển khoản {missing.has('amount') && <span style={{ color: '#fbbf24' }}>(bắt buộc)</span>}
              </label>
              <div style={{
                padding: '14px 18px',
                borderRadius: '10px',
                background: missing.has('amount') ? 'rgba(245, 158, 11, 0.08)' : 'rgba(16, 185, 129, 0.08)',
                border: `1px solid ${missing.has('amount') ? '#f59e0b' : 'rgba(16, 185, 129, 0.4)'}`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}>
                <span style={{ fontSize: '1.25rem', fontWeight: 800, color: '#34d399' }}>
                  {formatVnd(draft.amount)}
                </span>
                {draft.fee && (
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    Phí mock: <strong style={{ color: '#fff' }}>{formatVnd(draft.fee.totalFee)}</strong> (VAT: {formatVnd(draft.fee.vat)})
                  </span>
                )}
              </div>
            </div>
          </>
        )}

      </div>
    </div>
  );
};
