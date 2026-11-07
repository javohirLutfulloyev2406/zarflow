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
}

export interface CreateTransactionRequest {
  accountId: string;
  categoryId: string;
  type: "INCOME" | "EXPENSE";
  amount: string;
  necessity?: Necessity | null;
  transactionDate?: string | null;
  description?: string | null;
}

export interface UpdateTransactionRequest {
  accountId: string;
  categoryId: string;
  amount: string;
  necessity?: Necessity | null;
  transactionDate?: string | null;
  description?: string | null;
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
  month: { income: string; expense: string; net: string };
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
}

/** Mirrors NotificationSettingsController.UpdateSettingsRequest. */
export interface UpdateNotificationSettingsRequest {
  dailyReportEnabled: boolean;
  dailyReportTime: string;
}
