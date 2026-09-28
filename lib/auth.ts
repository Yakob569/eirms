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

export async function signIn(email: string, password: string): Promise<void> {
  await delay(500);
  if (!email || !password) throw new AuthError("Enter your email and password.");
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
