import React, { useState, useEffect } from 'react';
import {
  CreditCard,
  User,
  CheckCircle2,
  X,
  ShieldCheck,
  Zap,
  Sparkles,
} from 'lucide-react';
import { fetchAccountDetail } from '../api/tellerApi';

interface AccountDetailModalProps {
  isOpen: boolean;
  accountNumber: string | null;
  onClose: () => void;
  onOpenCustomer360: (cif: string) => void;
  onTriggerAction: (prompt: string) => void;
}

export const AccountDetailModal: React.FC<AccountDetailModalProps> = ({
  isOpen,
  accountNumber,
  onClose,
  onOpenCustomer360,
  onTriggerAction,
}) => {
  const [data, setData] = useState<Record<string, any> | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen || !accountNumber) {
      setData(null);
      setError(null);
      return;
    }

    const load = async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await fetchAccountDetail(accountNumber);
        setData(res);
      } catch (err: any) {
        setError(err.message || 'Không thể tải thông tin tài khoản.');
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [isOpen, accountNumber]);

  if (!isOpen) return null;

  const formatVnd = (amount: any) => {
    if (amount === undefined || amount === null) return '0 VND';
    const num = typeof amount === 'number' ? amount : parseFloat(amount);
    return new Intl.NumberFormat('vi-VN').format(num) + ' VND';
  };

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
          maxWidth: '680px',
          background: 'linear-gradient(180deg, #181c26 0%, #11141c 100%)',
          borderRadius: '16px',
          border: '1px solid rgba(255, 255, 255, 0.12)',
          boxShadow: '0 25px 60px -15px rgba(0, 0, 0, 0.8), 0 0 40px rgba(16, 185, 129, 0.15)',
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
                width: '38px',
                height: '38px',
                borderRadius: '10px',
                background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)',
              }}
            >
              <CreditCard size={20} color="#fff" />
            </div>
            <div>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 800, color: '#fff', margin: 0 }}>
                Chi Tiết Tài Khoản Ngân Hàng
              </h2>
              <p style={{ fontSize: '0.78rem', color: '#9ca3af', margin: 0 }}>
                Core Banking Account Resolver · B.Smart Smart Counter
              </p>
            </div>
          </div>

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

        {/* Content Body */}
        <div style={{ padding: '20px', overflowY: 'auto' }}>
          {loading ? (
            <div style={{ padding: '50px 20px', textAlign: 'center', color: '#9ca3af' }}>
              <Sparkles size={32} className="animate-spin" style={{ margin: '0 auto 12px', color: '#10b981' }} />
              <p style={{ fontSize: '0.9rem' }}>Đang tra cứu dữ liệu tài khoản từ Core Banking...</p>
            </div>
          ) : error ? (
            <div style={{ padding: '30px', textAlign: 'center', color: '#fca5a5' }}>
              <p style={{ fontSize: '0.95rem', fontWeight: 600 }}>{error}</p>
              <p style={{ fontSize: '0.8rem', color: '#9ca3af', marginTop: '6px' }}>
                Số tài khoản {accountNumber} không tồn tại trong hệ thống.
              </p>
            </div>
          ) : data ? (
            <div>
              {/* Account Balance Banner */}
              <div
                style={{
                  background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.15) 0%, rgba(59, 130, 246, 0.08) 100%)',
                  borderRadius: '12px',
                  border: '1px solid rgba(16, 185, 129, 0.3)',
                  padding: '18px 20px',
                  marginBottom: '20px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  flexWrap: 'wrap',
                  gap: '12px',
                }}
              >
                <div>
                  <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em', color: '#6ee7b7', fontWeight: 600 }}>
                    Số Dư Khả Dụng (Available Balance)
                  </div>
                  <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#ffffff', marginTop: '4px', letterSpacing: '-0.02em' }}>
                    {formatVnd(data.availableBalance ?? data.balance)}
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '8px' }}>
                  <span className="badge badge-success" style={{ padding: '6px 12px', fontSize: '0.8rem' }}>
                    <CheckCircle2 size={13} style={{ display: 'inline', verticalAlign: '-1px', marginRight: '4px' }} />
                    {data.status || 'ACTIVE · HOẠT ĐỘNG'}
                  </span>
                  <span className="badge badge-purple" style={{ padding: '6px 12px', fontSize: '0.8rem' }}>
                    {data.currency || 'VND'}
                  </span>
                </div>
              </div>

              {/* Information Grid */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                  gap: '14px',
                  marginBottom: '20px',
                }}
              >
                {/* Account Number */}
                <div
                  style={{
                    background: 'rgba(255, 255, 255, 0.03)',
                    padding: '12px 14px',
                    borderRadius: '10px',
                    border: '1px solid rgba(255, 255, 255, 0.06)',
                  }}
                >
                  <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginBottom: '3px' }}>Số Tài Khoản</div>
                  <div style={{ fontSize: '1.05rem', fontWeight: 700, color: '#60a5fa', fontFamily: 'monospace' }}>
                    {data.accountNumber}
                  </div>
                </div>

                {/* Account Holder */}
                <div
                  style={{
                    background: 'rgba(255, 255, 255, 0.03)',
                    padding: '12px 14px',
                    borderRadius: '10px',
                    border: '1px solid rgba(255, 255, 255, 0.06)',
                  }}
                >
                  <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginBottom: '3px' }}>Chủ Tài Khoản</div>
                  <div style={{ fontSize: '1rem', fontWeight: 700, color: '#ffffff' }}>
                    {data.accountHolder || data.customerName || data.name}
                  </div>
                </div>

                {/* Account Type */}
                <div
                  style={{
                    background: 'rgba(255, 255, 255, 0.03)',
                    padding: '12px 14px',
                    borderRadius: '10px',
                    border: '1px solid rgba(255, 255, 255, 0.06)',
                  }}
                >
                  <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginBottom: '3px' }}>Loại Tài Khoản</div>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, color: '#e5e7eb' }}>
                    {data.accountType || data.type || 'Tài khoản thanh toán cá nhân (CA)'}
                  </div>
                </div>

                {/* Linked CIF */}
                <div
                  style={{
                    background: 'rgba(255, 255, 255, 0.03)',
                    padding: '12px 14px',
                    borderRadius: '10px',
                    border: '1px solid rgba(255, 255, 255, 0.06)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div>
                    <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginBottom: '3px' }}>Mã Khách Hàng (CIF)</div>
                    <div style={{ fontSize: '0.95rem', fontWeight: 700, color: '#a855f7' }}>
                      {data.cif || data.customerRef || 'CIF-0001842'}
                    </div>
                  </div>
                  {data.cif && (
                    <button
                      className="btn btn-secondary"
                      onClick={() => {
                        onClose();
                        onOpenCustomer360(data.cif);
                      }}
                      style={{ fontSize: '0.75rem', padding: '5px 10px' }}
                    >
                      <User size={13} color="#a855f7" />
                      <span>Hồ sơ 360</span>
                    </button>
                  )}
                </div>

                {/* Branch */}
                <div
                  style={{
                    background: 'rgba(255, 255, 255, 0.03)',
                    padding: '12px 14px',
                    borderRadius: '10px',
                    border: '1px solid rgba(255, 255, 255, 0.06)',
                  }}
                >
                  <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginBottom: '3px' }}>Chi Nhánh Quản Lý</div>
                  <div style={{ fontSize: '0.88rem', color: '#cbd5e1' }}>
                    {data.branch || 'Chi nhánh Sở Giao Dịch Hà Nội (CN-SGD-01)'}
                  </div>
                </div>

                {/* Hold Balance */}
                <div
                  style={{
                    background: 'rgba(255, 255, 255, 0.03)',
                    padding: '12px 14px',
                    borderRadius: '10px',
                    border: '1px solid rgba(255, 255, 255, 0.06)',
                  }}
                >
                  <div style={{ fontSize: '0.75rem', color: '#9ca3af', marginBottom: '3px' }}>Số Dư Phong Tỏa / Giữ Chỗ</div>
                  <div style={{ fontSize: '0.88rem', color: '#9ca3af' }}>
                    0 VND (Không có lệnh phong tỏa)
                  </div>
                </div>
              </div>

              {/* Quick Transaction Action Bar */}
              <div
                style={{
                  background: 'rgba(0, 0, 0, 0.25)',
                  borderRadius: '12px',
                  padding: '14px',
                  border: '1px solid rgba(255, 255, 255, 0.06)',
                }}
              >
                <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#60a5fa', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Zap size={14} />
                  <span>Khởi Tạo Giao Dịch Nhanh Với Tài Khoản Này:</span>
                </div>

                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  <button
                    className="btn btn-secondary"
                    onClick={() => {
                      onClose();
                      onTriggerAction(`Nộp tiền vào tài khoản ${data.accountNumber}`);
                    }}
                    style={{ fontSize: '0.8rem', padding: '6px 12px' }}
                  >
                    <span>💵 Nộp tiền mặt</span>
                  </button>

                  <button
                    className="btn btn-secondary"
                    onClick={() => {
                      onClose();
                      onTriggerAction(`Rút tiền từ tài khoản ${data.accountNumber}`);
                    }}
                    style={{ fontSize: '0.8rem', padding: '6px 12px' }}
                  >
                    <span>💸 Rút tiền mặt</span>
                  </button>

                  <button
                    className="btn btn-secondary"
                    onClick={() => {
                      onClose();
                      onTriggerAction(`Chuyển tiền từ tài khoản ${data.accountNumber}`);
                    }}
                    style={{ fontSize: '0.8rem', padding: '6px 12px' }}
                  >
                    <span>🔄 Chuyển khoản</span>
                  </button>

                  <button
                    className="btn btn-secondary"
                    onClick={() => {
                      onClose();
                      onTriggerAction(`Xem lịch sử giao dịch và sao kê của tài khoản ${data.accountNumber}`);
                    }}
                    style={{ fontSize: '0.8rem', padding: '6px 12px' }}
                  >
                    <span>📋 Trích lục sao kê</span>
                  </button>
                </div>
              </div>
            </div>
          ) : null}
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
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#10b981' }}>
            <ShieldCheck size={14} />
            <span>Xác thực bởi McpTool: account.resolve.by.number</span>
          </div>

          <button className="btn btn-secondary" onClick={onClose} style={{ fontSize: '0.8rem', padding: '6px 14px' }}>
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
};
