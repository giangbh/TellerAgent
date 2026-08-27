'use strict';

const ui = {
  sessionCode: document.querySelector('#session-code'), branchId: document.querySelector('#branch-id'), counterId: document.querySelector('#counter-id'), tellerId: document.querySelector('#teller-id'),
  customerName: document.querySelector('#customer-name'), customerTags: document.querySelector('#customer-tags'), workflowStatus: document.querySelector('#workflow-status'),
  messages: document.querySelector('#messages'), input: document.querySelector('#message-input'), form: document.querySelector('#message-form'), send: document.querySelector('#send-button'),
  scenarioList: document.querySelector('#scenario-list'), draftTitle: document.querySelector('#draft-title'), draftEmpty: document.querySelector('#draft-empty'), draftContent: document.querySelector('#draft-content'), draftValidity: document.querySelector('#draft-validity'),
  screenBanner: document.querySelector('#screen-banner'), screenTitle: document.querySelector('#screen-title'), screenCode: document.querySelector('#screen-code'), transferApprovalRow: document.querySelector('#transfer-approval-row'), cashSubmitRow: document.querySelector('#cash-submit-row'), cashSubmit: document.querySelector('#cash-submit-button'),
  postingState: document.querySelector('#posting-state'), controlGates: document.querySelector('#control-gates'), planObjective: document.querySelector('#plan-objective'), delegations: document.querySelector('#delegations'),
  budgetCounter: document.querySelector('#budget-counter'), toolCalls: document.querySelector('#tool-calls'), toolCounter: document.querySelector('#tool-counter'), execute: document.querySelector('#execute-button'),
  newSession: document.querySelector('#new-session'), toast: document.querySelector('#toast'), clock: document.querySelector('#clock'),
};

let bootstrap;
let session;

init().catch(showError);

async function init() {
  bootstrap = await api('/api/bootstrap');
  renderScenarios();
  await createSession();
  setInterval(() => { ui.clock.textContent = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }); }, 1000);
}

async function createSession() {
  setBusy(true);
  try {
    session = await api('/api/sessions', { method: 'POST', body: {} });
    render();
    ui.input.value = '';
    showToast('Đã mở phiên B.Smart mới.');
  } finally { setBusy(false); }
}

ui.form.addEventListener('submit', async event => {
  event.preventDefault();
  const text = ui.input.value.trim();
  if (!text) return;
  setBusy(true);
  try {
    session = await api(`/api/sessions/${session.sessionId}/messages`, { method: 'POST', body: { text } });
    ui.input.value = '';
    render();
  } catch (error) { showError(error); }
  finally { setBusy(false); }
});

ui.newSession.addEventListener('click', createSession);
ui.execute.addEventListener('click', async () => {
  setBusy(true);
  try {
    session = await api(`/api/sessions/${session.sessionId}/execute`, { method: 'POST', body: {} });
    render();
    showToast(`Core mock: ${session.execution.coreReference}`);
  } catch (error) { showError(error); }
  finally { setBusy(false); }
});

ui.cashSubmit.addEventListener('click', async () => {
  setBusy(true);
  try {
    session = await api(`/api/sessions/${session.sessionId}/confirm-and-execute`, { method: 'POST', body: {} });
    render();
    showToast(`Core cash mock: ${session.execution.coreReference}`);
  } catch (error) { showError(error); }
  finally { setBusy(false); }
});

document.querySelectorAll('.approval-button').forEach(button => {
  button.addEventListener('click', async () => {
    setBusy(true);
    try {
      session = await api(`/api/sessions/${session.sessionId}/approvals`, { method: 'POST', body: { actor: button.dataset.actor } });
      render();
    } catch (error) { showError(error); }
    finally { setBusy(false); }
  });
});

function renderScenarios() {
  ui.scenarioList.innerHTML = bootstrap.scenarios.map((scenario, index) => `
    <button class="scenario-button" data-prompt="${escapeHtml(scenario.prompt)}">
      <span class="scenario-number">0${index + 1}</span><strong>${escapeHtml(scenario.name)}</strong>
    </button>`).join('');
  ui.scenarioList.querySelectorAll('button').forEach(button => {
    button.addEventListener('click', () => { ui.input.value = button.dataset.prompt; ui.input.focus(); });
  });
}

