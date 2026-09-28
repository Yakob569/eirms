// Auth API boundary. These are mocks so the flow is clickable end to end —
// swap each body for a call to the real backend; the screens only depend on
// the signatures below.

export type VerifyVia = "email" | "sms";

export type SignupData = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string; // national number without the +251 prefix
  password: string;
};

export class AuthError extends Error {}

const delay = (ms: number) => new Promise((r) => setTimeout(r, ms));

export type Session = {
  name: string;
  merchant: string;
  /** True when the account signed in with a temporary password and must set its own. */
  mustChangePassword: boolean;
};

export async function signIn(email: string, password: string): Promise<Session> {
  await delay(500);
  if (!email || !password) throw new AuthError("Enter your email and password.");
  // Mock: any password containing "temp" behaves like a temporary one from an invite.
  return { ...DEMO_SESSION, mustChangePassword: /temp/i.test(password) };
}

export async function signOut(): Promise<void> {
  await delay(200);
}

export async function requestPasswordReset(email: string): Promise<void> {
  await delay(500);
  // The real endpoint should answer the same way whether or not the account exists.
  void email;
}

/** Checks the token from the emailed link and returns the account's email. */
export async function verifyResetToken(token: string): Promise<{ email: string }> {
  await delay(400);
  if (!token) throw new AuthError("This reset link is invalid or has expired.");
  return { email: DEMO_SESSION_EMAIL };
}

export async function resetPassword(token: string, password: string): Promise<void> {
  await delay(500);
  if (!token) throw new AuthError("This reset link is invalid or has expired.");
  void password;
}

export async function changePassword(current: string, next: string): Promise<void> {
  await delay(500);
  if (current === next) throw new AuthError("New password must differ from the temporary one.");
}

/** Minutes a password reset link stays valid. */
export const RESET_TTL_MINUTES = 30;

export const PASSWORD_RULES: { label: string; test: (pw: string) => boolean }[] = [
  { label: "8+ characters", test: (pw) => pw.length >= 8 },
  { label: "Upper & lowercase", test: (pw) => /[a-z]/.test(pw) && /[A-Z]/.test(pw) },
  { label: "A number", test: (pw) => /\d/.test(pw) },
  { label: "A symbol", test: (pw) => /[^A-Za-z0-9]/.test(pw) },
];

/** Number of PASSWORD_RULES met (0–4). */
export function passwordScore(pw: string): number {
  return PASSWORD_RULES.filter((r) => r.test(pw)).length;
}

/** Accepted: at least 8 characters and 3 of the 4 rules. */
export function passwordAcceptable(pw: string): boolean {
  return pw.length >= 8 && passwordScore(pw) >= 3;
}

export async function signUp(data: SignupData): Promise<void> {
  await delay(500);
  void data;
}

export async function sendCode(via: VerifyVia, destination: string): Promise<void> {
  await delay(500);
  void via;
  void destination;
}

export async function verifyCode(code: string): Promise<void> {
  await delay(500);
  if (!/^\d{6}$/.test(code)) throw new AuthError("Enter the 6-digit code.");
}

/** Seconds a verification code stays valid. */
export const CODE_TTL_SECONDS = 900;

/** Sample account shown if someone opens a verify screen without signing up first. */
export const DEMO_ACCOUNT = { firstName: "Yishak", lastName: "Girma", email: "yishak@gmail.com", phone: "921322273" };

/** Sample signed-in user returned by the mocked signIn / verifyResetToken. */
const DEMO_SESSION: Session = { name: "Yakob Girma", merchant: "Addispay Financial Tech.", mustChangePassword: false };
const DEMO_SESSION_EMAIL = "yakob@addispay.et";

export function maskEmail(email: string): string {
  const [user, domain] = email.split("@");
  if (!user || !domain) return email;
  return `${user.slice(0, 2)}••••@${domain}`;
}

export function maskPhone(phone: string): string {
  const digits = phone.replace(/\D/g, "").replace(/^(251|0)/, "");
  if (digits.length < 6) return `+251 ${digits}`;
  return `+251 ${digits.slice(0, 2)} ••• ${digits.slice(-4)}`;
}
