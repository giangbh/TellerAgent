import React from 'react';
import { ShieldCheck, Cpu, RefreshCw, Layers, UserCheck, Search, Command } from 'lucide-react';
import { Session } from '../types/teller';

interface HeaderProps {
  session: Session | null;
  onNewSession: () => void;
  onOpenMcp: () => void;
  onOpenSearch: () => void;
  loading: boolean;
}

export const Header: React.FC<HeaderProps> = ({
  session,
  onNewSession,
  onOpenMcp,
  onOpenSearch,
  loading,
}) => {
  const getStatusBadge = (status?: string) => {
    switch (status) {
      case 'POSTED':
        return <span className="badge badge-success">POSTED · HẠCH TOÁN THÀNH CÔNG</span>;
      case 'DRAFT_READY':
        return <span className="badge badge-success">DRAFT SẴN SÀNG</span>;
      case 'WAITING_HUMAN_INPUT':
        return <span className="badge badge-warning">CẦN BỔ SUNG THÔNG TIN</span>;
      case 'RISK_REVIEW':
        return <span className="badge badge-danger">CẢNH BÁO RỦI RO AML</span>;
      case 'AWAITING_APPROVAL':
        return <span className="badge badge-purple">CHỜ PHÊ DUYỆT</span>;
      case 'READY_TO_POST':
        return <span className="badge badge-success">ĐỦ ĐIỀU KIỆN POSTING</span>;
      case 'ASSISTANCE_READY':
        return <span className="badge badge-purple">ĐÃ TRẢ LỜI NGHIỆP VỤ</span>;
      default:
        return <span className="badge badge-neutral">PHIÊN ĐANG MỞ</span>;
    }
  };

  return (
    <header className="glass-panel" style={{ padding: '16px 24px', marginBottom: '20px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        
        {/* Brand & Title */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{
            width: '44px',
            height: '44px',
            borderRadius: '12px',
            background: 'linear-gradient(135deg, #10b981 0%, #3b82f6 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 16px rgba(16, 185, 129, 0.4)',
          }}>
            <ShieldCheck size={26} color="#ffffff" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h1 style={{ fontSize: '1.25rem', fontWeight: 800, letterSpacing: '-0.02em', background: 'linear-gradient(to right, #fff, #9ca3af)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                B.Smart Teller Copilot
              </h1>
              <span className="badge badge-purple" style={{ fontSize: '0.65rem' }}>
                <Cpu size={12} /> Spring Boot + MCP
              </span>
            </div>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Smart Counter Assistant · Bounded Autonomy & Maker-Checker Control Gate
            </p>
          </div>
        </div>

        {/* Global Search Omnibox Trigger */}
        <button
          onClick={onOpenSearch}
          style={{
            background: 'rgba(255, 255, 255, 0.05)',
            border: '1px solid rgba(255, 255, 255, 0.12)',
            borderRadius: '10px',
            padding: '8px 16px',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            color: '#9ca3af',
            cursor: 'pointer',
            minWidth: '280px',
            transition: 'all 0.2s ease',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.borderColor = '#60a5fa';
            e.currentTarget.style.background = 'rgba(59, 130, 246, 0.08)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.borderColor = 'rgba(255, 255, 255, 0.12)';
            e.currentTarget.style.background = 'rgba(255, 255, 255, 0.05)';
          }}
          title="Tìm kiếm nhanh toàn cục (Ctrl + K / ⌘K)"
        >
          <Search size={16} color="#60a5fa" />
          <span style={{ fontSize: '0.82rem', flex: 1, textAlign: 'left' }}>
            Tìm tài khoản, CIF, quy trình...
          </span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '3px', background: 'rgba(255, 255, 255, 0.08)', padding: '2px 6px', borderRadius: '5px', fontSize: '0.7rem', color: '#e5e7eb' }}>
            <Command size={11} />
            <span>K</span>
          </div>
        </button>

        {/* Counter Info Badges */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          {session && (
            <>
              <div style={{ background: 'rgba(255, 255, 255, 0.04)', padding: '6px 12px', borderRadius: '8px', border: '1px solid var(--border-color)', fontSize: '0.8rem' }}>
                <span style={{ color: 'var(--text-dim)' }}>Quầy: </span>
                <strong style={{ color: '#fff' }}>{session.counterId}</strong>
                <span style={{ color: 'var(--text-dim)', margin: '0 6px' }}>|</span>
                <span style={{ color: 'var(--text-dim)' }}>GDV: </span>
                <strong style={{ color: '#fff' }}>{session.tellerId}</strong>
              </div>

              <div style={{ background: 'rgba(255, 255, 255, 0.04)', padding: '6px 12px', borderRadius: '8px', border: '1px solid var(--border-color)', fontSize: '0.8rem' }}>
                <span style={{ color: 'var(--text-dim)' }}>Khách hàng: </span>
                <strong style={{ color: '#34d399' }}><UserCheck size={14} style={{ display: 'inline', verticalAlign: '-2px', marginRight: '4px' }} />{session.customerRef}</strong>
              </div>

              {getStatusBadge(session.status)}
            </>
          )}

          {/* Action Buttons */}
          <button 
            className="btn btn-secondary" 
            onClick={onOpenMcp}
            title="Xem danh mục 22 MCP Banking Tools và JSON-RPC logs"
            style={{ fontSize: '0.8rem', padding: '8px 14px' }}
          >
            <Layers size={15} color="#8b5cf6" />
            <span>MCP Explorer</span>
          </button>

          <button 
            className="btn btn-secondary" 
            onClick={onNewSession} 
            disabled={loading}
            style={{ fontSize: '0.8rem', padding: '8px 14px' }}
          >
            <RefreshCw size={15} className={loading ? 'animate-spin' : ''} />
            <span>Phiên Mới</span>
          </button>
        </div>

      </div>
    </header>
  );
};
