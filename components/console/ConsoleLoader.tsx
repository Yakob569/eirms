import { Icon } from "./Icon";
import s from "./shell.module.css";

/** See-through loader shown over the content column while a MoR call runs. */
export function ConsoleLoader({ message, detail }: { message: string; detail: string }) {
  return (
    <div className={s.loader} role="status" aria-live="polite">
      <div className={s.loaderMark}>
        <div className={s.loaderSpin} />
        <div className={s.loaderCore}>
          <Icon name="shield" size={32} stroke="#fff" strokeWidth={1.8} />
        </div>
      </div>
      <div className={s.loaderText}>
        <span className={`serif ${s.loaderMessage}`}>{message}</span>
        <span className={s.loaderDetail}>{detail}</span>
      </div>
    </div>
  );
}