function render() {
  ui.sessionCode.textContent = session.sessionId;
  ui.branchId.textContent = session.branchId;
  ui.counterId.textContent = session.counterId;
  ui.tellerId.textContent = session.tellerId;
  ui.workflowStatus.textContent = session.status;

  const customer = session.customerContext || {};
  ui.customerName.textContent = customer.displayName || 'Chưa tải hồ sơ';
  ui.customerTags.innerHTML = [customer.segment, customer.kycStatus, customer.riskRating].filter(Boolean).map(tag => `<span class="tag">${escapeHtml(tag)}</span>`).join('');
  renderMessages();
  renderDraft();
  renderControl();
  renderPlan();
  renderTools();
  renderButtons();
}

function renderMessages() {
  const intro = [{ role: 'assistant', text: 'Hãy mô tả yêu cầu của khách hàng hoặc chọn một kịch bản demo.' }];
  ui.messages.innerHTML = [...intro, ...session.messages].map(message => `
    <div class="message ${message.role}">
      <div class="message-icon">${message.role === 'user' ? 'TV' : 'AI'}</div>
      <div><strong>${message.role === 'user' ? 'Teller' : 'B.Smart'}</strong><p>${escapeHtml(message.text)}</p></div>
    </div>`).join('');
  ui.messages.scrollTop = ui.messages.scrollHeight;
}

function renderDraft() {
  const draft = session.transactionDraft || {};
  const hasDraft = Object.keys(draft).length > 0;
  const cashWorkflow = session.workflow?.startsWith('cash_');
  ui.draftEmpty.classList.toggle('hidden', hasDraft);
  ui.draftContent.classList.toggle('hidden', !hasDraft);
  ui.screenBanner.classList.toggle('hidden', !cashWorkflow || !hasDraft);
  ui.draftTitle.textContent = cashWorkflow && draft.screenTitle ? draft.screenTitle : 'Bản nháp giao dịch';
  if (cashWorkflow && hasDraft) {
    ui.screenTitle.textContent = draft.screenTitle;
    ui.screenCode.textContent = draft.screenCode;
  }
  if (!hasDraft) {
    ui.draftValidity.textContent = 'Chưa có dữ liệu';
    ui.draftValidity.className = 'status-chip neutral';
    return;
  }
  const fields = cashWorkflow ? [
    ['Loại giao dịch', draft.transactionType === 'CASH_DEPOSIT' ? 'Nộp tiền' : 'Rút tiền'],
    ['Số tài khoản', draft.accountNumber],
    ['Chủ tài khoản', draft.accountHolder],
    ['Số tiền', money(draft.amount)],
    ['Số dư khả dụng', money(draft.availableBalance)],
    ['Nội dung', draft.description],
  ] : [
    ['Tài khoản nguồn', draft.sourceAccountMasked],
    ['Người thụ hưởng', draft.beneficiaryName],
    ['Tài khoản nhận', maskAccount(draft.beneficiaryAccount)],
    ['Ngân hàng', draft.bankName || draft.bankCode],
    ['Số tiền', money(draft.amount)],
    ['Phí + VAT', money(draft.fee?.totalFee)],
  ];
  ui.draftContent.innerHTML = fields.map(([label, value]) => cashWorkflow
    ? `<label class="draft-field"><span>${label}</span><input class="autofilled" readonly value="${escapeHtml(value || 'Chưa có')}"></label>`
    : `<div class="draft-field"><span>${label}</span><strong>${escapeHtml(value || 'Chưa có')}</strong></div>`).join('');
  const valid = draft.validation?.valid;
  ui.draftValidity.textContent = valid ? 'VALIDATED' : `THIẾU ${draft.validation?.missingFields?.length || 0} TRƯỜNG`;
  ui.draftValidity.className = `status-chip ${valid ? 'ok' : 'warn'}`;
}

