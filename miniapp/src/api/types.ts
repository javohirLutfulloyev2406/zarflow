export type Language = "uz" | "ru";

export interface ProblemDetailFieldError {
  field: string;
  message: string;
}

/** Mirrors the backend's ProblemDetail shape (GlobalExceptionHandler.java). */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errorCode?: string;
  requestId?: string;
  errors?: ProblemDetailFieldError[];
}

export interface LoginResponseUser {
  id: string;
  firstName: string;
  displayName: string | null;
  language: Language;
  isNew: boolean;
}

/** Mirrors TelegramAuthController.LoginResponse. */
export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: LoginResponseUser;
}

/** Mirrors UserController.UserProfileResponse. Money stays a string end to end. */
export interface UserProfile {
  id: string;
  firstName: string | null;
  lastName: string | null;
  username: string | null;
  displayName: string | null;
  language: Language;
  timezone: string;
  expectedMonthlyIncome: string | null;
  onboardingCompleted: boolean;
  createdAt: string;
}

/** Mirrors UserController.UpdateSettingsRequest. */
export interface UpdateSettingsRequest {
  language: Language;
  timezone: string;
  expectedMonthlyIncome: string | null;
}

/** Mirrors UserController.OnboardingRequest. */
export interface OnboardingRequest {
  accountName: string;
  accountType: AccountType;
  initialBalance: string;
  expectedMonthlyIncome: string | null;
}

export type AccountType = "CASH" | "BANK_CARD" | "BANK_ACCOUNT" | "SAVINGS" | "OTHER";

/** Mirrors AccountController.AccountResponse. */
export interface Account {
  id: string;
  name: string;
  type: AccountType;
  currency: "UZS";
  initialBalance: string;
  color: string | null;
  isDefault: boolean;
  balance: string;
}

export interface CreateAccountRequest {
  name: string;
  type: AccountType;
  initialBalance: string;
  color?: string | null;
}

export interface UpdateAccountRequest {
  name: string;
  type: AccountType;
  color?: string | null;
}

export type TransactionType = "INCOME" | "EXPENSE" | "TRANSFER";
export type Necessity = "ESSENTIAL" | "DISCRETIONARY";

/** Mirrors CategoryController.CategoryResponse. */
export interface Category {
  id: string;
  code: string | null;
  nameUz: string;
  nameRu: string;
  type: "INCOME" | "EXPENSE";
  necessity: Necessity | null;
  icon: string;
  color: string;
  fixedDefault: boolean;
}

/** Mirrors TransactionController.TransactionResponse. */
export interface Transaction {
  id: string;
  accountId: string;
  categoryId: string | null;
  type: TransactionType;
  amount: string;
  currency: "UZS";
  necessity: Necessity | null;
  transactionDate: string;
  description: string | null;
  transferGroupId: string | null;
  isFixed: boolean;
}

export interface CreateTransactionRequest {
  accountId: string;
  categoryId: string;
  type: "INCOME" | "EXPENSE";
  amount: string;
  necessity?: Necessity | null;
  transactionDate?: string | null;
  description?: string | null;
  isFixed?: boolean | null;
}

export interface UpdateTransactionRequest {
  accountId: string;
  categoryId: string;
  amount: string;
  necessity?: Necessity | null;
  transactionDate?: string | null;
  description?: string | null;
  isFixed?: boolean | null;
}

export interface TransferRequest {
  fromAccountId: string;
  toAccountId: string;
  amount: string;
  transactionDate?: string | null;
  description?: string | null;
}

export interface TransferResponse {
  transferGroupId: string;
  fromLeg: Transaction;
  toLeg: Transaction;
}

export interface AdjustRequest {
  actualBalance: string;
  description?: string | null;
}

export interface TransactionsPage {
  items: Transaction[];
  totalElements: number;
  page: number;
  size: number;
}

export interface TransactionsSearchParams {
  from?: string;
  to?: string;
  type?: TransactionType;
  categoryId?: string;
  accountId?: string;
  q?: string;
  page?: number;
  size?: number;
}

export interface ExpenseLine {
  id: string;
  accountId: string;
  categoryId: string | null;
  amount: string;
  description: string | null;
  transactionDate: string;
}

export interface AccountBalance {
  accountId: string;
  accountName: string;
  balance: string;
}

/** Mirrors SummaryController.TodayAndMonthResponse. */
export interface TodayAndMonthSummary {
  today: { expenses: ExpenseLine[]; total: string };
  month: {
    income: string;
    expense: string;
    net: string;
    debtPayments: string;
    debtPrincipal: string;
    debtInterestAndFee: string;
  };
  balances: AccountBalance[];
}

/** Mirrors UserController.UpdateDisplayNameRequest. */
export interface UpdateDisplayNameRequest {
  displayName: string;
}

