import React, { useState, useEffect } from 'react';
import { ShieldCheck, Activity, Award, CheckCircle2, AlertTriangle, Clock, RefreshCw, X, Layers } from 'lucide-react';
import { EvaluationJudgeResult, TraceContext, ChatMessage, Session } from '../types/teller';

interface ObservabilityModalProps {
  isOpen: boolean;
  onClose: () => void;
  session: Session | null;
  selectedMessage: ChatMessage | null;
  userPrompt: string;
}

export const ObservabilityModal: React.FC<ObservabilityModalProps> = ({
  isOpen,
  onClose,
  session,
  selectedMessage,
  userPrompt
}) => {
  const [activeTab, setActiveTab] = useState<'judge' | 'tracing'>('judge');
  const [loadingJudge, setLoadingJudge] = useState(false);
  const [loadingTrace, setLoadingTrace] = useState(false);
  const [judgeResult, setJudgeResult] = useState<EvaluationJudgeResult | null>(null);
  const [traceContext, setTraceContext] = useState<TraceContext | null>(null);

  const traceId = selectedMessage?.traceId;

  useEffect(() => {
    if (isOpen && selectedMessage) {
      fetchJudgeEvaluation();
      if (traceId) {
        fetchTraceDetails(traceId);
      }
    }
  }, [isOpen, selectedMessage]);

  const fetchJudgeEvaluation = async () => {
    if (!selectedMessage) return;
    setLoadingJudge(true);
    try {
      const res = await fetch('/api/evaluations/judge', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sessionId: session?.sessionId || '',
          traceId: traceId || '',
          userPrompt: userPrompt || 'Yêu cầu nghiệp vụ ngân hàng',
          assistantResponse: selectedMessage.text,
          latencyMs: selectedMessage.thinkingTimeMs || 1800
        })
      });
      if (res.ok) {
        const data = await res.json();
        setJudgeResult(data);
      }
    } catch (e) {
      console.error('Failed to run LLM Judge evaluation', e);
    } finally {
      setLoadingJudge(false);
    }
  };

  const fetchTraceDetails = async (tId: string) => {
    setLoadingTrace(true);
    try {
      const res = await fetch(`/api/observability/traces/${tId}`);
      if (res.ok) {
        const data = await res.json();
        setTraceContext(data);
      }
    } catch (e) {
      console.error('Failed to load trace details', e);
    } finally {
      setLoadingTrace(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(10, 14, 23, 0.85)',
      backdropFilter: 'blur(8px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 9999,
      padding: '24px'
    }}>
      <div style={{
        backgroundColor: '#0F172A',
        border: '1px solid rgba(56, 189, 248, 0.3)',
        borderRadius: '16px',
        width: '100%',
        maxWidth: '860px',
        maxHeight: '90vh',
        display: 'flex',
        flexDirection: 'column',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.7), 0 0 30px rgba(56, 189, 248, 0.15)',
        overflow: 'hidden',
        color: '#F8FAFC'
      }}>
        {/* Header */}
        <div style={{
          padding: '20px 24px',
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          background: 'linear-gradient(90deg, rgba(30, 41, 59, 0.8), rgba(15, 23, 42, 0.95))'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{
              width: '40px',
              height: '40px',
              borderRadius: '10px',
              background: 'linear-gradient(135deg, #3B82F6, #8B5CF6)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 15px rgba(59, 130, 246, 0.4)'
            }}>
              <Award size={22} color="#FFFFFF" />
            </div>
            <div>
              <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 600, color: '#F8FAFC' }}>
                AI Quality Auditor & OpenTelemetry APM
              </h2>
              <p style={{ margin: '2px 0 0', fontSize: '12px', color: '#94A3B8' }}>
                Giám định độc lập chất lượng phản hồi & Chi tiết cây dấu vết phân tán
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            style={{
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: '8px',
              padding: '8px',
              cursor: 'pointer',
              color: '#94A3B8',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <X size={18} />
          </button>
        </div>

        {/* Tab Switcher */}
        <div style={{
          display: 'flex',
          gap: '8px',
          padding: '12px 24px',
          background: 'rgba(15, 23, 42, 0.6)',
          borderBottom: '1px solid rgba(255, 255, 255, 0.06)'
        }}>
          <button
            onClick={() => setActiveTab('judge')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 16px',
              borderRadius: '8px',
              fontSize: '13px',
              fontWeight: 500,
              cursor: 'pointer',
              border: activeTab === 'judge' ? '1px solid #38BDF8' : '1px solid transparent',
              background: activeTab === 'judge' ? 'rgba(56, 189, 248, 0.15)' : 'transparent',
              color: activeTab === 'judge' ? '#38BDF8' : '#94A3B8'
            }}
          >
            <ShieldCheck size={16} />
            ⚖️ LLM-as-a-Judge Evaluation
          </button>

          <button
            onClick={() => setActiveTab('tracing')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '8px 16px',
              borderRadius: '8px',
              fontSize: '13px',
              fontWeight: 500,
              cursor: 'pointer',
              border: activeTab === 'tracing' ? '1px solid #A855F7' : '1px solid transparent',
              background: activeTab === 'tracing' ? 'rgba(168, 85, 247, 0.15)' : 'transparent',
              color: activeTab === 'tracing' ? '#C084FC' : '#94A3B8'
            }}
          >
            <Activity size={16} />
            📊 OpenTelemetry Distributed Tracing
          </button>
        </div>

        {/* Content Body */}
        <div style={{ padding: '24px', overflowY: 'auto', flex: 1 }}>
          {activeTab === 'judge' && (
            <div>
              {loadingJudge ? (
                <div style={{ padding: '40px 0', textAlign: 'center', color: '#94A3B8' }}>
                  <RefreshCw size={28} className="animate-spin" style={{ margin: '0 auto 16px', color: '#38BDF8' }} />
                  <p style={{ margin: 0, fontSize: '14px' }}>Chuyên gia AI LLM-as-a-Judge đang phân tích & đối chiếu dữ liệu gốc...</p>
                </div>
              ) : judgeResult ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                  {/* Verdict & Score Banner */}
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '16px 20px',
                    borderRadius: '12px',
                    background: judgeResult.verdict === 'PASS' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(245, 158, 11, 0.1)',
                    border: `1px solid ${judgeResult.verdict === 'PASS' ? 'rgba(16, 185, 129, 0.3)' : 'rgba(245, 158, 11, 0.3)'}`
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                      {judgeResult.verdict === 'PASS' ? (
                        <CheckCircle2 size={28} color="#10B981" />
                      ) : (
                        <AlertTriangle size={28} color="#F59E0B" />
                      )}
                      <div>
                        <div style={{ fontSize: '16px', fontWeight: 600, color: judgeResult.verdict === 'PASS' ? '#10B981' : '#F59E0B' }}>
                          Kết quả Giám định: {judgeResult.verdict}
                        </div>
                        <div style={{ fontSize: '12px', color: '#94A3B8', marginTop: '2px' }}>
                          Đánh giá bởi: {judgeResult.judgeModel} · Thời gian: {new Date(judgeResult.evaluatedAt).toLocaleTimeString('vi-VN')}
                        </div>
                      </div>
                    </div>

                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontSize: '28px', fontWeight: 700, color: '#F8FAFC', letterSpacing: '-0.5px' }}>
                        {judgeResult.overallScore} <span style={{ fontSize: '16px', color: '#94A3B8', fontWeight: 400 }}>/ 10</span>
                      </div>
                      <div style={{ fontSize: '11px', color: '#38BDF8', fontWeight: 500 }}>Điểm chất lượng tổng hợp</div>
                    </div>
                  </div>

                  {/* 4 Pillars Metric Cards */}
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '16px' }}>
                    {/* Metric 1 */}
                    <div style={{
                      padding: '16px',
                      borderRadius: '12px',
                      background: 'rgba(30, 41, 59, 0.5)',
                      border: '1px solid rgba(255, 255, 255, 0.06)'
                    }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                        <span style={{ fontSize: '13px', color: '#94A3B8', fontWeight: 500 }}>🎯 Không ảo giác (Ground Truth)</span>
                        <span style={{ fontSize: '14px', fontWeight: 700, color: '#38BDF8' }}>{judgeResult.hallucinationScore}/10</span>
                      </div>
                      <div style={{ height: '6px', background: 'rgba(255,255,255,0.08)', borderRadius: '3px', overflow: 'hidden' }}>
                        <div style={{ width: `${judgeResult.hallucinationScore * 10}%`, height: '100%', background: 'linear-gradient(90deg, #38BDF8, #3B82F6)', borderRadius: '3px' }} />
                      </div>
                      <p style={{ margin: '8px 0 0', fontSize: '11px', color: '#64748B' }}>Độ khớp 100% với số liệu chuẩn Inflow/Outflow</p>
                    </div>

                    {/* Metric 2 */}
                    <div style={{
                      padding: '16px',
                      borderRadius: '12px',
                      background: 'rgba(30, 41, 59, 0.5)',
                      border: '1px solid rgba(255, 255, 255, 0.06)'
                    }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                        <span style={{ fontSize: '13px', color: '#94A3B8', fontWeight: 500 }}>🤝 Chuẩn mực giao tiếp (Politeness)</span>
                        <span style={{ fontSize: '14px', fontWeight: 700, color: '#10B981' }}>{judgeResult.politenessScore}/10</span>
                      </div>
                      <div style={{ height: '6px', background: 'rgba(255,255,255,0.08)', borderRadius: '3px', overflow: 'hidden' }}>
                        <div style={{ width: `${judgeResult.politenessScore * 10}%`, height: '100%', background: 'linear-gradient(90deg, #10B981, #059669)', borderRadius: '3px' }} />
                      </div>
                      <p style={{ margin: '8px 0 0', fontSize: '11px', color: '#64748B' }}>Xưng hô chuyên nghiệp, định dạng tiền tệ VND</p>
                    </div>

                    {/* Metric 3 */}
                    <div style={{
                      padding: '16px',
                      borderRadius: '12px',
                      background: 'rgba(30, 41, 59, 0.5)',
                      border: '1px solid rgba(255, 255, 255, 0.06)'
                    }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                        <span style={{ fontSize: '13px', color: '#94A3B8', fontWeight: 500 }}>🛡️ Tuân thủ & Bảo mật (Compliance)</span>
                        <span style={{ fontSize: '14px', fontWeight: 700, color: '#A855F7' }}>{judgeResult.complianceScore}/10</span>
                      </div>
                      <div style={{ height: '6px', background: 'rgba(255,255,255,0.08)', borderRadius: '3px', overflow: 'hidden' }}>
                        <div style={{ width: `${judgeResult.complianceScore * 10}%`, height: '100%', background: 'linear-gradient(90deg, #A855F7, #7C3AED)', borderRadius: '3px' }} />
                      </div>
                      <p style={{ margin: '8px 0 0', fontSize: '11px', color: '#64748B' }}>Không lộ PII, tuân thủ hạn mức Maker-Checker</p>
                    </div>

                    {/* Metric 4 */}
                    <div style={{
                      padding: '16px',
                      borderRadius: '12px',
                      background: 'rgba(30, 41, 59, 0.5)',
                      border: '1px solid rgba(255, 255, 255, 0.06)'
                    }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                        <span style={{ fontSize: '13px', color: '#94A3B8', fontWeight: 500 }}>⚡ Độ trễ SLA ({judgeResult.latencyMs}ms)</span>
                        <span style={{ fontSize: '14px', fontWeight: 700, color: '#F59E0B' }}>{judgeResult.latencyScore}/10</span>
                      </div>
                      <div style={{ height: '6px', background: 'rgba(255,255,255,0.08)', borderRadius: '3px', overflow: 'hidden' }}>
                        <div style={{ width: `${judgeResult.latencyScore * 10}%`, height: '100%', background: 'linear-gradient(90deg, #F59E0B, #D97706)', borderRadius: '3px' }} />
                      </div>
                      <p style={{ margin: '8px 0 0', fontSize: '11px', color: '#64748B' }}>Thời gian phản hồi đạt chuẩn quầy giao dịch</p>
                    </div>
                  </div>

                  {/* Critique from Judge */}
                  <div style={{
                    padding: '16px 20px',
                    borderRadius: '12px',
                    background: 'rgba(15, 23, 42, 0.7)',
                    border: '1px solid rgba(56, 189, 248, 0.2)'
                  }}>
                    <h4 style={{ margin: '0 0 8px', fontSize: '13px', fontWeight: 600, color: '#38BDF8', display: 'flex', alignItems: 'center', gap: '6px' }}>
                      💬 Nhận Xét Chi Tiết Của Ban Giám Định AI
                    </h4>
                    <p style={{ margin: 0, fontSize: '13px', lineHeight: 1.6, color: '#E2E8F0' }}>
                      {judgeResult.critique}
                    </p>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <button
                      onClick={fetchJudgeEvaluation}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '6px',
                        padding: '8px 16px',
                        borderRadius: '8px',
                        background: 'linear-gradient(135deg, #2563EB, #1D4ED8)',
                        border: 'none',
                        color: '#FFFFFF',
                        fontSize: '13px',
                        fontWeight: 500,
                        cursor: 'pointer'
                      }}
                    >
                      <RefreshCw size={14} /> Chấm Điểm Lại (Re-Evaluate)
                    </button>
                  </div>
                </div>
              ) : null}
            </div>
          )}

          {activeTab === 'tracing' && (
            <div>
              {loadingTrace ? (
                <div style={{ padding: '40px 0', textAlign: 'center', color: '#94A3B8' }}>
                  <RefreshCw size={28} className="animate-spin" style={{ margin: '0 auto 16px', color: '#C084FC' }} />
                  <p style={{ margin: 0, fontSize: '14px' }}>Đang tải cây dấu vết OpenTelemetry APM Spans...</p>
                </div>
              ) : traceContext ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
                  {/* Trace Summary Bar */}
                  <div style={{
                    padding: '14px 18px',
                    borderRadius: '10px',
                    background: 'rgba(30, 41, 59, 0.6)',
                    border: '1px solid rgba(168, 85, 247, 0.3)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}>
                    <div>
                      <span style={{ fontSize: '11px', color: '#94A3B8' }}>Trace ID:</span>
                      <span style={{ fontSize: '13px', fontWeight: 600, color: '#C084FC', marginLeft: '8px', fontFamily: 'monospace' }}>
                        {traceContext.traceId}
                      </span>
                    </div>
                    <div style={{ display: 'flex', gap: '16px', fontSize: '12px', color: '#E2E8F0' }}>
                      <span>Tổng thời gian: <strong style={{ color: '#38BDF8' }}>{traceContext.totalDurationMs} ms</strong></span>
                      <span>Số lượng Spans: <strong style={{ color: '#10B981' }}>{traceContext.spans.length}</strong></span>
                    </div>
                  </div>

                  {/* Waterfall Spans Timeline */}
                  <div>
                    <h4 style={{ margin: '0 0 12px', fontSize: '13px', fontWeight: 600, color: '#94A3B8', display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Layers size={15} /> Phân Bổ Thời Gian Thực Thi (Waterfall Timeline)
                    </h4>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                      {traceContext.spans.map((span, idx) => {
                        const total = traceContext.totalDurationMs || 1;
                        const widthPct = Math.max(8, Math.min(100, Math.round((span.durationMs / total) * 100)));
                        return (
                          <div
                            key={span.spanId || idx}
                            style={{
                              padding: '10px 14px',
                              borderRadius: '8px',
                              background: 'rgba(15, 23, 42, 0.5)',
                              border: '1px solid rgba(255, 255, 255, 0.05)',
                              display: 'flex',
                              flexDirection: 'column',
                              gap: '6px'
                            }}
                          >
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px' }}>
                              <span style={{ fontWeight: 600, color: '#F1F5F9', fontFamily: 'monospace' }}>
                                ⚡ {span.name}
                              </span>
                              <span style={{ color: '#38BDF8', fontWeight: 500 }}>
                                {span.durationMs} ms ({widthPct}%)
                              </span>
                            </div>
                            <div style={{ height: '6px', background: 'rgba(255,255,255,0.06)', borderRadius: '3px', overflow: 'hidden' }}>
                              <div
                                style={{
                                  width: `${widthPct}%`,
                                  height: '100%',
                                  background: span.name.includes('mcp_tool')
                                    ? 'linear-gradient(90deg, #10B981, #059669)'
                                    : (span.name.includes('synthesis')
                                        ? 'linear-gradient(90deg, #8B5CF6, #6D28D9)'
                                        : 'linear-gradient(90deg, #38BDF8, #2563EB)'),
                                  borderRadius: '3px'
                                }}
                              />
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              ) : (
                <div style={{ padding: '40px 0', textAlign: 'center', color: '#94A3B8' }}>
                  <Clock size={32} style={{ margin: '0 auto 12px', color: '#64748B' }} />
                  <p style={{ margin: 0, fontSize: '13px' }}>Không tìm thấy traceId liên kết với tin nhắn này.</p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
