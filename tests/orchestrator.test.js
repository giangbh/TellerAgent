'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { TellerOrchestrator, OrchestratorError } = require('../src/orchestrator');
const { MockToolGateway, ToolPolicyError } = require('../src/mock-tools');

test('Tool Gateway từ chối agent gọi financial-write tool', async () => {
  const gateway = new MockToolGateway();
  await assert.rejects(
    gateway.call({
      caller: 'transaction_draft_agent',
      workflow: 'domestic_transfer',
      capabilityId: 'core.transfer.execute',
      args: { amount: 1_000_000 },
      idempotencyKey: 'IDEMPOTENCY-1',
    }),
    error => error instanceof ToolPolicyError && error.code === 'TOOL_POLICY_DENIED'
  );
});

test('agent tự lập plan, delegate và tạo draft chuyển khoản hoàn chỉnh', async () => {
  const orchestrator = new TellerOrchestrator();
  const opened = orchestrator.createSession();
  const state = await orchestrator.processMessage(
    opened.sessionId,
    'Chuyển 50 triệu cho Nguyễn Văn An tại Vietcombank, số tài khoản 012345678901'
  );

  assert.equal(state.workflow, 'domestic_transfer');
  assert.equal(state.status, 'DRAFT_READY');
  assert.equal(state.transactionDraft.amount, 50_000_000);
  assert.equal(state.transactionDraft.beneficiaryAccount, '012345678901');
  assert.equal(state.transactionDraft.bankCode, 'VCB');
  assert.equal(state.transactionDraft.validation.valid, true);
  assert.equal(state.risk.decision, 'PASS');
  assert.deepEqual(state.agentRuntime.completedSteps, ['S1', 'S2', 'S3', 'S4']);
  assert.ok(state.agentRuntime.toolCalls.some(call => call.capabilityId === 'pricing.transfer.fee'));
  assert.ok(state.agentRuntime.toolCalls.some(call => call.capabilityId === 'risk.transfer.screen'));
  assert.ok(!state.agentRuntime.toolCalls.some(call => call.capabilityId === 'core.transfer.execute'));
});

test('orchestrator dừng tại human input khi thiếu trường và resume bằng message tiếp theo', async () => {
  const orchestrator = new TellerOrchestrator();
  const opened = orchestrator.createSession();
  const waiting = await orchestrator.processMessage(opened.sessionId, 'Chuyển 25 triệu sang Vietcombank');
  assert.equal(waiting.status, 'WAITING_HUMAN_INPUT');
  assert.deepEqual(waiting.transactionDraft.validation.missingFields.sort(), ['beneficiaryAccount', 'beneficiaryName'].sort());

  const resumed = await orchestrator.processMessage(opened.sessionId, 'Chuyển cho Lê Minh Quân số tài khoản 123456789012');
  assert.equal(resumed.status, 'DRAFT_READY');
  assert.equal(resumed.transactionDraft.beneficiaryName, 'Lê Minh Quân');
  assert.equal(resumed.transactionDraft.beneficiaryAccount, '123456789012');
});

test('customer phải xác nhận trước Teller và core chỉ mở sau đủ control gates', async () => {
  const orchestrator = new TellerOrchestrator();
  const opened = orchestrator.createSession();
  await orchestrator.processMessage(opened.sessionId, 'Chuyển 50 triệu cho Nguyễn Văn An tại VCB, số tài khoản 012345678901');

  assert.throws(
    () => orchestrator.approve(opened.sessionId, 'teller'),
    error => error instanceof OrchestratorError && error.code === 'CUSTOMER_APPROVAL_REQUIRED'
  );
  let state = orchestrator.approve(opened.sessionId, 'customer');
  assert.equal(state.control.postingAllowed, false);
  state = orchestrator.approve(opened.sessionId, 'teller');
  assert.equal(state.status, 'READY_TO_POST');
  assert.equal(state.control.postingAllowed, true);

  const posted = await orchestrator.execute(opened.sessionId);
  assert.equal(posted.status, 'POSTED');
  assert.match(posted.execution.coreReference, /^FT\d{8}$/);
  const repeated = await orchestrator.execute(opened.sessionId);
  assert.equal(repeated.execution.coreReference, posted.execution.coreReference);
});

