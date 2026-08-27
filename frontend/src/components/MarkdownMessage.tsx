import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface MarkdownMessageProps {
  content: string;
  isUser?: boolean;
}

export const MarkdownMessage: React.FC<MarkdownMessageProps> = ({ content, isUser }) => {
  if (isUser) {
    return <div style={{ whiteSpace: 'pre-line' }}>{content}</div>;
  }

  return (
    <div className="markdown-body" style={{ color: '#F1F5F9', fontSize: '0.875rem', lineHeight: '1.65' }}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          h1: ({ children }) => (
            <h1 style={{ fontSize: '1.15rem', fontWeight: 700, margin: '14px 0 8px', color: '#F8FAFC', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '4px' }}>
              {children}
            </h1>
          ),
          h2: ({ children }) => (
            <h2 style={{ fontSize: '1.05rem', fontWeight: 600, margin: '12px 0 6px', color: '#38BDF8', display: 'flex', alignItems: 'center', gap: '6px' }}>
              {children}
            </h2>
          ),
          h3: ({ children }) => (
            <h3 style={{ fontSize: '0.95rem', fontWeight: 600, margin: '10px 0 4px', color: '#C084FC' }}>
              {children}
            </h3>
          ),
          p: ({ children }) => (
            <p style={{ margin: '0 0 8px 0', lineHeight: 1.6 }}>
              {children}
            </p>
          ),
          strong: ({ children }) => (
            <strong style={{ color: '#F8FAFC', fontWeight: 700, background: 'rgba(56, 189, 248, 0.12)', padding: '1px 5px', borderRadius: '4px', border: '1px solid rgba(56, 189, 248, 0.25)' }}>
              {children}
            </strong>
          ),
          em: ({ children }) => (
            <em style={{ color: '#94A3B8', fontStyle: 'italic' }}>
              {children}
            </em>
          ),
          ul: ({ children }) => (
            <ul style={{ margin: '4px 0 10px 0', paddingLeft: '18px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
              {children}
            </ul>
          ),
          ol: ({ children }) => (
            <ol style={{ margin: '4px 0 10px 0', paddingLeft: '20px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
              {children}
            </ol>
          ),
          li: ({ children }) => (
            <li style={{ margin: '2px 0', lineHeight: 1.55 }}>
              {children}
            </li>
          ),
          table: ({ children }) => (
            <div style={{ overflowX: 'auto', margin: '10px 0' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8rem', background: 'rgba(15, 23, 42, 0.6)', borderRadius: '8px', overflow: 'hidden', border: '1px solid rgba(255,255,255,0.08)' }}>
                {children}
              </table>
            </div>
          ),
          thead: ({ children }) => (
            <thead style={{ background: 'rgba(30, 41, 59, 0.8)', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
              {children}
            </thead>
          ),
          th: ({ children }) => (
            <th style={{ padding: '8px 12px', textAlign: 'left', fontWeight: 600, color: '#38BDF8' }}>
              {children}
            </th>
          ),
          td: ({ children }) => (
            <td style={{ padding: '8px 12px', borderBottom: '1px solid rgba(255,255,255,0.05)', color: '#E2E8F0' }}>
              {children}
            </td>
          ),
          blockquote: ({ children }) => (
            <blockquote style={{ margin: '8px 0', padding: '8px 14px', borderLeft: '3px solid #38BDF8', background: 'rgba(56, 189, 248, 0.08)', borderRadius: '0 8px 8px 0', color: '#E2E8F0' }}>
              {children}
            </blockquote>
          ),
          code: ({ children }) => (
            <code style={{ fontFamily: 'monospace', background: 'rgba(0,0,0,0.4)', padding: '2px 6px', borderRadius: '4px', fontSize: '0.8rem', color: '#F472B6', border: '1px solid rgba(255,255,255,0.08)' }}>
              {children}
            </code>
          )
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
};
