import { BootstrapData, Session } from '../types/teller';

const API_BASE = '/api';

/**
 * P0-3: mọi request phải mang danh tính đã xác thực.
 *
 * POC: header do BFF/reverse proxy đặt. Khi chuyển sang OIDC, thay hàm này
 * bằng việc gắn Bearer token — phần còn lại của frontend không đổi.
 */
export type ActorIdentity = {
  id: string;
  name: string;
  role: 'TELLER' | 'SUPERVISOR' | 'SECURITY_ADMIN';
  branchId?: string;
};

let currentActor: ActorIdentity | null = null;

export function setCurrentActor(actor: ActorIdentity) {
  currentActor = actor;
}

function safeHeaderValue(val: string): string {
  // Browser fetch headers only allow ISO-8859-1 (ASCII-safe) characters
  return /^[\x00-\x7F]*$/.test(val) ? val : encodeURIComponent(val);
}

function authHeaders(): Record<string, string> {
  if (!currentActor) {
    throw new Error('Chưa đăng nhập: không xác định được danh tính người dùng.');
  }
  return {
    'Content-Type': 'application/json',
    'X-Actor-Id': safeHeaderValue(currentActor.id),
    'X-Actor-Name': safeHeaderValue(currentActor.name),
    'X-Actor-Role': currentActor.role,
    ...(currentActor.branchId ? { 'X-Actor-Branch': safeHeaderValue(currentActor.branchId) } : {}),
  };
}

export async function fetchBootstrap(): Promise<BootstrapData> {
  const res = await fetch(`${API_BASE}/bootstrap`);
  if (!res.ok) throw new Error('Không thể tải thông tin khởi động hệ thống.');
  return res.json();
}

export async function createSession(params?: Record<string, unknown>): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(params || {}),
  });
  if (!res.ok) throw new Error('Không thể khởi tạo phiên làm việc.');
  return res.json();
}

export async function getSession(sessionId: string): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions/${sessionId}`, { headers: authHeaders() });
  if (!res.ok) throw new Error('Không thể tải thông tin phiên làm việc.');
  return res.json();
}

export async function sendMessage(sessionId: string, text: string): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions/${sessionId}/messages`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ text }),
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data?.error?.message || 'Lỗi khi gửi tin nhắn cho Agent.');
  }
  return data;
}

export async function approveSession(sessionId: string, role: 'teller' | 'supervisor'): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions/${sessionId}/approvals`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ role }),
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data?.error?.message || `Lỗi khi ${role} phê duyệt.`);
  }
  return data;
}

/**
 * P0-4: xác nhận của khách hàng là một hành động riêng, bắt buộc kèm bằng chứng.
 * Không còn cách nào để hệ thống tự đánh dấu "khách hàng đã đồng ý".
 */
export async function recordCustomerConsent(
  sessionId: string,
  evidenceType: 'OTP' | 'SIGNATURE' | 'DOCUMENT' | 'BIOMETRIC',
  evidenceRef: string,
): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions/${sessionId}/consent`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ evidenceType, evidenceRef }),
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(data?.error?.message || 'Lỗi khi ghi nhận xác nhận của khách hàng.');
  }
  return data;
}

export async function executeSession(sessionId: string): Promise<Session> {
  const res = await fetch(`${API_BASE}/sessions/${sessionId}/execute`, {
    method: 'POST',
    headers: authHeaders(),
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
    headers: authHeaders(),
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

export interface SearchResultItem {
  id: string;
  title: string;
  subtitle: string;
  badge: string;
  badgeColor: string;
  icon: string;
  actionType: string;
  prompt: string;
  payload?: Record<string, any>;
}

export interface SearchResultCategory {
  type: string;
  title: string;
  items: SearchResultItem[];
}

export interface GlobalSearchResponse {
  query: string;
  totalResults: number;
  categories: SearchResultCategory[];
}

export async function searchGlobal(query: string): Promise<GlobalSearchResponse> {
  const res = await fetch(`${API_BASE}/search/global?q=${encodeURIComponent(query)}`);
  if (!res.ok) throw new Error('Lỗi khi tìm kiếm dữ liệu.');
  return res.json();
}
