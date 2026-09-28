import { ShieldCheck } from "../icons";
import styles from "./BrandLoader.module.css";

export function BrandLoader({ message, detail, durationMs }: { message: string; detail: string; durationMs: number }) {
  return (
    <div className={styles.overlay} role="status" aria-live="polite">
      <div className={styles.mark}>
        <div className={styles.ring} />
        <div className={styles.ring} style={{ animationDelay: "0.7s" }} />
        <div className={styles.core}>
          <ShieldCheck size={38} stroke="#fff" strokeWidth={1.8} />
        </div>
      </div>
      <div className={styles.text}>
        <div className={`serif ${styles.message}`}>{message}</div>
        <div className={styles.detail}>{detail}</div>
      </div>
      <div className={styles.track}>
        <div className={styles.bar} style={{ animationDuration: `${durationMs}ms` }} />
      </div>
      <div className={`mono ${styles.footer}`}>EIRSPAY · SECURED BY EIRS</div>
    </div>
  );
}
