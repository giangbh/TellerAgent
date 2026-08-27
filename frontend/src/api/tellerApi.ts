import { BootstrapData, Session } from '../types/teller';

const API_BASE = '/api';

export async function fetchBootstrap(): Promise<BootstrapData> {
  const res = await fetch(`${API_BASE}/bootstrap`);
  if (!res.ok) throw new Error('Không thể tải thông tin khởi động hệ thống.');
  return res.json();
}

export async function createSession(params?: Record<string, unknown>): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params || {}),
  });
  if (!res.ok) throw new Error('Không thể khởi tạo phiên làm việc.');
  return res.json();
}

export async function getSession(sessionId: string): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions/${sessionId}`);
  if (!res.ok) throw new Error('Không thể tải thông tin phiên làm việc.');
  return res.json();
}

export async function sendMessage(sessionId: string, text: string): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions/${sessionId}/messages`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data?.error?.message || 'Lỗi khi gửi tin nhắn cho Agent.');
  }
  return data;
}

export async function approveSession(sessionId: string, actor: 'customer' | 'teller' | 'supervisor'): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions/${sessionId}/approvals`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ actor }),
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data?.error?.message || `Lỗi khi ${actor} phê duyệt.`);
  }
  return data;
}

export async function executeSession(sessionId: string): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions/${sessionId}/execute`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data?.error?.message || 'Lỗi khi thực thi posting vào Core Banking.');
  }
  return data;
}

export async function confirmAndExecuteCash(sessionId: string): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions/${sessionId}/confirm-and-execute`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data?.error?.message || 'Lỗi khi GDV xác nhận và thực thi nộp/rút tiền.');
  }
  return data;
}

export async function callMcpJsonRpc(method: string, params: Record<string, unknown> = {}): Promise<unknown> {
  const req = {
    jsonrpc: '2.0',
    id: Date.now(),
    method,
    params,
  };
  const res = await fetch(`${API_BASE}/mcp`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error('Lỗi khi gọi MCP JSON-RPC endpoint.');
  return res.json();
}
