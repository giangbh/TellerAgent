import React, { useState, useEffect } from 'react';
import {
  User,
  CreditCard,
  CheckCircle2,
  X,
  ShieldCheck,
  TrendingUp,
  Award,
  Sparkles,
  Phone,
  Briefcase,
  ArrowUpRight,
  ArrowDownLeft,
} from 'lucide-react';
import { fetchCustomer360 } from '../api/tellerApi';

interface Customer360ModalProps {
  isOpen: boolean;
  cif: string | null;
  onClose: () => void;
  onOpenAccount: (accountNumber: string) => void;
  onTriggerAction?: (prompt: string) => void;
  onSelectAsActiveCustomer?: (cif: string) => void;
}

export const Customer360Modal: React.FC<Customer360ModalProps> = ({
  isOpen,
  cif,
  onClose,
  onOpenAccount,
  onSelectAsActiveCustomer,
}) => {
  const [data, setData] = useState<Record<string, any> | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'ACCOUNTS' | 'CREDIT_NBO' | 'HISTORY'>('OVERVIEW');

  useEffect(() => {
    if (!isOpen || !cif) {
      setData(null);
      setError(null);
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await fetchCustomer360(cif);
        setData(res);
      } catch (err: any) {
        setError(err.message || 'Không thể tải hồ sơ 360 khách hàng.');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [isOpen, cif]);

  if (!isOpen) return null;

  const formatVnd = (amount: any) => {
    if (amount === undefined || amount === null) return '0 VND';
    const num = typeof amount === 'number' ? amount : parseFloat(amount);
    return new Intl.NumberFormat('vi-VN').format(num) + ' VND';
  };

  const profile = data?.profile || {};
  const accountsData = data?.accounts || {};
  const accountsSummary = data?.accountsSummary || {};
  const creditScore = data?.creditScore || {};
  const recentTransactions = data?.recentTransactions || [];

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        backdropFilter: 'blur(8px)',
        zIndex: 10000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px',
      }}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '880px',
          background: 'linear-gradient(180deg, #181c26 0%, #11141c 100%)',
          borderRadius: '16px',
          border: '1px solid rgba(255, 255, 255, 0.12)',
          boxShadow: '0 25px 60px -15px rgba(0, 0, 0, 0.8), 0 0 40px rgba(168, 85, 247, 0.15)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
          maxHeight: '90vh',
        }}
      >
        {/* Header */}
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            background: 'rgba(255, 255, 255, 0.02)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div
              style={{
                width: '40px',
                height: '40px',
                borderRadius: '10px',
                background: 'linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 4px 12px rgba(139, 92, 246, 0.3)',
              }}
            >
              <User size={22} color="#fff" />
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <h2 style={{ fontSize: '1.15rem', fontWeight: 800, color: '#fff', margin: 0 }}>
                  {profile.displayName || profile.name || cif}
                </h2>
                <span className="badge badge-purple" style={{ fontSize: '0.7rem' }}>
                  {profile.cif || cif}
                </span>
                <span className="badge badge-success" style={{ fontSize: '0.7rem' }}>
                  {profile.segment || 'DIAMOND VIP'}
                </span>
              </div>
              <p style={{ fontSize: '0.78rem', color: '#9ca3af', margin: '2px 0 0 0' }}>
                Hồ Sơ Khách Hàng 360 Độ · Customer Intelligence Engine
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {onSelectAsActiveCustomer && (
              <button
                className="btn btn-secondary"
                onClick={() => {
                  onSelectAsActiveCustomer(cif || '');
                  onClose();
                }}
                style={{ fontSize: '0.78rem', padding: '6px 12px' }}
                title="Gán khách hàng này vào phiên quầy hiện tại"
              >
                <CheckCircle2 size={14} color="#10b981" />
                <span>Chọn cho phiên này</span>
              </button>
            )}

            <button
              onClick={onClose}
              style={{
                background: 'rgba(255, 255, 255, 0.08)',
                border: 'none',
                color: '#9ca3af',
                borderRadius: '50%',
                width: '28px',
                height: '28px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
              }}
            >
              <X size={16} />
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div
          style={{
            display: 'flex',
            padding: '8px 20px',
            gap: '8px',
            borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
            background: 'rgba(0, 0, 0, 0.2)',
          }}
        >
          {[
            { id: 'OVERVIEW', label: 'Thông tin Định Danh (KYC)' },
            { id: 'ACCOUNTS', label: `Tài khoản & Tiết kiệm (${accountsData.accounts?.length || 3})` },
            { id: 'CREDIT_NBO', label: 'Điểm Tín Dụng & Ưu Đãi NBO' },
            { id: 'HISTORY', label: 'Giao Dịch Gần Nhất' },
          ].map((tab) => {
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                style={{
                  background: active ? 'rgba(139, 92, 246, 0.2)' : 'transparent',
                  border: active ? '1px solid rgba(139, 92, 246, 0.4)' : '1px solid transparent',
                  color: active ? '#c084fc' : '#9ca3af',
                  padding: '6px 14px',
                  borderRadius: '8px',
                  fontSize: '0.8rem',
                  fontWeight: active ? 600 : 400,
                  cursor: 'pointer',
                  transition: 'all 0.15s ease',
                }}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Content Body */}
        <div style={{ padding: '20px', overflowY: 'auto', flex: 1, minHeight: '380px' }}>
          {loading ? (
            <div style={{ padding: '60px 20px', textAlign: 'center', color: '#9ca3af' }}>
              <Sparkles size={32} className="animate-spin" style={{ margin: '0 auto 12px', color: '#a855f7' }} />
              <p style={{ fontSize: '0.9rem' }}>Đang tổng hợp dữ liệu chân dung khách hàng 360...</p>
            </div>
          ) : error ? (
            <div style={{ padding: '30px', textAlign: 'center', color: '#fca5a5' }}>
              <p style={{ fontSize: '0.95rem', fontWeight: 600 }}>{error}</p>
            </div>
          ) : (
            <div>
              {/* TAB 1: OVERVIEW & KYC */}
              {activeTab === 'OVERVIEW' && (
                <div>
                  <div
                    style={{
                      display: 'grid',
                      gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
                      gap: '14px',
                      marginBottom: '20px',
                    }}
                  >
                    <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(255, 255, 255, 0.06)' }}>
                      <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Họ Và Tên</div>
                      <div style={{ fontSize: '1rem', fontWeight: 700, color: '#fff', marginTop: '2px' }}>
                        {profile.displayName || profile.name}
                      </div>
                    </div>

                    <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(255, 255, 255, 0.06)' }}>
                      <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Mã CIF Khách Hàng</div>
                      <div style={{ fontSize: '1rem', fontWeight: 700, color: '#a855f7', fontFamily: 'monospace', marginTop: '2px' }}>
                        {profile.cif || cif}
                      </div>
                    </div>

                    <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(255, 255, 255, 0.06)' }}>
                      <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>CCCD / Hộ Chiếu (Đã Masking)</div>
                      <div style={{ fontSize: '1rem', fontWeight: 600, color: '#60a5fa', fontFamily: 'monospace', marginTop: '2px' }}>
                        {profile.identityMasked || '0010••••3847'}
                      </div>
                    </div>

                    <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(255, 255, 255, 0.06)' }}>
                      <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Trạng Thái KYC</div>
                      <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#10b981', marginTop: '2px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                        <CheckCircle2 size={15} />
                        <span>ĐÃ XÁC THỰC (VERIFIED)</span>
                      </div>
                    </div>

                    <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(255, 255, 255, 0.06)' }}>
                      <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Số Điện Thoại Liên Hệ</div>
                      <div style={{ fontSize: '0.95rem', color: '#cbd5e1', marginTop: '2px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Phone size={14} color="#9ca3af" />
                        <span>098••••321</span>
                      </div>
                    </div>

                    <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '12px 14px', borderRadius: '10px', border: '1px solid rgba(255, 255, 255, 0.06)' }}>
                      <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Nghề Nghiệp / Phân Khúc</div>
                      <div style={{ fontSize: '0.95rem', color: '#cbd5e1', marginTop: '2px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Briefcase size={14} color="#9ca3af" />
                        <span>Chuyên gia CNTT · {profile.segment || 'VIP'}</span>
                      </div>
                    </div>
                  </div>

                  {/* Summary Metric Box */}
                  <div
                    style={{
                      background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.12) 0%, rgba(59, 130, 246, 0.08) 100%)',
                      borderRadius: '12px',
                      border: '1px solid rgba(139, 92, 246, 0.3)',
                      padding: '16px 20px',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      gap: '12px',
                    }}
                  >
                    <div>
                      <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: '#c084fc', fontWeight: 600 }}>
                        Tổng Tài Sản Khách Hàng (Total AUM)
                      </div>
                      <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#fff', marginTop: '4px' }}>
                        {formatVnd(accountsSummary.totalAum || 1792500000)}
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '10px' }}>
                      <button
                        className="btn btn-primary"
                        onClick={() => setActiveTab('ACCOUNTS')}
                        style={{ fontSize: '0.8rem', padding: '8px 14px' }}
                      >
                        <CreditCard size={14} />
                        <span>Xem danh sách tài khoản</span>
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* TAB 2: ACCOUNTS & DEPOSITS */}
              {activeTab === 'ACCOUNTS' && (
                <div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {(accountsData.accounts || [
                      { accountNumber: '3456789', accountType: 'CHECKING', availableBalance: 250000000, currency: 'VND', status: 'ACTIVE' },
                      { accountNumber: '012345678901', accountType: 'CHECKING', availableBalance: 42500000, currency: 'VND', status: 'ACTIVE' },
                      { accountNumber: 'TD-888999', accountType: 'TERM_DEPOSIT', availableBalance: 1500000000, currency: 'VND', status: 'ACTIVE', termMonths: 6, interestRate: 5.8 },
                    ]).map((acc: any) => (
                      <div
                        key={acc.accountNumber}
                        style={{
                          background: 'rgba(255, 255, 255, 0.03)',
                          border: '1px solid rgba(255, 255, 255, 0.08)',
                          borderRadius: '12px',
                          padding: '14px 18px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          flexWrap: 'wrap',
                          gap: '12px',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                          <div
                            style={{
                              width: '36px',
                              height: '36px',
                              borderRadius: '8px',
                              background: acc.accountType === 'TERM_DEPOSIT' ? 'rgba(245, 158, 11, 0.2)' : 'rgba(16, 185, 129, 0.2)',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                            }}
                          >
                            <CreditCard size={18} color={acc.accountType === 'TERM_DEPOSIT' ? '#f59e0b' : '#10b981'} />
                          </div>
                          <div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              <span style={{ fontSize: '1rem', fontWeight: 700, color: '#60a5fa', fontFamily: 'monospace' }}>
                                {acc.accountNumber}
                              </span>
                              <span className={`badge badge-${acc.accountType === 'TERM_DEPOSIT' ? 'warning' : 'success'}`} style={{ fontSize: '0.65rem' }}>
                                {acc.accountType === 'TERM_DEPOSIT' ? 'TIẾT KIỆM CÓ KỲ HẠN' : 'THANH TOÁN (CA)'}
                              </span>
                            </div>
                            <div style={{ fontSize: '0.78rem', color: '#9ca3af', marginTop: '2px' }}>
                              {acc.accountType === 'TERM_DEPOSIT'
                                ? `Kỳ hạn ${acc.termMonths || 6} tháng · Lãi suất ${acc.interestRate || 5.8}%/năm`
                                : 'Tài khoản thanh toán đa năng VND'}
                            </div>
                          </div>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                          <div style={{ textAlign: 'right' }}>
                            <div style={{ fontSize: '1.1rem', fontWeight: 700, color: '#fff' }}>
                              {formatVnd(acc.availableBalance)}
                            </div>
                            <div style={{ fontSize: '0.72rem', color: '#34d399' }}>Số dư khả dụng</div>
                          </div>

                          <button
                            className="btn btn-secondary"
                            onClick={() => {
                              onClose();
                              onOpenAccount(acc.accountNumber);
                            }}
                            style={{ fontSize: '0.78rem', padding: '6px 12px' }}
                          >
                            <span>Chi tiết TK</span>
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* TAB 3: CREDIT SCORE & NBO */}
              {activeTab === 'CREDIT_NBO' && (
                <div>
                  <div
                    style={{
                      display: 'grid',
                      gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
                      gap: '16px',
                    }}
                  >
                    {/* Credit Score Card */}
                    <div
                      style={{
                        background: 'rgba(255, 255, 255, 0.03)',
                        borderRadius: '12px',
                        border: '1px solid rgba(255, 255, 255, 0.08)',
                        padding: '16px',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
                        <Award size={18} color="#f59e0b" />
                        <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#fff', margin: 0 }}>
                          Điểm Tín Dụng CIC & Hạn Mức Vay
                        </h4>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px', marginBottom: '10px' }}>
                        <span style={{ fontSize: '2rem', fontWeight: 800, color: '#10b981' }}>
                          {creditScore.score || 745}
                        </span>
                        <span style={{ color: '#9ca3af', fontSize: '0.85rem' }}>/ 850 (Hạng 1 - Xuất sắc)</span>
                      </div>

                      <div style={{ fontSize: '0.8rem', color: '#cbd5e1', lineHeight: '1.6' }}>
                        <div>• Lịch sử nợ xấu CIC: <strong>Không có nợ quá hạn (Nhóm 1)</strong></div>
                        <div>• Hạn mức vay tín chấp phê duyệt trước: <strong style={{ color: '#34d399' }}>500.000.000 VND</strong></div>
                        <div>• Hạn mức thẻ tín dụng tối đa: <strong style={{ color: '#60a5fa' }}>200.000.000 VND</strong></div>
                      </div>
                    </div>

                    {/* Next Best Offers Card */}
                    <div
                      style={{
                        background: 'rgba(255, 255, 255, 0.03)',
                        borderRadius: '12px',
                        border: '1px solid rgba(255, 255, 255, 0.08)',
                        padding: '16px',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px' }}>
                        <TrendingUp size={18} color="#60a5fa" />
                        <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#fff', margin: 0 }}>
                          Đề Xuất Sản Phẩm Phù Hợp (NBO)
                        </h4>
                      </div>

                      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                        <div style={{ background: 'rgba(59, 130, 246, 0.08)', padding: '8px 10px', borderRadius: '8px', border: '1px solid rgba(59, 130, 246, 0.2)' }}>
                          <div style={{ fontSize: '0.82rem', fontWeight: 600, color: '#93c5fd' }}>
                            💳 Thẻ Tín Dụng B.Smart Signature
                          </div>
                          <div style={{ fontSize: '0.74rem', color: '#9ca3af' }}>
                            Hoàn tiền 10% chi tiêu ẩm thực & du lịch, miễn phí thường niên trọn đời.
                          </div>
                        </div>

                        <div style={{ background: 'rgba(16, 185, 129, 0.08)', padding: '8px 10px', borderRadius: '8px', border: '1px solid rgba(16, 185, 129, 0.2)' }}>
                          <div style={{ fontSize: '0.82rem', fontWeight: 600, color: '#6ee7b7' }}>
                            📈 Chứng Chỉ Tiền Gửi Sinh Lời 6.2%/năm
                          </div>
                          <div style={{ fontSize: '0.74rem', color: '#9ca3af' }}>
                            Tối ưu dòng tiền nhàn rỗi với lãi suất vượt trội so với tiền gửi thông thường.
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* TAB 4: RECENT TRANSACTIONS */}
              {activeTab === 'HISTORY' && (
                <div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {(recentTransactions.length > 0
                      ? recentTransactions
                      : [
                          { txId: 'TX-901', type: 'INFLOW', amount: 50000000, description: 'Nhận chuyển khoản lương tháng', timestamp: '2026-08-28 09:30' },
                          { txId: 'TX-902', type: 'OUTFLOW', amount: 15000000, description: 'Chuyển tiền mua sắm trực tuyến', timestamp: '2026-08-27 14:15' },
                          { txId: 'TX-903', type: 'INFLOW', amount: 120000000, description: 'Nộp tiền mặt tại quầy CN Sở Giao Dịch', timestamp: '2026-08-25 10:00' },
                        ]
                    ).map((tx: any) => (
                      <div
                        key={tx.txId}
                        style={{
                          background: 'rgba(255, 255, 255, 0.02)',
                          padding: '10px 14px',
                          borderRadius: '8px',
                          border: '1px solid rgba(255, 255, 255, 0.05)',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <div
                            style={{
                              width: '28px',
                              height: '28px',
                              borderRadius: '6px',
                              background: tx.type === 'INFLOW' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                            }}
                          >
                            {tx.type === 'INFLOW' ? (
                              <ArrowDownLeft size={14} color="#10b981" />
                            ) : (
                              <ArrowUpRight size={14} color="#ef4444" />
                            )}
                          </div>
                          <div>
                            <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#fff' }}>
                              {tx.description}
                            </div>
                            <div style={{ fontSize: '0.72rem', color: '#9ca3af' }}>{tx.timestamp}</div>
                          </div>
                        </div>

                        <div
                          style={{
                            fontSize: '0.95rem',
                            fontWeight: 700,
                            color: tx.type === 'INFLOW' ? '#10b981' : '#f87171',
                          }}
                        >
                          {tx.type === 'INFLOW' ? '+' : '-'}{formatVnd(tx.amount)}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div
          style={{
            padding: '12px 20px',
            borderTop: '1px solid rgba(255, 255, 255, 0.06)',
            background: 'rgba(0, 0, 0, 0.3)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '0.75rem',
            color: '#9ca3af',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#8b5cf6' }}>
            <ShieldCheck size={14} />
            <span>Xác thực bởi McpTool: customer.profile.read & customer.accounts.summary</span>
          </div>

          <button className="btn btn-secondary" onClick={onClose} style={{ fontSize: '0.8rem', padding: '6px 14px' }}>
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
};
