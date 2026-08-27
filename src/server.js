#!/usr/bin/env node
'use strict';

const http = require('http');
const fs = require('fs');
const path = require('path');
const { TellerOrchestrator, OrchestratorError } = require('./orchestrator');

const HOST = '127.0.0.1';
const PORT = Number(process.env.PORT || 8799);
const PUBLIC_ROOT = path.join(__dirname, '..', 'public');
const orchestrator = new TellerOrchestrator();

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.svg': 'image/svg+xml',
};

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, `http://${HOST}:${PORT}`);
    if (url.pathname.startsWith('/api/')) return await handleApi(req, res, url);
    return serveStatic(res, url.pathname);
  } catch (error) {
    const status = error instanceof OrchestratorError ? error.status : 500;
    sendJson(res, status, { error: { code: error.code || 'INTERNAL_ERROR', message: error.message } });
  }
});

async function handleApi(req, res, url) {
  if (req.method === 'GET' && url.pathname === '/api/health') {
    return sendJson(res, 200, { status: 'ok', mode: 'MOCK_ONLY', now: new Date().toISOString() });
  }
  if (req.method === 'GET' && url.pathname === '/api/bootstrap') {
    return sendJson(res, 200, orchestrator.bootstrap());
  }
  if (req.method === 'POST' && url.pathname === '/api/sessions') {
    return sendJson(res, 201, orchestrator.createSession(await readJson(req)));
  }

  const match = url.pathname.match(/^\/api\/sessions\/([^/]+)(?:\/(messages|approvals|execute|confirm-and-execute))?$/);
  if (!match) return sendJson(res, 404, { error: { code: 'NOT_FOUND', message: 'Endpoint không tồn tại.' } });
  const [, sessionId, action] = match;
  if (req.method === 'GET' && !action) return sendJson(res, 200, orchestrator.getSession(sessionId));
  if (req.method === 'POST' && action === 'messages') {
    const body = await readJson(req);
    return sendJson(res, 200, await orchestrator.processMessage(sessionId, body.text));
  }
  if (req.method === 'POST' && action === 'approvals') {
    const body = await readJson(req);
    return sendJson(res, 200, orchestrator.approve(sessionId, body.actor));
  }
  if (req.method === 'POST' && action === 'execute') {
    return sendJson(res, 200, await orchestrator.execute(sessionId));
  }
  if (req.method === 'POST' && action === 'confirm-and-execute') {
    return sendJson(res, 200, await orchestrator.confirmAndExecuteCash(sessionId));
  }
  return sendJson(res, 405, { error: { code: 'METHOD_NOT_ALLOWED', message: 'Method không được hỗ trợ.' } });
}

function serveStatic(res, pathname) {
  const requested = pathname === '/' ? '/index.html' : pathname;
  const normalized = path.normalize(requested).replace(/^(\.\.(\/|\\|$))+/, '');
  const filePath = path.join(PUBLIC_ROOT, normalized);
  if (!filePath.startsWith(PUBLIC_ROOT)) return sendJson(res, 403, { error: { code: 'FORBIDDEN', message: 'Đường dẫn không hợp lệ.' } });
  if (!fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) return sendJson(res, 404, { error: { code: 'NOT_FOUND', message: 'Tệp không tồn tại.' } });
  res.writeHead(200, { 'Content-Type': MIME[path.extname(filePath)] || 'application/octet-stream', 'Cache-Control': 'no-store' });
  fs.createReadStream(filePath).pipe(res);
}

async function readJson(req) {
  let raw = '';
  for await (const chunk of req) {
    raw += chunk;
    if (raw.length > 256_000) throw new OrchestratorError('Payload vượt giới hạn.', 'PAYLOAD_TOO_LARGE', 413);
  }
  try { return raw ? JSON.parse(raw) : {}; }
  catch { throw new OrchestratorError('JSON không hợp lệ.', 'INVALID_JSON'); }
}

function sendJson(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' });
  res.end(JSON.stringify(body));
}

server.listen(PORT, HOST, () => {
  console.log(`B.Smart Teller Agent POC đang chạy tại http://${HOST}:${PORT}`);
  console.log('Chế độ MOCK_ONLY: không kết nối hệ thống ngân hàng thật.');
});

module.exports = { server, orchestrator };
