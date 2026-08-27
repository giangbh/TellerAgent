'use strict';

const { getCapability } = require('./capability-registry');

class ToolPolicyError extends Error {
  constructor(message, code = 'TOOL_POLICY_DENIED') {
    super(message);
    this.name = 'ToolPolicyError';
    this.code = code;
  }
}

class MockToolGateway {
  constructor() {
    this.executions = new Map();
  }

  async call({ caller, workflow, capabilityId, args = {}, idempotencyKey }) {
    const capability = getCapability(capabilityId);
    if (!capability) throw new ToolPolicyError(`Capability không tồn tại: ${capabilityId}`, 'UNKNOWN_CAPABILITY');
    if (!capability.allowedCallers.includes(caller)) {
      throw new ToolPolicyError(`${caller} không được phép gọi ${capabilityId}`);
    }
    if (!capability.workflows.includes(workflow)) {
      throw new ToolPolicyError(`${capabilityId} không thuộc workflow ${workflow}`);
    }
    if (capability.requiresIdempotency && !idempotencyKey) {
      throw new ToolPolicyError(`${capabilityId} yêu cầu idempotency key`, 'IDEMPOTENCY_REQUIRED');
    }

    const startedAt = new Date().toISOString();
    const result = await this.#dispatch(capabilityId, args, idempotencyKey);
    return {
      capabilityId,
      caller,
      risk: capability.risk,
      sideEffect: capability.sideEffect,
      startedAt,
      completedAt: new Date().toISOString(),
      args: maskArgs(args),
      result,
    };
  }

