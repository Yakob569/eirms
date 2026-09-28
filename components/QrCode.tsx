import styles from "./QrCode.module.css";

const SIZE = 13;

// Decorative 13×13 QR-style grid: three finder squares plus a seeded pattern.
// Deterministic so server and client render the same cells.
function buildCells() {
  const cells: boolean[] = [];
  let seed = 7;
  const inFinder = (r: number, c: number) => r >= 0 && r < 4 && c >= 0 && c < 4;
  for (let r = 0; r < SIZE; r++) {
    for (let c = 0; c < SIZE; c++) {
      let on: boolean;
      if (inFinder(r, c) || inFinder(r, c - 9) || inFinder(r - 9, c)) {
        const rr = r >= 9 ? r - 9 : r;
        const cc = c >= 9 ? c - 9 : c;
        on = rr === 0 || rr === 3 || cc === 0 || cc === 3 || (rr > 0 && rr < 3 && cc > 0 && cc < 3);
      } else {
        seed = (seed * 9301 + 49297) % 233280;
        on = seed / 233280 > 0.52;
      }
      cells.push(on);
    }
  }
  return cells;
}

const CELLS = buildCells();

export function QrCode({ size = 72, bordered = true }: { size?: number; bordered?: boolean }) {
  return (
    <div
      className={styles.qr}
      data-bordered={bordered}
      style={{ width: size, height: size, padding: size >= 70 ? 5 : 4 }}
      aria-hidden
    >
      {CELLS.map((on, i) => (
        <div key={i} style={{ background: on ? "var(--ink)" : "transparent" }} />
      ))}
      <div className={styles.scan} />
    </div>
  );
}