/** Mirrors AnalyticsController.CategoryBreakdownResponse. */
export interface AnalyticsCategoryBreakdown {
  categoryId: string;
  name: string;
  color: string | null;
  icon: string | null;
  amount: string;
  sharePercent: string;
  changeVsPrevMonthPercent: string | null;
}

/** Mirrors AnalyticsController.MonthResponse. */
export interface AnalyticsMonthResponse {
  income: string;
  expense: string;
  net: string;
  previousMonthExpense: string;
  expenseChangeVsPrevMonthPercent: string | null;
  essentialExpense: string;
  discretionaryExpense: string;
  dailyAverageExpense: string;
  categories: AnalyticsCategoryBreakdown[];
}

/** Mirrors AnalyticsController.DayTotalResponse. */
export interface AnalyticsCalendarDay {
  date: string;
  income: string;
  expense: string;
  count: number;
}

/** Mirrors AnalyticsController.CalendarResponse. */
export interface AnalyticsCalendarResponse {
  days: AnalyticsCalendarDay[];
}

/** Mirrors AnalyticsController.TransactionResponse. */
export interface AnalyticsDayTransaction {
  id: string;
  accountId: string;
  categoryId: string | null;
  type: TransactionType;
  amount: string;
  description: string | null;
  transactionDate: string;
}

/** Mirrors AnalyticsController.DayResponse. */
export interface AnalyticsDayResponse {
  transactions: AnalyticsDayTransaction[];
  income: string;
  expense: string;
}

/** Mirrors NotificationSettingsController.SettingsResponse. */
export interface NotificationSettings {
  dailyReportEnabled: boolean;
  dailyReportTime: string;
  weeklyReport: boolean;
  monthlyReport: boolean;
}

/** Mirrors NotificationSettingsController.UpdateSettingsRequest. */
export interface UpdateNotificationSettingsRequest {
  dailyReportEnabled: boolean;
  dailyReportTime: string;
  weeklyReport: boolean;
  monthlyReport: boolean;
}

/** Mirrors BudgetController.LimitResponse. */
export interface BudgetLimitView {
  categoryId: string;
  categoryName: string;
  categoryColor: string | null;
  categoryIcon: string | null;
  amount: string;
  spent: string;
  percent: string;
}

/** Mirrors BudgetController.BudgetResponse. Fields are null when hasBudget is false. */
export interface BudgetResponse {
  hasBudget: boolean;
  month: string;
  expectedIncome: string | null;
  fixedPayments: string | null;
  fixedPaymentsIsAuto: boolean;
  loanPayments: string | null;
  loanPaymentsIsAuto: boolean;
  plannedSavings: string | null;
  freeBudget: string | null;
  limits: BudgetLimitView[];
}

/** Mirrors BudgetController.TodayResponse. Fields are null when hasBudget is false. */
export interface TodayBudgetResponse {
  hasBudget: boolean;
  dailyAllowance: string | null;
  spentToday: string | null;
  todayLeft: string | null;
  overspentToday: boolean;
  overspentAmount: string | null;
  tomorrowAllowance: string | null;
  freeBudget: string | null;
  obligationsExceedIncome: boolean;
  forecastOverage: string | null;
  forecastPositive: boolean;
  limits: BudgetLimitView[];
}

export interface UpdateBudgetLimitRequest {
  categoryId: string;
  amount: string;
}

/** Mirrors BudgetController.UpdateBudgetRequest. fixedPayments/loanPayments:
 * null means "auto from recurring rules"; a value sets a manual override. */
export interface UpdateBudgetRequest {
  expectedIncome: string;
  fixedPayments: string | null;
  loanPayments: string | null;
  plannedSavings: string;
  limits: UpdateBudgetLimitRequest[];
}

export type RecurringKind = "REGULAR" | "LOAN" | "INCOME";
export type RecurringSchedule = "MONTHLY" | "WEEKLY" | "EVERY_N_DAYS";
export type OccurrenceStatus = "PENDING" | "PAID" | "SKIPPED";

/** Mirrors RecurringController.RuleResponse. */
export interface RecurringRule {
  id: string;
  name: string;
  kind: RecurringKind;
  amount: string;
  categoryId: string | null;
  accountId: string;
  schedule: RecurringSchedule;
  dayOfMonth: number | null;
  dayOfWeek: number | null;
  everyNDays: number | null;
  startDate: string;
  nextDueDate: string;
  autoCreate: boolean;
  remindDaysBefore: number;
  active: boolean;
}

/** Mirrors RecurringController.CreateRuleRequest. */
export interface CreateRecurringRuleRequest {
  name: string;
  kind: RecurringKind;
  amount: string;
  categoryId: string | null;
  accountId: string;
  schedule: RecurringSchedule;
  dayOfMonth?: number | null;
  dayOfWeek?: number | null;
  everyNDays?: number | null;
  startDate: string;
  autoCreate: boolean;
  remindDaysBefore: number;
}

