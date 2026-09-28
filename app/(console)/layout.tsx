import { ConsoleProvider } from "@/components/console/ConsoleProvider";
import { ConsoleShell } from "@/components/console/ConsoleShell";

export default function ConsoleLayout({ children }: { children: React.ReactNode }) {
  return (
    <ConsoleProvider>
      <ConsoleShell>{children}</ConsoleShell>
    </ConsoleProvider>
  );
}
