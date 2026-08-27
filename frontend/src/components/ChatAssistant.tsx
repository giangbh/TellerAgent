import React, { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, Sparkles, Brain, ChevronDown, ChevronRight, ShieldCheck, Maximize2, Minimize2, CornerDownLeft, X, MoveDiagonal } from 'lucide-react';
import { ChatMessage, Intent } from '../types/teller';
import { ObservabilityModal } from './ObservabilityModal';
import { MarkdownMessage } from './MarkdownMessage';

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
  const [inputRows, setInputRows] = useState(2);
  const [isExpandedView, setIsExpandedView] = useState(false);
  const chatEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

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

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!inputText.trim() || loading) return;
    onSendMessage(inputText.trim());
    setInputText('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const toggleThinking = (idx: number) => {
    setExpandedThinking((prev) => ({ ...prev, [idx]: !prev[idx] }));
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInputText(e.target.value);
    // Auto-adjust height
    const el = e.target;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 220) + 'px';
  };

  return (
    <div
      className="card glass-panel"
      style={{
        display: 'flex',
        flexDirection: 'column',
        height: isExpandedView ? '85vh' : '100%',
        minHeight: '660px',
        padding: '0',
        overflow: 'hidden',
        position: isExpandedView ? 'fixed' : 'relative',
        top: isExpandedView ? '80px' : 'auto',
        left: isExpandedView ? '5%' : 'auto',
        width: isExpandedView ? '90%' : '100%',
        zIndex: isExpandedView ? 1000 : 1,
        boxShadow: isExpandedView ? '0 25px 60px rgba(0,0,0,0.8), 0 0 40px rgba(139, 92, 246, 0.3)' : 'var(--shadow-card)',
        transition: 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)'
      }}
    >
      {/* Header */}
      <div style={{
        padding: '16px 20px',
        borderBottom: '1px solid var(--border-color)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        background: 'rgba(15, 23, 42, 0.7)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            width: '32px',
            height: '32px',
            borderRadius: '8px',
            background: 'linear-gradient(135deg, #8B5CF6, #3B82F6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 0 12px rgba(139, 92, 246, 0.4)'
          }}>
            <Bot size={18} color="#FFFFFF" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h3 style={{ margin: '0', fontSize: '1rem', fontWeight: '600', color: '#F8FAFC' }}>AI Teller Copilot</h3>
              <span className="badge badge-purple" style={{ fontSize: '0.675rem' }}>DeepSeek-R1 Active</span>
            </div>
            <p style={{ margin: 0, fontSize: '0.725rem', color: 'var(--text-muted)' }}>Hỗ trợ nghiệp vụ & Đối soát dữ liệu đa công cụ</p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          {intent && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem', color: '#38BDF8', background: 'rgba(56, 189, 248, 0.1)', padding: '3px 8px', borderRadius: '6px', border: '1px solid rgba(56, 189, 248, 0.2)' }}>
              <Sparkles size={12} color="#38BDF8" />
              <span>{intent.workflow}</span>
            </div>
          )}

          {/* Maximize / Minimize Panel Button */}
          <button
            onClick={() => setIsExpandedView(!isExpandedView)}
            title={isExpandedView ? "Thu nhỏ cửa sổ" : "Mở rộng toàn màn hình"}
            style={{
              background: isExpandedView ? 'rgba(139, 92, 246, 0.25)' : 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.12)',
              borderRadius: '8px',
              padding: '6px 10px',
              color: isExpandedView ? '#C084FC' : '#94A3B8',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '0.75rem',
              fontWeight: 500
            }}
          >
            {isExpandedView ? <Minimize2 size={14} /> : <Maximize2 size={14} />}
            <span>{isExpandedView ? 'Thu nhỏ' : 'Phóng to'}</span>
          </button>
        </div>
      </div>

      {/* Messages List */}
      <div style={{ flex: 1, overflowY: 'auto', padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {messages.length === 0 && (
          <div style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '40px 0' }}>
            <Bot size={40} style={{ margin: '0 auto 12px', opacity: '0.3' }} />
            <p style={{ margin: 0, fontSize: '0.875rem' }}>Chào GDV! Hãy nhập yêu cầu của khách hàng hoặc click kịch bản mẫu bên trên để tôi hỗ trợ tự động.</p>
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
                width: '30px',
                height: '30px',
                borderRadius: '8px',
                flexShrink: 0,
                background: isUser ? 'rgba(59, 130, 246, 0.2)' : 'rgba(139, 92, 246, 0.2)',
                border: `1px solid ${isUser ? 'rgba(59, 130, 246, 0.4)' : 'rgba(139, 92, 246, 0.4)'}`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}>
                {isUser ? <User size={16} color="#60a5fa" /> : <Bot size={16} color="#c084fc" />}
              </div>

              <div style={{
                maxWidth: isExpandedView ? '75%' : '88%',
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
                  padding: '14px 18px',
                  borderRadius: '14px',
                  fontSize: '0.9rem',
                  lineHeight: '1.6',
                  background: isUser
                    ? 'linear-gradient(135deg, #1e3a8a 0%, #1e40af 100%)'
                    : 'rgba(30, 41, 59, 0.6)',
                  color: isUser ? '#ffffff' : 'var(--text-main)',
                  border: isUser ? 'none' : '1px solid var(--border-color)',
                  boxShadow: isUser ? '0 4px 12px rgba(30, 58, 138, 0.3)' : 'none',
                }}>
                  <MarkdownMessage content={msg.text} isUser={isUser} />
                  
                  {/* Message Footer with Timestamp & AI Judge Button */}
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: isUser ? 'flex-end' : 'space-between',
                    marginTop: '8px',
                    paddingTop: '6px',
                    borderTop: isUser ? 'none' : '1px solid rgba(255, 255, 255, 0.06)'
                  }}>
                    <div style={{ fontSize: '0.675rem', color: isUser ? 'rgba(255,255,255,0.6)' : 'var(--text-dim)' }}>
                      {new Date(msg.at).toLocaleTimeString('vi-VN')}
                    </div>

                    {!isUser && (
                      <button
                        onClick={() => handleOpenObservability(msg, index)}
                        style={{
                          background: 'rgba(56, 189, 248, 0.12)',
                          border: '1px solid rgba(56, 189, 248, 0.3)',
                          borderRadius: '6px',
                          padding: '3px 9px',
                          fontSize: '0.7rem',
                          color: '#38BDF8',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '4px',
                          fontWeight: 500,
                          transition: 'all 0.2s ease'
                        }}
                        onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(56, 189, 248, 0.25)')}
                        onMouseLeave={(e) => (e.currentTarget.style.background = 'rgba(56, 189, 248, 0.12)')}
                      >
                        <ShieldCheck size={13} />
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
              width: '30px',
              height: '30px',
              borderRadius: '8px',
              background: 'rgba(139, 92, 246, 0.2)',
              border: '1px solid rgba(139, 92, 246, 0.4)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              flexShrink: 0,
            }}>
              <Brain size={16} color="#c084fc" className="animate-spin" />
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

      {/* Upgraded Multi-line Expandable Input Box */}
      <form
        onSubmit={handleSubmit}
        style={{
          padding: '14px 18px',
          borderTop: '1px solid var(--border-color)',
          background: 'rgba(15, 23, 42, 0.85)',
          display: 'flex',
          flexDirection: 'column',
          gap: '8px'
        }}
      >
        <div style={{
          position: 'relative',
          display: 'flex',
          alignItems: 'flex-end',
          borderRadius: '12px',
          background: 'rgba(0, 0, 0, 0.35)',
          border: '1px solid rgba(255, 255, 255, 0.12)',
          padding: '4px 8px 4px 12px',
          boxShadow: 'inset 0 2px 4px rgba(0,0,0,0.3)'
        }}>
          <textarea
            ref={textareaRef}
            value={inputText}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
            placeholder="Nhập yêu cầu nghiệp vụ ngân hàng (VD: Trích lục sao kê tài khoản 3456789, phân tích dòng tiền vào ra, giao dịch lớn nhất...)"
            disabled={loading}
            rows={inputRows}
            style={{
              flex: 1,
              padding: '8px 4px',
              fontSize: '0.875rem',
              lineHeight: '1.5',
              background: 'transparent',
              border: 'none',
              color: '#F8FAFC',
              outline: 'none',
              resize: 'vertical',
              minHeight: '52px',
              maxHeight: '220px',
              fontFamily: 'inherit'
            }}
          />

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', paddingBottom: '6px' }}>
            {inputText.trim().length > 0 && (
              <button
                type="button"
                onClick={() => {
                  setInputText('');
                  if (textareaRef.current) textareaRef.current.style.height = 'auto';
                }}
                title="Xóa nội dung"
                style={{
                  background: 'rgba(255, 255, 255, 0.08)',
                  border: 'none',
                  borderRadius: '6px',
                  padding: '6px',
                  color: '#94A3B8',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                <X size={14} />
              </button>
            )}

            <button
              type="submit"
              disabled={loading || !inputText.trim()}
              className="btn btn-purple"
              style={{
                padding: '8px 16px',
                borderRadius: '8px',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                fontSize: '0.825rem',
                fontWeight: 600
              }}
            >
              <Send size={15} />
              <span>Gửi</span>
            </button>
          </div>
        </div>

        {/* Input Footer Helper Tools */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          fontSize: '0.7rem',
          color: '#64748B',
          padding: '0 4px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
              <CornerDownLeft size={11} /> Nhấn <strong>Enter</strong> để gửi • <strong>Shift + Enter</strong> để xuống dòng
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {inputText.length > 0 && (
              <span>{inputText.length} ký tự</span>
            )}
            <button
              type="button"
              onClick={() => {
                const nextRows = inputRows === 2 ? 5 : 2;
                setInputRows(nextRows);
                if (textareaRef.current) {
                  textareaRef.current.rows = nextRows;
                  textareaRef.current.style.height = 'auto';
                }
              }}
              style={{
                background: 'transparent',
                border: 'none',
                color: '#94A3B8',
                cursor: 'pointer',
                fontSize: '0.7rem',
                display: 'flex',
                alignItems: 'center',
                gap: '3px'
              }}
            >
              <MoveDiagonal size={11} />
              {inputRows === 2 ? 'Mở rộng 5 dòng' : 'Thu gọn 2 dòng'}
            </button>
          </div>
        </div>
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