/** Mirrors RecurringController.UpdateRuleRequest. */
export interface UpdateRecurringRuleRequest {
  name: string;
  amount: string;
  categoryId: string | null;
  accountId: string;
  schedule: RecurringSchedule;
  dayOfMonth?: number | null;
  dayOfWeek?: number | null;
  everyNDays?: number | null;
  autoCreate: boolean;
  remindDaysBefore: number;
}

/** Mirrors RecurringController.UpcomingResponse. */
export interface UpcomingOccurrence {
  occurrenceId: string;
  ruleId: string;
  ruleName: string;
  kind: RecurringKind;
  amount: string;
  accountId: string;
  dueDate: string;
  status: OccurrenceStatus;
}

/** Mirrors RecurringController.OccurrenceResponse. */
export interface RecurringOccurrenceResult {
  occurrenceId: string;
  status: OccurrenceStatus;
  transactionId: string | null;
}

export type InsightCode = "CATEGORY_UP_WEEK" | "BUDGET_80" | "FORECAST_OVER" | "DEBT_SHARE" | "DISCRETIONARY_DOWN" | "NO_ENTRIES_3_DAYS";

/** Mirrors InsightController.InsightResponse. */
export interface Insight {
  id: string;
  code: InsightCode;
  icon: string;
  text: string;
  actionable: boolean;
  createdAt: string;
}

export type AchievementCode = "STREAK_7" | "STREAK_30" | "LIMIT_WEEK" | "FULL_MONTH";

/** Mirrors HabitController.AchievementResponse. */
export interface Achievement {
  code: AchievementCode;
  unlocked: boolean;
  achievedAt: string | null;
}

/** Mirrors HabitController.HabitSummaryResponse. */
export interface HabitSummary {
  currentStreak: number;
  bestStreak: number;
  daysWithinLimitThisMonth: number;
  achievements: Achievement[];
}

export type DebtType = "BANK_LOAN" | "INSTALLMENT" | "CREDIT_CARD" | "PERSONAL_DEBT" | "OTHER";
export type DebtInterestMethod = "ANNUITY_MONTHLY" | "ACTUAL_365";
export type DebtStatusValue = "ACTIVE" | "CLOSED";

/** Mirrors DebtController.DebtResponse. */
export interface Debt {
  id: string;
  name: string;
  lender: string | null;
  type: DebtType;
  originalAmount: string;
  outstandingPrincipal: string;
  annualRate: string;
  monthlyPayment: string;
  startDate: string;
  endDate: string;
  paymentDay: number;
  interestMethod: DebtInterestMethod;
  status: DebtStatusValue;
  scheduleIsEstimated: boolean;
}

export interface CreateDebtRequest {
  name: string;
  lender?: string | null;
  type: DebtType;
  originalAmount: string;
  annualRate: string;
  interestMethod: DebtInterestMethod;
  startDate: string;
  termMonths?: number | null;
  endDate?: string | null;
  paymentDay: number;
}

export interface UpdateDebtRequest {
  name: string;
  lender?: string | null;
  annualRate: string;
  interestMethod: DebtInterestMethod;
  paymentDay: number;
}

export interface ManualScheduleRowRequest {
  dueDate: string;
  principal: string;
  interest: string;
  fee?: string | null;
}

/** Mirrors DebtController.ScheduleRowResponse. */
export interface DebtScheduleRow {
  id: string;
  seq: number;
  dueDate: string;
  principal: string;
  interest: string;
  fee: string;
  isEstimated: boolean;
  paid: boolean;
}

/** Mirrors DebtController.PaymentResponse. */
export interface DebtPayment {
  id: string;
  paidOn: string;
  principal: string;
  interest: string;
  fee: string;
  extra: boolean;
}

export interface PayDebtRequest {
  amount: string;
  paidOn: string;
  accountId: string;
  extra: boolean;
}

/** Mirrors DebtController.PaymentResultResponse. */
export interface DebtPaymentResult {
  debtId: string;
  principal: string;
  interest: string;
  fee: string;
  outstandingPrincipal: string;
  closed: boolean;
  transactionId: string;
}

/** Mirrors DebtController.ProgressResponse. */
export interface DebtProgress {
  paidPercent: string;
  monthsLeft: number;
  estimatedCloseDate: string | null;
  interestRemaining: string;
}

/** Mirrors DebtController.SimulateResponse. */
export interface DebtSimulateResult {
  newCloseDate: string;
  monthsSaved: number;
  interestSaved: string;
}

export type DebtStrategyKind = "AVALANCHE" | "SNOWBALL";

export interface DebtStrategyOutcome {
  closeDate: string;
  totalInterest: string;
}

/** Mirrors DebtController.StrategyResponse. */
export interface DebtStrategyComparison {
  avalanche: DebtStrategyOutcome;
  snowball: DebtStrategyOutcome;
  recommended: DebtStrategyKind;
}