test('risk mock chặn luồng trước approval', async () => {
  const orchestrator = new TellerOrchestrator();
  const opened = orchestrator.createSession();
  const state = await orchestrator.processMessage(
    opened.sessionId,
    'Chuyển 220 triệu cho Trần Thị Lan tại BIDV, số tài khoản 123456789999'
  );
  assert.equal(state.status, 'RISK_REVIEW');
  assert.equal(state.risk.decision, 'REVIEW');
  assert.equal(state.control.postingAllowed, false);
  assert.throws(() => orchestrator.approve(opened.sessionId, 'customer'), /chưa sẵn sàng/i);
});

test('policy assistance chỉ gọi Policy Agent và trả citation', async () => {
  const orchestrator = new TellerOrchestrator();
  const opened = orchestrator.createSession();
  const state = await orchestrator.processMessage(opened.sessionId, 'Quy trình và biểu phí tại quầy như thế nào?');
  assert.equal(state.status, 'ASSISTANCE_READY');
  assert.equal(state.agentRuntime.delegations.length, 1);
  assert.equal(state.agentRuntime.delegations[0].agent, 'policy_agent');
  assert.ok(state.policyFindings.citations.length >= 1);
});

test('nộp tiền tự mở đúng màn hình và tự điền amount, account trước khi GDV submit', async () => {
  const orchestrator = new TellerOrchestrator();
  const opened = orchestrator.createSession();
  const draft = await orchestrator.processMessage(opened.sessionId, 'nộp tiền 50tr vào tài khoản 3456789');

  assert.equal(draft.intent.type, 'CASH_DEPOSIT');
  assert.equal(draft.workflow, 'cash_deposit');
  assert.equal(draft.status, 'DRAFT_READY');
  assert.equal(draft.transactionDraft.screenCode, 'CASH_DEPOSIT_SCREEN');
  assert.equal(draft.transactionDraft.screenTitle, 'Màn hình Nộp tiền');
  assert.equal(draft.transactionDraft.amount, 50_000_000);
  assert.equal(draft.transactionDraft.accountNumber, '3456789');
  assert.equal(draft.transactionDraft.accountHolder, 'Nguyễn Minh Anh');
  assert.equal(draft.transactionDraft.validation.valid, true);
  assert.equal(draft.control.postingAllowed, false);
  assert.ok(draft.agentRuntime.toolCalls.some(call => call.capabilityId === 'account.resolve.by_number'));
  assert.ok(!draft.agentRuntime.toolCalls.some(call => call.capabilityId === 'core.cash.execute'));

  const posted = await orchestrator.confirmAndExecuteCash(opened.sessionId);
  assert.equal(posted.status, 'POSTED');
  assert.equal(posted.approvals.customer, true);
  assert.equal(posted.approvals.teller, true);
  assert.match(posted.execution.coreReference, /^CD\d{8}$/);
});

test('rút tiền tự mở màn hình rút tiền với dữ liệu đã điền', async () => {
  const orchestrator = new TellerOrchestrator();
  const opened = orchestrator.createSession();
  const draft = await orchestrator.processMessage(opened.sessionId, 'rút tiền 50tr từ tài khoản 3456789');

  assert.equal(draft.intent.type, 'CASH_WITHDRAWAL');
  assert.equal(draft.workflow, 'cash_withdrawal');
  assert.equal(draft.transactionDraft.screenCode, 'CASH_WITHDRAWAL_SCREEN');
  assert.equal(draft.transactionDraft.amount, 50_000_000);
  assert.equal(draft.transactionDraft.accountNumber, '3456789');
  assert.equal(draft.transactionDraft.limit.sufficientFunds, true);

  const posted = await orchestrator.confirmAndExecuteCash(opened.sessionId);
  assert.equal(posted.status, 'POSTED');
  assert.match(posted.execution.coreReference, /^CW\d{8}$/);
});
