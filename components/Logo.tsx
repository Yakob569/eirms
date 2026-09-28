import Link from "next/link";
import { ShieldCheck } from "./icons";
import styles from "./Logo.module.css";

export function Logo({ size = "md" }: { size?: "sm" | "md" }) {
  const box = size === "sm" ? 30 : 34;
  return (
    <Link href="/" className={styles.logo} data-size={size}>
      <span className={styles.mark} style={{ width: box, height: box }}>
        <ShieldCheck size={size === "sm" ? 16 : 18} />
      </span>
      <span className={styles.word}>EIRSPay</span>
    </Link>
  );
}