function renderControl() {
  const gates = [
    ['Bản nháp hợp lệ', session.transactionDraft.validation?.valid === true],
    ['Risk decision = PASS', session.risk.decision === 'PASS'],
    ['Khách hàng xác nhận', session.approvals.customer],
    ['Teller xác nhận', session.approvals.teller],
  ];
  if (session.transactionDraft.limit?.supervisorRequired) gates.push(['Kiểm soát viên xác nhận', session.approvals.supervisor]);
  ui.controlGates.innerHTML = gates.map(([label, pass]) => `<div class="gate ${pass ? 'pass' : ''}"><span>${label}</span><span class="indicator">${pass ? '✓' : '×'}</span></div>`).join('');
  ui.postingState.textContent = session.control.postingAllowed ? 'UNLOCKED' : 'LOCKED';
  ui.postingState.classList.toggle('open', session.control.postingAllowed);
}

function renderPlan() {
  const runtime = session.agentRuntime;
  ui.planObjective.textContent = runtime.plan?.objective || 'Chưa có kế hoạch.';
  ui.budgetCounter.textContent = `${runtime.budget.delegationsUsed} / ${runtime.budget.maxDelegations}`;
  ui.delegations.innerHTML = runtime.delegations.length ? runtime.delegations.map(item => `
    <div class="delegation"><div class="delegation-head"><strong>${escapeHtml(item.agent)}</strong><span>${escapeHtml(item.status)}</span></div><span>${item.stepId} · delegated by orchestrator</span></div>`).join('') : '<div class="empty-state">Chưa có delegation.</div>';
}

function renderTools() {
  const calls = session.agentRuntime.toolCalls || [];
  ui.toolCounter.textContent = `${calls.length} calls`;
  ui.toolCalls.innerHTML = calls.length ? [...calls].reverse().map(call => `
    <div class="tool-call ${call.sideEffect ? 'risk-write' : ''}">
      <div class="tool-head"><strong>${escapeHtml(call.capabilityId)}</strong><span>${escapeHtml(call.risk)}</span></div>
      <p>${escapeHtml(call.caller)} · ${call.sideEffect ? 'SIDE EFFECT' : 'READ / VALIDATE'}</p>
    </div>`).join('') : '<div class="empty-state">Tool Gateway chưa nhận lệnh.</div>';
}

function renderButtons() {
  const ready = ['DRAFT_READY', 'AWAITING_APPROVAL', 'READY_TO_POST'].includes(session.status);
  const cashWorkflow = session.workflow?.startsWith('cash_');
  ui.transferApprovalRow.classList.toggle('hidden', cashWorkflow);
  ui.cashSubmitRow.classList.toggle('hidden', !cashWorkflow);
  document.querySelectorAll('.approval-button').forEach(button => {
    const actor = button.dataset.actor;
    const required = actor !== 'supervisor' || session.transactionDraft.limit?.supervisorRequired;
    button.disabled = !ready || !required || Boolean(session.approvals[actor]);
    button.classList.toggle('approved', Boolean(session.approvals[actor]));
  });
  ui.execute.disabled = !session.control.postingAllowed || session.status === 'POSTED';
  ui.execute.textContent = session.status === 'POSTED' ? `Đã ghi ${session.execution.coreReference}` : 'Gửi Core Mock';
  ui.cashSubmit.disabled = !ready || session.status === 'POSTED';
  ui.cashSubmit.textContent = session.status === 'POSTED' ? `Đã ghi ${session.execution.coreReference}` : 'Xác nhận & Submit Mock';
}

async function api(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
  const body = await response.json();
  if (!response.ok) throw new Error(body.error?.message || `HTTP ${response.status}`);
  return body;
}

function setBusy(busy) {
  ui.send.disabled = busy;
  ui.send.textContent = busy ? 'Đang điều phối…' : 'Phân tích yêu cầu';
}

function showToast(message, error = false) {
  ui.toast.textContent = message;
  ui.toast.className = `toast show${error ? ' error' : ''}`;
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => { ui.toast.className = 'toast'; }, 3200);
}

function showError(error) { showToast(error.message || String(error), true); }
function money(value) { return value == null ? null : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value); }
function maskAccount(value) { const text = String(value || ''); return text.length <= 4 ? text : `${'•'.repeat(text.length - 4)}${text.slice(-4)}`; }
function escapeHtml(value) { return String(value ?? '').replace(/[&<>'"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[character]); }
