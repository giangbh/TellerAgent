import React, { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, Sparkles, Brain, ChevronDown, ChevronRight, ShieldCheck } from 'lucide-react';
import { ChatMessage, Intent } from '../types/teller';
import { ObservabilityModal } from './ObservabilityModal';

interface ChatAssistantProps {
  messages: ChatMessage[];
  intent?: Intent | null;
  onSendMessage: (text: string) => void;
  loading: boolean;
}

const LIVE_THINKING_STAGES = [
  '🧠 Đang phân tích ngữ cảnh & ý định người dùng...',
  '🔍 Đang đối chiếu 23 MCP Banking Tools Schema...',
  '⚡ Đang bóc tách thực thể & đối soát dữ liệu ngân hàng...',
  '🛡️ Đang kiểm tra chính sách an toàn RBAC & Hạn mức...',
  '✨ Đang lập kế hoạch ReAct & sinh phản hồi nghiệp vụ...'
];

export const ChatAssistant: React.FC<ChatAssistantProps> = ({ messages, intent, onSendMessage, loading }) => {
  const [inputText, setInputText] = useState('');
  const [expandedThinking, setExpandedThinking] = useState<Record<number, boolean>>({});
  const [currentStageIdx, setCurrentStageIdx] = useState(0);
  const [obsModalOpen, setObsModalOpen] = useState(false);
  const [selectedObsMsg, setSelectedObsMsg] = useState<ChatMessage | null>(null);
  const [selectedObsUserPrompt, setSelectedObsUserPrompt] = useState('');
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  // Rotate thinking stage text when loading
  useEffect(() => {
    if (!loading) {
      setCurrentStageIdx(0);
      return;
    }
    const interval = setInterval(() => {
      setCurrentStageIdx((prev) => (prev + 1) % LIVE_THINKING_STAGES.length);
    }, 1200);
    return () => clearInterval(interval);
  }, [loading]);

  // Auto expand latest assistant thinking
  useEffect(() => {
    if (messages.length > 0) {
      const lastIdx = messages.length - 1;
      if (messages[lastIdx].role === 'assistant' && messages[lastIdx].thinkingSteps?.length) {
        setExpandedThinking(prev => ({ ...prev, [lastIdx]: true }));
      }
    }
  }, [messages]);

  const handleOpenObservability = (msg: ChatMessage, idx: number) => {
    let prompt = '';
    for (let i = idx - 1; i >= 0; i--) {
      if (messages[i].role === 'user') {
        prompt = messages[i].text;
        break;
      }
    }
    setSelectedObsMsg(msg);
    setSelectedObsUserPrompt(prompt);
    setObsModalOpen(true);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim() || loading) return;
    onSendMessage(inputText);
    setInputText('');
  };

  const toggleThinking = (idx: number) => {
    setExpandedThinking((prev) => ({ ...prev, [idx]: !prev[idx] }));
  };

  return (
    <div className="card glass-panel" style={{ display: 'flex', flexDirection: 'column', height: '100%', padding: '0', overflow: 'hidden' }}>
      
      {/* Header */}
      <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Bot size={18} color="var(--primary-light)" />
          <h3 style={{ margin: '0', fontSize: '1rem', fontWeight: '600' }}>AI Teller Copilot</h3>
          <span className="badge badge-purple" style={{ fontSize: '0.7rem' }}>DeepSeek-R1 Active</span>
        </div>
        {intent && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem', color: 'var(--text-dim)' }}>
            <Sparkles size={12} color="var(--warning)" />
            <span>Workflow: <strong>{intent.workflow}</strong></span>
          </div>
        )}
      </div>

      {/* Messages List */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {messages.length === 0 && (
          <div style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '40px 0' }}>
            <Bot size={40} style={{ margin: '0 auto 12px', opacity: '0.3' }} />
            <p style={{ margin: 0, fontSize: '0.875rem' }}>Chào GDV! Hãy nhập yêu cầu của khách hàng hoặc click kịch bản mẫu bên trái để tôi hỗ trợ tự động.</p>
          </div>
        )}

        {messages.map((msg, index) => {
          const isUser = msg.role === 'user';
          const hasThinking = msg.thinkingSteps && msg.thinkingSteps.length > 0;
          const isExpanded = expandedThinking[index] !== undefined ? expandedThinking[index] : true;

          return (
            <div
              key={index}
              className="animate-fade-in"
              style={{
                display: 'flex',
                gap: '10px',
                alignItems: 'flex-start',
                flexDirection: isUser ? 'row-reverse' : 'row',
              }}
            >
              <div style={{
                width: '28px',
                height: '28px',
                borderRadius: '8px',
                flexShrink: 0,
                background: isUser ? 'rgba(59, 130, 246, 0.2)' : 'rgba(139, 92, 246, 0.2)',
                border: `1px solid ${isUser ? 'rgba(59, 130, 246, 0.4)' : 'rgba(139, 92, 246, 0.4)'}`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}>
                {isUser ? <User size={15} color="#60a5fa" /> : <Bot size={15} color="#c084fc" />}
              </div>

              <div style={{
                maxWidth: '85%',
                display: 'flex',
                flexDirection: 'column',
                gap: '8px',
              }}>
                {/* Collapsible DeepSeek Thinking Process Block */}
                {hasThinking && (
                  <div style={{
                    borderRadius: '10px',
                    background: 'rgba(139, 92, 246, 0.08)',
                    border: '1px solid rgba(139, 92, 246, 0.25)',
                    overflow: 'hidden',
                    transition: 'all 0.2s ease',
                  }}>
                    <button
                      onClick={() => toggleThinking(index)}
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        background: 'transparent',
                        border: 'none',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        cursor: 'pointer',
                        color: '#c084fc',
                        fontSize: '0.775rem',
                        fontWeight: 600,
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Brain size={14} className="animate-pulse" />
                        <span>Quá trình suy nghĩ</span>
                        {msg.thinkingTimeMs && (
                          <span style={{
                            fontSize: '0.675rem',
                            color: '#a78bfa',
                            background: 'rgba(139, 92, 246, 0.2)',
                            padding: '1px 6px',
                            borderRadius: '4px',
                            fontWeight: 400,
                            marginLeft: '4px'
                          }}>
                            {msg.thinkingTimeMs < 1000 ? `${msg.thinkingTimeMs}ms` : `${(msg.thinkingTimeMs / 1000).toFixed(1)}s`}
                          </span>
                        )}
                      </div>
                      {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                    </button>

                    {isExpanded && (
                      <div style={{
                        padding: '8px 12px 10px 12px',
                        borderTop: '1px solid rgba(139, 92, 246, 0.15)',
                        fontSize: '0.75rem',
                        color: '#cbd5e1',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '6px',
                        lineHeight: '1.4',
                      }}>
                        {msg.thinkingSteps!.map((step, sIdx) => (
                          <div key={sIdx} style={{ display: 'flex', alignItems: 'flex-start', gap: '6px' }}>
                            <span>{step}</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}

                {/* Main Message Content */}
                <div style={{
                  padding: '12px 16px',
                  borderRadius: '14px',
                  fontSize: '0.875rem',
                  lineHeight: '1.5',
                  background: isUser
                    ? 'linear-gradient(135deg, #1e3a8a 0%, #1e40af 100%)'
                    : 'rgba(255, 255, 255, 0.05)',
                  color: isUser ? '#ffffff' : 'var(--text-main)',
                  border: isUser ? 'none' : '1px solid var(--border-color)',
                  boxShadow: isUser ? '0 4px 12px rgba(30, 58, 138, 0.3)' : 'none',
                  whiteSpace: 'pre-line',
                }}>
                  {msg.text}
                  
                  {/* Message Footer with Timestamp & AI Judge Button */}
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: isUser ? 'flex-end' : 'space-between',
                    marginTop: '8px',
                    paddingTop: '6px',
                    borderTop: isUser ? 'none' : '1px solid rgba(255, 255, 255, 0.05)'
                  }}>
                    <div style={{ fontSize: '0.675rem', color: isUser ? 'rgba(255,255,255,0.6)' : 'var(--text-dim)' }}>
                      {new Date(msg.at).toLocaleTimeString('vi-VN')}
                    </div>

                    {!isUser && (
                      <button
                        onClick={() => handleOpenObservability(msg, index)}
                        style={{
                          background: 'rgba(56, 189, 248, 0.1)',
                          border: '1px solid rgba(56, 189, 248, 0.25)',
                          borderRadius: '6px',
                          padding: '2px 8px',
                          fontSize: '0.7rem',
                          color: '#38BDF8',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '4px',
                          fontWeight: 500,
                          transition: 'all 0.2s ease'
                        }}
                        onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(56, 189, 248, 0.2)')}
                        onMouseLeave={(e) => (e.currentTarget.style.background = 'rgba(56, 189, 248, 0.1)')}
                      >
                        <ShieldCheck size={12} />
                        <span>AI Judge & Trace</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          );
        })}

        {/* Live Animated Thinking Box during generation */}
        {loading && (
          <div style={{ display: 'flex', gap: '10px', alignItems: 'flex-start' }}>
            <div style={{
              width: '28px',
              height: '28px',
              borderRadius: '8px',
              background: 'rgba(139, 92, 246, 0.2)',
              border: '1px solid rgba(139, 92, 246, 0.4)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}>
              <Brain size={15} color="#c084fc" className="animate-spin" />
            </div>

            <div style={{
              maxWidth: '85%',
              display: 'flex',
              flexDirection: 'column',
              gap: '6px',
            }}>
              <div style={{
                borderRadius: '10px',
                background: 'rgba(139, 92, 246, 0.12)',
                border: '1px solid rgba(139, 92, 246, 0.4)',
                padding: '10px 14px',
                fontSize: '0.8rem',
                color: '#e2e8f0',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                boxShadow: '0 0 15px rgba(139, 92, 246, 0.2)',
              }}>
                <div style={{
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  backgroundColor: '#c084fc',
                  boxShadow: '0 0 8px #c084fc',
                }} className="animate-pulse" />
                {LIVE_THINKING_STAGES[currentStageIdx]}
              </div>
            </div>
          </div>
        )}

        <div ref={chatEndRef} />
      </div>

      {/* Input Box */}
      <form onSubmit={handleSubmit} style={{ padding: '16px 20px', borderTop: '1px solid var(--border-color)', display: 'flex', gap: '10px' }}>
        <input
          type="text"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder="Nhập yêu cầu (VD: chuyển 50tr cho An tại VCB STK 012345678901)..."
          disabled={loading}
          style={{
            flex: 1,
            padding: '12px 16px',
            fontSize: '0.875rem',
            borderRadius: '10px',
            background: 'rgba(0, 0, 0, 0.3)',
            border: '1px solid var(--border-color)',
            color: '#ffffff',
            outline: 'none',
          }}
        />
        <button
          type="submit"
          disabled={loading || !inputText.trim()}
          className="btn btn-purple"
          style={{ padding: '12px 20px' }}
        >
          <Send size={16} />
          <span>Gửi</span>
        </button>
      </form>

      {/* Observability & LLM Judge Modal */}
      <ObservabilityModal
        isOpen={obsModalOpen}
        onClose={() => setObsModalOpen(false)}
        session={null}
        selectedMessage={selectedObsMsg}
        userPrompt={selectedObsUserPrompt}
      />
    </div>
  );
};
