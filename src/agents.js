'use strict';

const BANK_ALIASES = [
  ['vietcombank', 'VCB'], ['vcb', 'VCB'], ['bidv', 'BIDV'], ['vietinbank', 'CTG'], ['ctg', 'CTG'], ['techcombank', 'TCB'], ['tcb', 'TCB'],
];

function parseAmount(text) {
  const normalized = text.toLowerCase().replace(/,/g, '.');
  const match = normalized.match(/(\d+(?:\.\d+)?)\s*(tỷ|ty|triệu|tr|nghìn|ngan|k)\b/);
  if (match) {
    const multipliers = { 'tỷ': 1_000_000_000, ty: 1_000_000_000, 'triệu': 1_000_000, tr: 1_000_000, 'nghìn': 1_000, ngan: 1_000, k: 1_000 };
    return Math.round(Number(match[1]) * multipliers[match[2]]);
  }
  const currencyMatch = normalized.match(/(?:số tiền|chuyển|rút)\s+(\d{5,})\b/);
  return currencyMatch ? Number(currencyMatch[1]) : null;
}

function extractEntities(text, previous = {}) {
  const lower = text.toLowerCase();
  const accountMatches = text.match(/\b\d{6,14}\b/g) || [];
  const bank = BANK_ALIASES.find(([alias]) => lower.includes(alias));
  const nameMatch = text.match(/(?:cho|tên|người nhận)\s+([A-Za-zÀ-ỹĐđ\s]{3,40}?)(?=\s+(?:tại|ở|ngân hàng|số tài khoản|stk|tk)|[,.;]|$)/i);
  return {
    ...previous,
    amount: parseAmount(text) || previous.amount || null,
    beneficiaryAccount: accountMatches.at(-1) || previous.beneficiaryAccount || null,
    bankCode: bank?.[1] || previous.bankCode || null,
    beneficiaryName: nameMatch?.[1]?.trim() || previous.beneficiaryName || null,
  };
}

function extractCashEntities(text, previous = {}) {
  const accountMatches = text.match(/\b\d{6,14}\b/g) || [];
  return {
    ...previous,
    amount: parseAmount(text) || previous.amount || null,
    accountNumber: accountMatches.at(-1) || previous.accountNumber || null,
  };
}

class MockReasoningEngine {
  detectIntent(text, currentIntent) {
    const lower = text.toLowerCase();
    if (/(nộp tiền|nộp\s+\d)/i.test(lower)) {
      return { type: 'CASH_DEPOSIT', workflow: 'cash_deposit', confidence: 0.97, entities: extractCashEntities(text) };
    }
    if (/(rút tiền|rút\s+\d)/i.test(lower)) {
      return { type: 'CASH_WITHDRAWAL', workflow: 'cash_withdrawal', confidence: 0.97, entities: extractCashEntities(text) };
    }
    if (/(chuyển khoản|chuyển tiền|\btransfer\b|chuyển\s+\d)/i.test(lower)) {
      return { type: 'DOMESTIC_TRANSFER', workflow: 'domestic_transfer', confidence: 0.94, entities: extractEntities(text) };
    }
    if (currentIntent?.type === 'DOMESTIC_TRANSFER') {
      return { ...currentIntent, confidence: 0.99, entities: extractEntities(text, currentIntent.entities) };
    }
    if (['CASH_DEPOSIT', 'CASH_WITHDRAWAL'].includes(currentIntent?.type)) {
      return { ...currentIntent, confidence: 0.99, entities: extractCashEntities(text, currentIntent.entities) };
    }
    return { type: 'POLICY_ASSISTANCE', workflow: 'policy_assistance', confidence: 0.82, entities: {}, query: text };
  }

  proposePlan(state) {
    if (state.intent.type === 'POLICY_ASSISTANCE') {
      return {
        objective: 'Trả lời yêu cầu nghiệp vụ bằng nguồn đã phê duyệt',
        steps: [{ id: 'S1', kind: 'DELEGATE', target: 'policy_agent', dependsOn: [] }],
        completionCriteria: ['Có câu trả lời', 'Có citation'],
      };
    }
    const objective = state.intent.type === 'CASH_DEPOSIT'
      ? 'Mở màn hình nộp tiền và tự điền bản nháp, không posting'
      : state.intent.type === 'CASH_WITHDRAWAL'
        ? 'Mở màn hình rút tiền và tự điền bản nháp, không posting'
        : 'Tạo bản nháp chuyển khoản trong nước, không posting';
    return {
      objective,
      steps: [
        { id: 'S1', kind: 'DELEGATE', target: 'customer_context_agent', dependsOn: [] },
        { id: 'S2', kind: 'DELEGATE', target: 'policy_agent', dependsOn: [] },
        { id: 'S3', kind: 'DELEGATE', target: 'transaction_draft_agent', dependsOn: ['S1', 'S2'] },
        { id: 'S4', kind: 'DELEGATE', target: 'risk_assistant', dependsOn: ['S3'] },
      ],
      completionCriteria: ['Có đủ trường bắt buộc', 'Qua validation', 'Có risk decision', 'Không posting'],
    };
  }
}

class AgentSuite {
  constructor(toolGateway) {
    this.tools = toolGateway;
  }

  async run(agent, state) {
    const handlers = {
      customer_context_agent: () => this.#customerContext(state),
      policy_agent: () => this.#policy(state),
      transaction_draft_agent: () => this.#transactionDraft(state),
      risk_assistant: () => this.#risk(state),
    };
    if (!handlers[agent]) throw new Error(`Agent không tồn tại: ${agent}`);
    return handlers[agent]();
  }

