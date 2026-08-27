export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  text: string;
  at: string;
}

export interface Intent {
  type: string;
  workflow: string;
  confidence: number;
  entities: {
    amount?: number | null;
    beneficiaryAccount?: string | null;
    beneficiaryName?: string | null;
    bankCode?: string | null;
    accountNumber?: string | null;
    [key: string]: unknown;
  };
  query?: string;
}

export interface PlanStep {
  id: string;
  kind: string;
  target: string;
  dependsOn: string[];
}

export interface Plan {
  objective: string;
  steps: PlanStep[];
  completionCriteria: string[];
}

export interface DelegationInfo {
  stepId: string;
  agent: string;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
  startedAt: string;
  completedAt?: string;
}

export interface ToolCallRecord {
  capabilityId: string;
  toolName?: string;
  caller: string;
  risk: string;
  sideEffect: boolean;
  startedAt: string;
  completedAt: string;
  args: Record<string, unknown>;
  result: Record<string, unknown>;
}

export interface AgentRuntime {
  reasoningMode: string;
  plan: Plan | null;
  completedSteps: string[];
  delegations: DelegationInfo[];
  toolCalls: ToolCallRecord[];
  budget: {
    maxDelegations: number;
    maxToolCalls: number;
    delegationsUsed: number;
    toolCallsUsed: number;
  };
}

export interface ValidationResult {
  valid: boolean;
  missingFields: string[];
  blockers: string[];
}

export interface TransactionDraft {
  screenCode?: string;
  screenTitle?: string;
  transactionType?: string;
  sourceAccountRef?: string | null;
  sourceAccountMasked?: string | null;
  accountRef?: string | null;
  accountNumber?: string | null;
  accountNoMasked?: string | null;
  accountHolder?: string | null;
  accountStatus?: string | null;
  availableBalance?: number | null;
  beneficiaryAccount?: string | null;
  beneficiaryName?: string | null;
  bankCode?: string | null;
  bankName?: string | null;
  amount?: number | null;
  fee?: {
    amount: number;
    fee: number;
    vat: number;
    totalFee: number;
    currency: string;
  } | null;
  currency?: string;
  description?: string;
  limit?: {
    amount?: number;
    tellerLimit?: number;
    withinTellerLimit?: boolean;
    supervisorRequired?: boolean;
    sufficientFunds?: boolean;
  } | null;
  validation?: ValidationResult | null;
}

export interface Citation {
  document: string;
  section: string;
  title: string;
  effectiveDate: string;
}

export interface PolicyFinding {
  answer?: string;
  citations?: Citation[];
  fee?: unknown;
}

export interface RiskAssessment {
  decision?: 'PASS' | 'REVIEW' | 'FLAGGED';
  alerts?: string[];
  model?: string;
}

export interface Approvals {
  customer: boolean;
  teller: boolean;
  supervisor: boolean;
}

export interface ControlGate {
  postingAllowed: boolean;
  missingGates: string[];
  lastEvaluatedAt?: string;
}

export interface ExecutionResult {
  status: string;
  coreReference: string;
  postedAt: string;
  amount?: number;
  transactionType?: string;
  mock: boolean;
}

export interface SessionEvent {
  seq: number;
  at: string;
  type: string;
  title: string;
  detail: string;
}

export interface Session {
  sessionId: string;
  transactionId?: string | null;
  revision: number;
  createdAt: string;
  updatedAt: string;
  branchId: string;
  counterId: string;
  tellerId: string;
  customerRef: string;
  workflow?: string | null;
  workflowVersion?: string | null;
  status: string;
  intent?: Intent | null;
  messages: ChatMessage[];
  customerContext: Record<string, unknown>;
  policyFindings: PolicyFinding;
  transactionDraft: TransactionDraft;
  risk: RiskAssessment;
  approvals: Approvals;
  control: ControlGate;
  execution?: ExecutionResult | null;
  agentRuntime: AgentRuntime;
  events: SessionEvent[];
}

export interface CapabilityItem {
  id: string;
  name: string;
  type: string;
  description: string;
  risk: string;
  sideEffect: boolean;
  allowedCallers: string[];
  workflows: string[];
  requiresIdempotency: boolean;
  inputSchema?: Record<string, unknown>;
}

export interface ScenarioItem {
  id: string;
  name: string;
  prompt: string;
}

export interface BootstrapData {
  product: string;
  mode: string;
  capabilities: CapabilityItem[];
  scenarios: ScenarioItem[];
}
