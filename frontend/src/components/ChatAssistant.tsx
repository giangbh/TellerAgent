import React, { useState, useRef, useEffect } from 'react';
import { Send, Bot, User, Sparkles } from 'lucide-react';
import { ChatMessage, Intent } from '../types/teller';

interface ChatAssistantProps {
  messages: ChatMessage[];
  intent?: Intent | null;
  onSendMessage: (text: string) => void;
  loading: boolean;
}

export const ChatAssistant: React.FC<ChatAssistantProps> = ({ messages, intent, onSendMessage, loading }) => {
  const [inputText, setInputText] = useState('');
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

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
            <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Natural Language Intent Parsing</p>
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
                maxWidth: '82%',
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
              }}>
                {msg.text}
                <div style={{ fontSize: '0.675rem', color: isUser ? 'rgba(255,255,255,0.6)' : 'var(--text-dim)', marginTop: '4px', textAlign: isUser ? 'right' : 'left' }}>
                  {new Date(msg.at).toLocaleTimeString('vi-VN')}
                </div>
              </div>
            </div>
          );
        })}

        {loading && (
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <div style={{
              width: '28px',
              height: '28px',
              borderRadius: '8px',
              background: 'rgba(139, 92, 246, 0.2)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <Bot size={15} color="#c084fc" />
            </div>
            <div style={{ padding: '10px 16px', borderRadius: '12px', background: 'rgba(255, 255, 255, 0.05)', border: '1px solid var(--border-color)', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              <span className="animate-pulse">Copilot đang phân tích & gọi MCP Tools...</span>
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