  async #call(agent, state, capabilityId, args) {
    return this.tools.call({ caller: agent, workflow: state.workflow, capabilityId, args });
  }

  async #customerContext(state) {
    const profileCall = await this.#call('customer_context_agent', state, 'customer.profile.read', { customerRef: state.customerRef });
    const toolCalls = [profileCall];
    let accounts = { accounts: [] };
    if (state.intent.type === 'DOMESTIC_TRANSFER') {
      const accountCall = await this.#call('customer_context_agent', state, 'customer.accounts.list', { customerRef: state.customerRef });
      toolCalls.push(accountCall);
      accounts = accountCall.result;
    }
    return {
      agent: 'customer_context_agent',
      writes: { customerContext: { ...profileCall.result, ...accounts } },
      evidence: ['MOCK-CIF-SNAPSHOT'],
      toolCalls,
    };
  }

  async #policy(state) {
    const cashQuery = state.intent.type === 'CASH_DEPOSIT'
      ? 'quy trình nộp tiền mặt vào tài khoản tại quầy'
      : 'quy trình rút tiền mặt từ tài khoản tại quầy';
    const policyCall = await this.#call('policy_agent', state, 'knowledge.policy.search', {
      query: state.intent.query || (state.workflow.startsWith('cash_') ? cashQuery : 'điều kiện và phí chuyển khoản tại quầy'),
    });
    const toolCalls = [policyCall];
    let fee = null;
    if (state.intent.type === 'DOMESTIC_TRANSFER' && state.intent.entities.amount) {
      const feeCall = await this.#call('policy_agent', state, 'pricing.transfer.fee', { amount: state.intent.entities.amount });
      toolCalls.push(feeCall);
      fee = feeCall.result;
    }
    return {
      agent: 'policy_agent',
      writes: { policyFindings: { ...policyCall.result, fee } },
      evidence: policyCall.result.citations.map(item => `${item.document}#${item.section}`),
      toolCalls,
    };
  }

  async #transactionDraft(state) {
    if (state.workflow.startsWith('cash_')) return this.#cashTransactionDraft(state);
    const entities = state.intent.entities;
    const sourceAccount = state.customerContext.accounts?.[0];
    const toolCalls = [];
    let bank = null;
    if (entities.bankCode) {
      const call = await this.#call('transaction_draft_agent', state, 'bank.directory.lookup', { bankCode: entities.bankCode });
      toolCalls.push(call);
      bank = call.result;
    }
    let limit = null;
    if (entities.amount) {
      const call = await this.#call('transaction_draft_agent', state, 'transfer.limit.check', { amount: entities.amount });
      toolCalls.push(call);
      limit = call.result;
    }
    const draft = {
      sourceAccountRef: sourceAccount?.accountRef || null,
      sourceAccountMasked: sourceAccount?.accountNoMasked || null,
      beneficiaryAccount: entities.beneficiaryAccount,
      beneficiaryName: entities.beneficiaryName,
      bankCode: entities.bankCode,
      bankName: bank?.bankName || null,
      amount: entities.amount,
      fee: state.policyFindings.fee,
      limit,
      currency: 'VND',
    };
    const validationCall = await this.#call('transaction_draft_agent', state, 'transaction.draft.validate', draft);
    toolCalls.push(validationCall);
    return {
      agent: 'transaction_draft_agent',
      writes: { transactionDraft: { ...draft, validation: validationCall.result } },
      evidence: ['TRANSACTION-DRAFT-SCHEMA-V2'],
      toolCalls,
    };
  }

  async #cashTransactionDraft(state) {
    const entities = state.intent.entities;
    const toolCalls = [];
    let account = null;
    if (entities.accountNumber) {
      const call = await this.#call('transaction_draft_agent', state, 'account.resolve.by_number', { accountNumber: entities.accountNumber });
      toolCalls.push(call);
      account = call.result;
    }
    const transactionType = state.intent.type;
    let limit = null;
    if (entities.amount) {
      const call = await this.#call('transaction_draft_agent', state, 'cash.limit.check', {
        amount: entities.amount,
        availableBalance: account?.availableBalance,
        transactionType,
      });
      toolCalls.push(call);
      limit = call.result;
    }
    const draft = {
      screenCode: transactionType === 'CASH_DEPOSIT' ? 'CASH_DEPOSIT_SCREEN' : 'CASH_WITHDRAWAL_SCREEN',
      screenTitle: transactionType === 'CASH_DEPOSIT' ? 'Màn hình Nộp tiền' : 'Màn hình Rút tiền',
      transactionType,
      accountRef: account?.accountRef || null,
      accountNumber: entities.accountNumber,
      accountNoMasked: account?.accountNoMasked || null,
      accountHolder: account?.accountHolder || null,
      accountStatus: account?.status || null,
      availableBalance: account?.availableBalance ?? null,
      amount: entities.amount,
      currency: 'VND',
      description: transactionType === 'CASH_DEPOSIT' ? 'Nộp tiền mặt tại quầy' : 'Rút tiền mặt tại quầy',
      limit,
    };
    const validationCall = await this.#call('transaction_draft_agent', state, 'cash.draft.validate', draft);
    toolCalls.push(validationCall);
    return {
      agent: 'transaction_draft_agent',
      writes: { transactionDraft: { ...draft, validation: validationCall.result } },
      evidence: ['CASH-TRANSACTION-DRAFT-SCHEMA-V1'],
      toolCalls,
    };
  }

  async #risk(state) {
    const capabilityId = state.workflow.startsWith('cash_') ? 'risk.cash.screen' : 'risk.transfer.screen';
    const call = await this.#call('risk_assistant', state, capabilityId, state.transactionDraft);
    return {
      agent: 'risk_assistant',
      writes: { risk: call.result },
      evidence: [call.result.model],
      toolCalls: [call],
    };
  }
}

module.exports = { MockReasoningEngine, AgentSuite, extractEntities, extractCashEntities, parseAmount };
