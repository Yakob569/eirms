import { AuthFlowProvider } from "@/components/auth/AuthFlow";
import { AuthShell } from "@/components/auth/AuthShell";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthFlowProvider>
      <AuthShell>{children}</AuthShell>
    </AuthFlowProvider>
  );
}
