'use strict';

const CAPABILITIES = [
  {
    id: 'customer.profile.read',
    type: 'TOOL',
    description: 'Đọc hồ sơ khách hàng đã được giới hạn cho phiên Teller.',
    risk: 'SENSITIVE_READ',
    sideEffect: false,
    allowedCallers: ['customer_context_agent'],
    workflows: ['domestic_transfer', 'cash_deposit', 'cash_withdrawal', 'policy_assistance'],
  },
  {
    id: 'customer.accounts.list',
    type: 'TOOL',
    description: 'Liệt kê tài khoản nguồn dưới dạng che số.',
    risk: 'SENSITIVE_READ',
    sideEffect: false,
    allowedCallers: ['customer_context_agent'],
    workflows: ['domestic_transfer', 'cash_withdrawal'],
  },
  {
    id: 'knowledge.policy.search',
    type: 'TOOL',
    description: 'Tra cứu kho quy trình đã phê duyệt và trả citation.',
    risk: 'READ_SAFE',
    sideEffect: false,
    allowedCallers: ['policy_agent'],
    workflows: ['domestic_transfer', 'cash_deposit', 'cash_withdrawal', 'policy_assistance'],
  },
  {
    id: 'pricing.transfer.fee',
    type: 'TOOL',
    description: 'Tính phí chuyển khoản tại quầy theo dữ liệu mock.',
    risk: 'READ_SAFE',
    sideEffect: false,
    allowedCallers: ['policy_agent'],
    workflows: ['domestic_transfer'],
  },
  {
    id: 'bank.directory.lookup',
    type: 'TOOL',
    description: 'Tra cứu mã và tên ngân hàng thụ hưởng.',
    risk: 'READ_SAFE',
    sideEffect: false,
    allowedCallers: ['transaction_draft_agent'],
    workflows: ['domestic_transfer'],
  },
  {
    id: 'transfer.limit.check',
    type: 'TOOL',
    description: 'Kiểm tra hạn mức Teller và yêu cầu cấp phê duyệt.',
    risk: 'CONTROL_READ',
    sideEffect: false,
    allowedCallers: ['transaction_draft_agent'],
    workflows: ['domestic_transfer'],
  },
  {
    id: 'transaction.draft.validate',
    type: 'TOOL',
    description: 'Kiểm tra schema và trường bắt buộc của bản nháp.',
    risk: 'DRAFT',
    sideEffect: false,
    allowedCallers: ['transaction_draft_agent'],
    workflows: ['domestic_transfer'],
  },
  {
    id: 'risk.transfer.screen',
    type: 'TOOL',
    description: 'Mô phỏng kiểm tra AML/fraud cho giao dịch chuyển tiền.',
    risk: 'CONTROL_READ',
    sideEffect: false,
    allowedCallers: ['risk_assistant'],
    workflows: ['domestic_transfer'],
  },
  {
    id: 'account.resolve.by_number',
    type: 'TOOL',
    description: 'Đối chiếu số tài khoản và trả thông tin chủ tài khoản mock.',
    risk: 'SENSITIVE_READ',
    sideEffect: false,
    allowedCallers: ['transaction_draft_agent'],
    workflows: ['cash_deposit', 'cash_withdrawal'],
  },
  {
    id: 'cash.limit.check',
    type: 'TOOL',
    description: 'Kiểm tra hạn mức GDV và số dư khả dụng cho giao dịch tiền mặt.',
    risk: 'CONTROL_READ',
    sideEffect: false,
    allowedCallers: ['transaction_draft_agent'],
    workflows: ['cash_deposit', 'cash_withdrawal'],
  },
  {
    id: 'cash.draft.validate',
    type: 'TOOL',
    description: 'Kiểm tra các trường bắt buộc của màn hình nộp/rút tiền.',
    risk: 'DRAFT',
    sideEffect: false,
    allowedCallers: ['transaction_draft_agent'],
    workflows: ['cash_deposit', 'cash_withdrawal'],
  },
  {
    id: 'risk.cash.screen',
    type: 'TOOL',
    description: 'Mô phỏng kiểm tra risk cho giao dịch tiền mặt.',
    risk: 'CONTROL_READ',
    sideEffect: false,
    allowedCallers: ['risk_assistant'],
    workflows: ['cash_deposit', 'cash_withdrawal'],
  },
  {
    id: 'core.transfer.execute',
    type: 'TOOL',
    description: 'Mock posting giao dịch vào core banking.',
    risk: 'FINANCIAL_WRITE',
    sideEffect: true,
    allowedCallers: ['business_orchestrator'],
    workflows: ['domestic_transfer'],
    requiresIdempotency: true,
  },
  {
    id: 'core.cash.execute',
    type: 'TOOL',
    description: 'Mock posting giao dịch nộp/rút tiền vào core banking.',
    risk: 'FINANCIAL_WRITE',
    sideEffect: true,
    allowedCallers: ['business_orchestrator'],
    workflows: ['cash_deposit', 'cash_withdrawal'],
    requiresIdempotency: true,
  },
];

const capabilityMap = new Map(CAPABILITIES.map(capability => [capability.id, capability]));

function getCapability(id) {
  return capabilityMap.get(id) || null;
}

function listCapabilities({ caller, workflow } = {}) {
  return CAPABILITIES.filter(capability =>
    (!caller || capability.allowedCallers.includes(caller)) &&
    (!workflow || capability.workflows.includes(workflow))
  );
}

module.exports = { CAPABILITIES, getCapability, listCapabilities };
