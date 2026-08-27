import React, { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, Sparkles, Brain, ChevronDown, ChevronRight } from 'lucide-react';
import { ChatMessage, Intent } from '../types/teller';

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

  const toggleThinking = (index: number) => {
    setExpandedThinking(prev => ({
      ...prev,
      [index]: !prev[index]
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim() || loading) return;
    onSendMessage(inputText.trim());
    setInputText('');
  };

  return (
    <div className="glass-panel" style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: '620px' }}>
      
      {/* Header */}
      <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            width: '32px',
            height: '32px',
            borderRadius: '8px',
            background: 'linear-gradient(135deg, #8b5cf6 0%, #3b82f6 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}>
            <Bot size={18} color="#fff" />
          </div>
          <div>
            <h2 style={{ fontSize: '0.95rem', fontWeight: 700 }}>AI Teller Copilot</h2>
            <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>DeepSeek AI & 23 MCP Tools</p>
          </div>
        </div>

        {intent && (
          <div style={{ textAlign: 'right' }}>
            <span className="badge badge-purple" style={{ fontSize: '0.7rem' }}>
              {intent.type}
            </span>
            <div style={{ fontSize: '0.675rem', color: 'var(--text-dim)', marginTop: '2px' }}>
              Độ tin cậy: {(intent.confidence * 100).toFixed(0)}%
            </div>
          </div>
        )}
      </div>

      {/* Messages Feed */}
      <div style={{ flex: 1, padding: '20px', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {messages.length === 0 && (
          <div style={{ textAlign: 'center', color: 'var(--text-dim)', marginTop: '60px', padding: '0 20px' }}>
            <Sparkles size={36} color="#8b5cf6" style={{ margin: '0 auto 12px', opacity: 0.6 }} />
            <h3 style={{ fontSize: '0.95rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '6px' }}>
              Sẵn sàng hỗ trợ Giao dịch viên
            </h3>
            <p style={{ fontSize: '0.8rem', lineHeight: '1.5' }}>
              Nhập yêu cầu của khách hàng bằng tiếng Việt tự nhiên hoặc chọn kịch bản mẫu ở trên.
            </p>
          </div>
        )}

        {messages.map((msg, index) => {
          const isUser = msg.role === 'user';
          const hasThinking = !isUser && msg.thinkingSteps && msg.thinkingSteps.length > 0;
          const isThinkingOpen = expandedThinking[index] ?? false;

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
                            padding: '1px 6px',
                            borderRadius: '4px',
                            background: 'rgba(139, 92, 246, 0.2)',
                            color: '#e9d5ff',
                            fontSize: '0.675rem',
                            fontWeight: 500
                          }}>
                            {msg.thinkingTimeMs > 1000 ? `${(msg.thinkingTimeMs / 1000).toFixed(1)}s` : `${msg.thinkingTimeMs}ms`}
                          </span>
                        )}
                      </div>
                      {isThinkingOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                    </button>

                    {isThinkingOpen && (
                      <div style={{
                        padding: '10px 14px 12px',
                        borderTop: '1px solid rgba(139, 92, 246, 0.15)',
                        fontSize: '0.75rem',
                        color: 'var(--text-muted)',
                        display: 'flex',
                        flexDirection: 'column',
                        gap: '6px',
                        lineHeight: '1.4',
                        background: 'rgba(0, 0, 0, 0.2)',
                      }}>
                        {msg.thinkingSteps!.map((step, sIdx) => (
                          <div key={sIdx} style={{ display: 'flex', alignItems: 'flex-start', gap: '6px' }}>
                            <span style={{ color: '#a855f7', marginTop: '1px' }}>•</span>
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
                  <div style={{ fontSize: '0.675rem', color: isUser ? 'rgba(255,255,255,0.6)' : 'var(--text-dim)', marginTop: '6px', textAlign: isUser ? 'right' : 'left' }}>
                    {new Date(msg.at).toLocaleTimeString('vi-VN')}
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
              padding: '12px 16px',
              borderRadius: '12px',
              background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.12) 0%, rgba(59, 130, 246, 0.08) 100%)',
              border: '1px solid rgba(139, 92, 246, 0.3)',
              fontSize: '0.8rem',
              color: '#e9d5ff',
              display: 'flex',
              flexDirection: 'column',
              gap: '6px',
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600 }}>
                <Sparkles size={14} color="#c084fc" />
                <span>DeepSeek AI đang suy nghĩ & điều phối MCP Tools...</span>
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
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

    </div>
  );
};
