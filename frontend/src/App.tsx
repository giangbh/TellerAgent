import React, { useState, useEffect } from 'react';
import { Header } from './components/Header';
import { ScenariosBar } from './components/ScenariosBar';
import { ChatAssistant } from './components/ChatAssistant';
import { PlanViewer } from './components/PlanViewer';
import { LiveDraftForm } from './components/LiveDraftForm';
import { ControlGatePanel } from './components/ControlGatePanel';
import { McpInspectorModal } from './components/McpInspectorModal';
import { BootstrapData, Session } from './types/teller';
import * as api from './api/tellerApi';

export const App: React.FC = () => {
  const [bootstrap, setBootstrap] = useState<BootstrapData | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isMcpOpen, setIsMcpOpen] = useState<boolean>(false);

  // Initialize Bootstrap & First Session
  useEffect(() => {
    const init = async () => {
      try {
        setLoading(true);
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
      const updated = await api.approveSession(session.sessionId, actor);
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

  return (
    <div style={{ maxWidth: '1600px', margin: '0 auto', padding: '24px' }}>
      
      {/* Top Header */}
      <Header
        session={session}
        onNewSession={handleNewSession}
        onOpenMcp={() => setIsMcpOpen(true)}
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

      {/* Quick Scenarios Bar */}
      {bootstrap && (
        <ScenariosBar
          scenarios={bootstrap.scenarios}
          onSelectScenario={handleSendMessage}
          disabled={loading}
        />
      )}

      {/* 3-Column Smart Counter Workspace */}
      <main style={{
        display: 'grid',
        gridTemplateColumns: 'minmax(320px, 380px) minmax(420px, 1fr) minmax(320px, 380px)',
        gap: '20px',
        alignItems: 'start',
      }}>
        
        {/* Left Column: Natural Language Copilot Chat */}
        <section>
          <ChatAssistant
            messages={session?.messages || []}
            intent={session?.intent}
            onSendMessage={handleSendMessage}
            loading={loading}
          />
        </section>

        {/* Center Column: Visual Plan + Live Action Draft */}
        <section style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
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

    </div>
  );
};

export default App;
