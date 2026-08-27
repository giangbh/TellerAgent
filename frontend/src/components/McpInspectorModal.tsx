import React, { useState } from 'react';
import { X, Layers, Terminal, Play } from 'lucide-react';
import { CapabilityItem } from '../types/teller';
import { callMcpJsonRpc } from '../api/tellerApi';

interface McpInspectorModalProps {
  isOpen: boolean;
  onClose: () => void;
  capabilities: CapabilityItem[];
}

export const McpInspectorModal: React.FC<McpInspectorModalProps> = ({ isOpen, onClose, capabilities }) => {
  const [selectedTool, setSelectedTool] = useState<CapabilityItem | null>(capabilities[0] || null);
  const [testMethod, setTestMethod] = useState('tools/list');
  const [testParamsJson, setTestParamsJson] = useState('{}');
  const [rpcResponse, setRpcResponse] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleRunRpc = async () => {
    setLoading(true);
    setRpcResponse(null);
    try {
      let parsed = {};
      try {
        parsed = JSON.parse(testParamsJson);
      } catch (err) {
        throw new Error('Params JSON không đúng cú pháp.');
      }
      const res = await callMcpJsonRpc(testMethod, parsed);
      setRpcResponse(JSON.stringify(res, null, 2));
    } catch (err: unknown) {
      setRpcResponse(JSON.stringify({ error: err instanceof Error ? err.message : 'Unknown error' }, null, 2));
    } finally {
      setLoading(false);
    }
  };

  const selectToolForTest = (tool: CapabilityItem) => {
    setSelectedTool(tool);
    setTestMethod('tools/call');
    const sampleParams = {
      name: tool.name,
      arguments: tool.name.includes('fee') ? { amount: 50000000 }
        : tool.name.includes('profile') ? { customerRef: 'CIF-0001842' }
        : tool.name.includes('bank') ? { bankCode: 'VCB' }
        : tool.name.includes('account') ? { accountNumber: '3456789' }
        : {},
      caller: tool.allowedCallers[0] || 'business_orchestrator',
      workflow: tool.workflows[0] || 'domestic_transfer',
    };
    setTestParamsJson(JSON.stringify(sampleParams, null, 2));
  };

  return (
    <div style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0, 0, 0, 0.75)',
      backdropFilter: 'blur(10px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 9999,
      padding: '20px',
    }}>
      <div className="glass-panel glow-box animate-fade-in" style={{
        width: '100%',
        maxWidth: '1080px',
        maxHeight: '90vh',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        background: '#0d1322',
        border: '1px solid rgba(139, 92, 246, 0.3)',
      }}>
        
        {/* Modal Header */}
        <div style={{
          padding: '18px 24px',
          borderBottom: '1px solid var(--border-color)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{
              width: '36px',
              height: '36px',
              borderRadius: '10px',
              background: 'linear-gradient(135deg, #8b5cf6 0%, #3b82f6 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <Layers size={20} color="#fff" />
            </div>
            <div>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 800 }}>MCP Banking Protocol & Tools Explorer</h2>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Model Context Protocol (JSON-RPC 2.0) · Java Spring Boot Capability Server
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid var(--border-color)',
              color: 'var(--text-muted)',
              borderRadius: '8px',
              padding: '6px',
              cursor: 'pointer',
            }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Modal Body */}
        <div style={{ display: 'grid', gridTemplateColumns: '320px 1fr', flex: 1, overflow: 'hidden' }}>
          
          {/* Left Column: Tool List */}
          <div style={{
            borderRight: '1px solid var(--border-color)',
            overflowY: 'auto',
            padding: '16px',
            display: 'flex',
            flexDirection: 'column',
            gap: '8px',
            background: 'rgba(0, 0, 0, 0.2)',
          }}>
            <span style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginBottom: '4px' }}>
              Danh mục {capabilities.length} Tools:
            </span>
            {capabilities.map((t) => {
              const isSel = selectedTool?.id === t.id;
              return (
                <div
                  key={t.id}
                  onClick={() => selectToolForTest(t)}
                  style={{
                    padding: '10px 12px',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    background: isSel ? 'rgba(139, 92, 246, 0.15)' : 'rgba(255, 255, 255, 0.02)',
                    border: `1px solid ${isSel ? 'rgba(139, 92, 246, 0.4)' : 'transparent'}`,
                    transition: 'all 0.15s ease',
                  }}
                >
                  <div style={{ fontSize: '0.8rem', fontWeight: 700, color: isSel ? '#c084fc' : '#fff', marginBottom: '3px' }}>
                    {t.name}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <span className="badge badge-neutral" style={{ fontSize: '0.625rem', padding: '2px 6px' }}>
                      {t.risk}
                    </span>
                    {t.sideEffect && (
                      <span className="badge badge-danger" style={{ fontSize: '0.625rem', padding: '2px 6px' }}>
                        WRITE
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Right Column: Schema & JSON-RPC Test Runner */}
          <div style={{ overflowY: 'auto', padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            
            {/* Selected Tool Details */}
            {selectedTool && (
              <div style={{
                padding: '16px',
                borderRadius: '10px',
                background: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid var(--border-color)',
              }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                  <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: '#34d399' }}>
                    {selectedTool.id} ({selectedTool.name})
                  </h3>
                  <span className="badge badge-purple">{selectedTool.risk}</span>
                </div>
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '10px' }}>
                  {selectedTool.description}
                </p>
                <div style={{ display: 'flex', gap: '12px', fontSize: '0.75rem', color: 'var(--text-dim)' }}>
                  <div><strong>Allowed Callers: </strong>{selectedTool.allowedCallers.join(', ')}</div>
                  <div><strong>Workflows: </strong>{selectedTool.workflows.join(', ')}</div>
                </div>
              </div>
            )}

            {/* Interactive JSON-RPC 2.0 Tester */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Terminal size={15} color="#8b5cf6" />
                  <span style={{ fontSize: '0.8rem', fontWeight: 700 }}>Live JSON-RPC 2.0 Runner</span>
                </div>
                <button
                  className="btn btn-purple"
                  onClick={handleRunRpc}
                  disabled={loading}
                  style={{ padding: '6px 14px', fontSize: '0.775rem' }}
                >
                  <Play size={13} />
                  <span>Execute MCP Method</span>
                </button>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '160px 1fr', gap: '10px' }}>
                <select
                  value={testMethod}
                  onChange={(e) => setTestMethod(e.target.value)}
                  style={{
                    padding: '8px 12px',
                    borderRadius: '8px',
                    background: 'rgba(0, 0, 0, 0.4)',
                    border: '1px solid var(--border-color)',
                    color: '#fff',
                    fontSize: '0.8rem',
                    outline: 'none',
                  }}
                >
                  <option value="tools/list">tools/list</option>
                  <option value="tools/call">tools/call</option>
                  <option value="initialize">initialize</option>
                  <option value="ping">ping</option>
                  <option value="resources/list">resources/list</option>
                  <option value="resources/read">resources/read</option>
                </select>

                <input
                  type="text"
                  value={testParamsJson}
                  onChange={(e) => setTestParamsJson(e.target.value)}
                  placeholder="Params JSON..."
                  style={{
                    padding: '8px 12px',
                    borderRadius: '8px',
                    background: 'rgba(0, 0, 0, 0.4)',
                    border: '1px solid var(--border-color)',
                    color: '#a78bfa',
                    fontFamily: 'monospace',
                    fontSize: '0.8rem',
                    outline: 'none',
                  }}
                />
              </div>

              {/* RPC Output Window */}
              {rpcResponse && (
                <div style={{ marginTop: '6px' }}>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-dim)', marginBottom: '4px' }}>
                    JSON-RPC 2.0 Response:
                  </div>
                  <pre style={{
                    padding: '12px',
                    borderRadius: '8px',
                    background: '#070b14',
                    border: '1px solid rgba(255, 255, 255, 0.1)',
                    fontSize: '0.75rem',
                    color: '#34d399',
                    maxHeight: '200px',
                    overflowY: 'auto',
                  }}>
                    {rpcResponse}
                  </pre>
                </div>
              )}
            </div>

          </div>

        </div>

      </div>
    </div>
  );
};