  async #dispatch(capabilityId, args, idempotencyKey) {
    switch (capabilityId) {
      case 'customer.profile.read':
        return {
          customerRef: args.customerRef || 'CIF-0001842',
          displayName: 'Nguyễn Minh Anh',
          kycStatus: 'VERIFIED',
          segment: 'PRIORITY',
          riskRating: 'LOW',
          identityMasked: '0123••••89',
        };
      case 'customer.accounts.list':
        return {
          accounts: [
            { accountRef: 'ACC-001', accountNoMasked: '•••• 6789', currency: 'VND', availableBalance: 250_000_000 },
            { accountRef: 'ACC-002', accountNoMasked: '•••• 2486', currency: 'VND', availableBalance: 42_500_000 },
          ],
        };
      case 'knowledge.policy.search':
        if (/nộp|rút|tiền mặt/i.test(args.query || '')) {
          return {
            answer: 'GDV phải đối chiếu số tài khoản, chủ tài khoản, số tiền và chứng từ trước khi xác nhận giao dịch tiền mặt.',
            citations: [
              { document: 'QTTM-2026', section: '3.1', title: 'Quy trình giao dịch tiền mặt tại quầy', effectiveDate: '2026-01-01' },
            ],
          };
        }
        return {
          answer: 'Chuyển khoản tại quầy yêu cầu khách hàng đã xác thực, kiểm tra thông tin người thụ hưởng và xác nhận phí trước khi gửi core.',
          citations: [
            { document: 'QTTT-CK-2026', section: '4.2', title: 'Quy trình chuyển khoản trong nước', effectiveDate: '2026-01-01' },
            { document: 'BIEUPHI-2026', section: '2.1', title: 'Biểu phí dịch vụ tại quầy', effectiveDate: '2026-04-01' },
          ],
        };
      case 'pricing.transfer.fee': {
        const amount = Number(args.amount || 0);
        const fee = Math.min(1_000_000, Math.max(10_000, Math.round(amount * 0.0003)));
        return { amount, fee, vat: Math.round(fee * 0.1), totalFee: fee + Math.round(fee * 0.1), currency: 'VND', mock: true };
      }
      case 'bank.directory.lookup': {
        const bankCode = String(args.bankCode || '').toUpperCase();
        const banks = {
          VCB: 'Ngân hàng TMCP Ngoại thương Việt Nam',
          BIDV: 'Ngân hàng TMCP Đầu tư và Phát triển Việt Nam',
          CTG: 'Ngân hàng TMCP Công thương Việt Nam',
          TCB: 'Ngân hàng TMCP Kỹ thương Việt Nam',
        };
        return { bankCode, bankName: banks[bankCode] || 'Ngân hàng thụ hưởng (mock)', serviceStatus: 'AVAILABLE' };
      }
      case 'transfer.limit.check': {
        const amount = Number(args.amount || 0);
        return {
          amount,
          tellerLimit: 100_000_000,
          withinTellerLimit: amount <= 100_000_000,
          supervisorRequired: amount > 100_000_000,
        };
      }
      case 'transaction.draft.validate': {
        const required = ['sourceAccountRef', 'beneficiaryAccount', 'beneficiaryName', 'bankCode', 'amount'];
        const missingFields = required.filter(field => !args[field]);
        return { valid: missingFields.length === 0, missingFields };
      }
      case 'risk.transfer.screen': {
        const amount = Number(args.amount || 0);
        const beneficiaryAccount = String(args.beneficiaryAccount || '');
        const flagged = amount >= 200_000_000 || beneficiaryAccount.endsWith('999');
        return {
          decision: flagged ? 'REVIEW' : 'PASS',
          alerts: flagged ? ['Giao dịch cần rà soát bổ sung theo kịch bản mock.'] : [],
          model: 'MOCK_RULESET_2026.1',
        };
      }
      case 'account.resolve.by_number': {
        const accountNumber = String(args.accountNumber || '');
        const known = accountNumber === '3456789';
        return {
          accountRef: known ? 'ACC-CASH-3456789' : `ACC-CASH-${accountNumber}`,
          accountNumber,
          accountNoMasked: maskAccount(accountNumber),
          accountHolder: known ? 'Nguyễn Minh Anh' : 'Chủ tài khoản Mock',
          status: accountNumber ? 'ACTIVE' : 'NOT_FOUND',
          currency: 'VND',
          availableBalance: known ? 180_000_000 : 90_000_000,
        };
      }
      case 'cash.limit.check': {
        const amount = Number(args.amount || 0);
        const availableBalance = Number(args.availableBalance || 0);
        const withdrawal = args.transactionType === 'CASH_WITHDRAWAL';
        return {
          amount,
          tellerLimit: 100_000_000,
          withinTellerLimit: amount <= 100_000_000,
          supervisorRequired: amount > 100_000_000,
          sufficientFunds: !withdrawal || amount <= availableBalance,
        };
      }
      case 'cash.draft.validate': {
        const required = ['accountRef', 'accountNumber', 'amount', 'transactionType'];
        const missingFields = required.filter(field => !args[field]);
        return {
          valid: missingFields.length === 0 && args.accountStatus === 'ACTIVE' && args.limit?.sufficientFunds !== false,
          missingFields,
          blockers: args.limit?.sufficientFunds === false ? ['INSUFFICIENT_AVAILABLE_BALANCE'] : [],
        };
      }
      case 'risk.cash.screen': {
        const amount = Number(args.amount || 0);
        const insufficientFunds = args.limit?.sufficientFunds === false;
        const flagged = amount >= 300_000_000 || insufficientFunds;
        return {
          decision: flagged ? 'REVIEW' : 'PASS',
          alerts: insufficientFunds
            ? ['Số dư khả dụng không đủ cho giao dịch rút tiền mock.']
            : flagged ? ['Giao dịch tiền mặt giá trị cao cần rà soát bổ sung.'] : [],
          model: 'MOCK_CASH_RULESET_2026.1',
        };
      }
      case 'core.transfer.execute': {
        if (this.executions.has(idempotencyKey)) return this.executions.get(idempotencyKey);
        const result = {
          status: 'POSTED',
          coreReference: `FT${String(this.executions.size + 1).padStart(8, '0')}`,
          postedAt: new Date().toISOString(),
          amount: Number(args.amount),
          mock: true,
        };
        this.executions.set(idempotencyKey, result);
        return result;
      }
      case 'core.cash.execute': {
        if (this.executions.has(idempotencyKey)) return this.executions.get(idempotencyKey);
        const prefix = args.transactionType === 'CASH_WITHDRAWAL' ? 'CW' : 'CD';
        const result = {
          status: 'POSTED',
          coreReference: `${prefix}${String(this.executions.size + 1).padStart(8, '0')}`,
          postedAt: new Date().toISOString(),
          amount: Number(args.amount),
          transactionType: args.transactionType,
          mock: true,
        };
        this.executions.set(idempotencyKey, result);
        return result;
      }
      default:
        throw new ToolPolicyError(`Chưa có mock handler cho ${capabilityId}`, 'MOCK_NOT_IMPLEMENTED');
    }
  }
}

function maskArgs(args) {
  const copy = { ...args };
  if (copy.beneficiaryAccount) copy.beneficiaryAccount = maskAccount(copy.beneficiaryAccount);
  if (copy.accountNumber) copy.accountNumber = maskAccount(copy.accountNumber);
  if (copy.customerRef) copy.customerRef = String(copy.customerRef).replace(/.(?=.{4})/g, '•');
  return copy;
}

function maskAccount(value) {
  const text = String(value);
  return text.length <= 4 ? text : `${'•'.repeat(text.length - 4)}${text.slice(-4)}`;
}

module.exports = { MockToolGateway, ToolPolicyError };
