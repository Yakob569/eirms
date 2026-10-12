import { SettingsLayout } from "@/components/console/settings/SettingsLayout";

// Settings sub-navigation (Account / Organization / Access) beside every Settings page.
export default function Layout({ children }: { children: React.ReactNode }) {
  return <SettingsLayout>{children}</SettingsLayout>;
}
