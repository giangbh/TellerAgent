import React, { useState, useEffect } from 'react';
import { Header } from './components/Header';
import { ScenariosBar } from './components/ScenariosBar';
import { ChatAssistant } from './components/ChatAssistant';
import { PlanViewer } from './components/PlanViewer';
import { LiveDraftForm } from './components/LiveDraftForm';
import { ControlGatePanel } from './components/ControlGatePanel';
import { McpInspectorModal } from './components/McpInspectorModal';
import { GlobalSearchModal } from './components/GlobalSearchModal';
import { AccountDetailModal } from './components/AccountDetailModal';
import { Customer360Modal } from './components/Customer360Modal';
import { BootstrapData, Session } from './types/teller';
import * as api from './api/tellerApi';
import { SearchResultItem } from './api/tellerApi';

export const App: React.FC = () => {
  const [bootstrap, setBootstrap] = useState<BootstrapData | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isMcpOpen, setIsMcpOpen] = useState<boolean>(false);
  const [isSearchOpen, setIsSearchOpen] = useState<boolean>(false);
  const [selectedAccountNumber, setSelectedAccountNumber] = useState<string | null>(null);
  const [selectedCif, setSelectedCif] = useState<string | null>(null);

  // Global Keyboard shortcut for Omnibox (Cmd+K / Ctrl+K)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && (e.key === 'k' || e.key === 'K')) {
        e.preventDefault();
        setIsSearchOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Initialize Bootstrap & First Session
  useEffect(() => {
    const init = async () => {
      try {
        setLoading(true);
        // P0-3: POC — danh tính lấy từ phiên đăng nhập. Thay bằng OIDC ở môi trường thật.
        api.setCurrentActor({
          id: 'GDV001',
          name: 'Nguyễn Thị Hà',
          role: 'TELLER',
          branchId: 'CN-SGD-01',
        });
        const boot = await api.fetchBootstrap();
        setBootstrap(boot);
        const sess = await api.createSession();
        setSession(sess);
      } catch (err: unknown) {
        setErrorMsg(err instanceof Error ? err.message : 'Không thể kết nối đến Spring Boot backend.');
      } finally {
        setLoading(false);
      }
    };
    init();
  }, []);

  const handleNewSession = async () => {
    try {
      setLoading(true);
      setErrorMsg(null);
      const sess = await api.createSession();
      setSession(sess);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Lỗi khi tạo phiên mới.');
    } finally {
      setLoading(false);
    }
  };

  const handleSendMessage = async (text: string) => {
    if (!session) return;
    try {
      setLoading(true);
      setErrorMsg(null);
      const updated = await api.sendMessage(session.sessionId, text);
      setSession(updated);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Lỗi xử lý yêu cầu.');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (actor: 'customer' | 'teller' | 'supervisor') => {
    if (!session) return;
    try {
      setLoading(true);
      setErrorMsg(null);

      let updated;
      if (actor === 'customer') {
        // P0-4: xác nhận của khách hàng phải có bằng chứng thật, GDV nhập vào.
        const ref = window.prompt('Nhập mã bằng chứng khách hàng đã đồng ý (mã OTP / số phiếu có chữ ký):');
        if (!ref || !ref.trim()) {
          setErrorMsg('Chưa nhập bằng chứng xác nhận của khách hàng.');
          return;
        }
        updated = await api.recordCustomerConsent(session.sessionId, 'SIGNATURE', ref.trim());
      } else {
        updated = await api.approveSession(session.sessionId, actor);
      }
      setSession(updated);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : `Lỗi khi ${actor} phê duyệt.`);
    } finally {
      setLoading(false);
    }
  };

  const handleExecute = async () => {
    if (!session) return;
    try {
      setLoading(true);
      setErrorMsg(null);
      const updated = await api.executeSession(session.sessionId);
      setSession(updated);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Lỗi khi thực thi hạch toán.');
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmAndExecuteCash = async () => {
    if (!session) return;
    try {
      setLoading(true);
      setErrorMsg(null);
      const updated = await api.confirmAndExecuteCash(session.sessionId);
      setSession(updated);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Lỗi khi GDV xác nhận.');
    } finally {
      setLoading(false);
    }
  };

  const [chatWidth, setChatWidth] = useState<number>(() => {
    const saved = localStorage.getItem('teller_chat_width');
    return saved ? parseInt(saved, 10) : 480;
  });
  const [isDragging, setIsDragging] = useState<boolean>(false);

  const handleStartDrag = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      if (!isDragging) return;
      // Calculate new width relative to main container left edge
      const newWidth = Math.min(Math.max(e.clientX - 32, 340), 900);
      setChatWidth(newWidth);
      localStorage.setItem('teller_chat_width', newWidth.toString());
    };

    const handleMouseUp = () => {
      if (isDragging) {
        setIsDragging(false);
      }
    };

    if (isDragging) {
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
    }
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
  }, [isDragging]);

  const handleSetPresetWidth = (w: number) => {
    setChatWidth(w);
    localStorage.setItem('teller_chat_width', w.toString());
  };

  const handleSelectSearchResult = (item: SearchResultItem) => {
    if (item.actionType === 'VIEW_ACCOUNT') {
      setSelectedAccountNumber(item.id);
    } else if (item.actionType === 'VIEW_CUSTOMER') {
      setSelectedCif(item.id);
    } else if (item.prompt) {
      handleSendMessage(item.prompt);
    }
  };

  return (
    <div style={{ maxWidth: '1800px', margin: '0 auto', padding: '24px', userSelect: isDragging ? 'none' : 'auto' }}>
      
      {/* Top Header with Omnibox */}
      <Header
        session={session}
        onNewSession={handleNewSession}
        onOpenMcp={() => setIsMcpOpen(true)}
        onOpenSearch={() => setIsSearchOpen(true)}
        loading={loading}
      />

      {/* Global Error Banner */}
      {errorMsg && (
        <div style={{
          padding: '12px 18px',
          background: 'rgba(239, 68, 68, 0.2)',
          border: '1px solid rgba(239, 68, 68, 0.4)',
          borderRadius: '10px',
          color: '#fca5a5',
          fontSize: '0.875rem',
          marginBottom: '20px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}>
          <span><strong>Lỗi: </strong>{errorMsg}</span>
          <button
            onClick={() => setErrorMsg(null)}
            style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer', fontWeight: 'bold' }}
          >
            ✕
          </button>
        </div>
      )}

      {/* Quick Scenarios Bar & Layout Preset Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '16px', marginBottom: '16px' }}>
        <div style={{ flex: 1 }}>
          {bootstrap && (
            <ScenariosBar
              scenarios={bootstrap.scenarios}
              onSelectScenario={handleSendMessage}
              disabled={loading}
            />
          )}
        </div>

        {/* Layout Width Presets */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          background: 'rgba(15, 23, 42, 0.6)',
          padding: '4px 8px',
          borderRadius: '10px',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          fontSize: '0.75rem',
          flexShrink: 0
        }}>
          <span style={{ color: '#94A3B8', fontWeight: 500, marginRight: '4px' }}>📐 Độ rộng Copilot:</span>
          <button
            onClick={() => handleSetPresetWidth(380)}
            style={{
              background: chatWidth <= 400 ? 'rgba(59, 130, 246, 0.25)' : 'transparent',
              border: chatWidth <= 400 ? '1px solid #3B82F6' : '1px solid transparent',
              color: chatWidth <= 400 ? '#60A5FA' : '#94A3B8',
              borderRadius: '6px',
              padding: '3px 8px',
              cursor: 'pointer',
              fontSize: '0.725rem'
            }}
          >
            Gọn (380px)
          </button>
          <button
            onClick={() => handleSetPresetWidth(520)}
            style={{
              background: chatWidth > 400 && chatWidth <= 600 ? 'rgba(139, 92, 246, 0.25)' : 'transparent',
              border: chatWidth > 400 && chatWidth <= 600 ? '1px solid #8B5CF6' : '1px solid transparent',
              color: chatWidth > 400 && chatWidth <= 600 ? '#C084FC' : '#94A3B8',
              borderRadius: '6px',
              padding: '3px 8px',
              cursor: 'pointer',
              fontSize: '0.725rem'
            }}
          >
            Rộng (520px)
          </button>
          <button
            onClick={() => handleSetPresetWidth(700)}
            style={{
              background: chatWidth > 600 ? 'rgba(16, 185, 129, 0.25)' : 'transparent',
              border: chatWidth > 600 ? '1px solid #10B981' : '1px solid transparent',
              color: chatWidth > 600 ? '#34D399' : '#94A3B8',
              borderRadius: '6px',
              padding: '3px 8px',
              cursor: 'pointer',
              fontSize: '0.725rem'
            }}
          >
            Siêu rộng (700px)
          </button>
        </div>
      </div>

      {/* 3-Column Smart Counter Workspace with Draggable Divider */}
      <main style={{
        display: 'grid',
        gridTemplateColumns: `${chatWidth}px 12px minmax(380px, 1fr) minmax(320px, 380px)`,
        gap: '0px',
        alignItems: 'start',
      }}>
        
        {/* Left Column: Natural Language Copilot Chat */}
        <section style={{ paddingRight: '12px' }}>
          <ChatAssistant
            messages={session?.messages || []}
            intent={session?.intent}
            onSendMessage={handleSendMessage}
            loading={loading}
          />
        </section>

        {/* Draggable Divider Handle */}
        <div
          onMouseDown={handleStartDrag}
          title="Kéo sang trái/phải để điều chỉnh độ rộng khung Chat"
          style={{
            width: '12px',
            height: '100%',
            minHeight: '660px',
            cursor: 'col-resize',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'background 0.2s ease',
            background: isDragging ? 'rgba(139, 92, 246, 0.3)' : 'transparent',
            borderRadius: '6px'
          }}
          onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(139, 92, 246, 0.15)')}
          onMouseLeave={(e) => {
            if (!isDragging) e.currentTarget.style.background = 'transparent';
          }}
        >
          <div style={{
            width: '3px',
            height: '40px',
            borderRadius: '2px',
            background: isDragging ? '#A855F7' : 'rgba(255, 255, 255, 0.2)'
          }} />
        </div>

        {/* Center Column: Visual Plan + Live Action Draft */}
        <section style={{ display: 'flex', flexDirection: 'column', gap: '20px', paddingLeft: '8px', paddingRight: '20px' }}>
          {session && (
            <>
              <PlanViewer
                agentRuntime={session.agentRuntime}
                workflow={session.workflow}
              />
              <LiveDraftForm
                draft={session.transactionDraft}
                workflow={session.workflow}
              />
            </>
          )}
        </section>

        {/* Right Column: Policy Citations, AML Risk & Maker-Checker Control Gates */}
        <section>
          {session && (
            <ControlGatePanel
              session={session}
              onApprove={handleApprove}
              onExecute={handleExecute}
              onConfirmAndExecuteCash={handleConfirmAndExecuteCash}
              loading={loading}
            />
          )}
        </section>

      </main>

      {/* MCP Protocol & Capabilities Explorer Modal */}
      {bootstrap && (
        <McpInspectorModal
          isOpen={isMcpOpen}
          onClose={() => setIsMcpOpen(false)}
          capabilities={bootstrap.capabilities}
        />
      )}

      {/* Global Search Omnibox Modal (Ctrl+K / ⌘K) */}
      <GlobalSearchModal
        isOpen={isSearchOpen}
        onClose={() => setIsSearchOpen(false)}
        onSelectAction={handleSelectSearchResult}
      />

      {/* Direct Account Details Modal */}
      <AccountDetailModal
        isOpen={!!selectedAccountNumber}
        accountNumber={selectedAccountNumber}
        onClose={() => setSelectedAccountNumber(null)}
        onOpenCustomer360={(cif) => setSelectedCif(cif)}
        onTriggerAction={handleSendMessage}
      />

      {/* Direct Customer 360 Profile Modal */}
      <Customer360Modal
        isOpen={!!selectedCif}
        cif={selectedCif}
        onClose={() => setSelectedCif(null)}
        onOpenAccount={(acc) => setSelectedAccountNumber(acc)}
        onTriggerAction={handleSendMessage}
        onSelectAsActiveCustomer={(cif) => {
          if (session) {
            setSession({ ...session, customerRef: cif });
          }
        }}
      />

    </div>
  );
};

export default App;
