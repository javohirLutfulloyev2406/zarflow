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

/** Mirrors UserController.UpdateDisplayNameRequest. */
export interface UpdateDisplayNameRequest {
  displayName: string;
}
