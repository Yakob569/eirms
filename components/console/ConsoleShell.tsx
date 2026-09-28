"use client";

import { ConsoleLoader } from "./ConsoleLoader";
import { useConsole } from "./ConsoleProvider";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";
import s from "./shell.module.css";

export function ConsoleShell({ children }: { children: React.ReactNode }) {
  const { busy } = useConsole();
  return (
    <div className={s.shell}>
      <Sidebar />
      <div className={s.column}>
        <Topbar />
        <div className={s.scroll} id="console-scroll">
          {children}
        </div>
        {busy && <ConsoleLoader message={busy.message} detail={busy.detail} />}
      </div>
    </div>
  );
}
