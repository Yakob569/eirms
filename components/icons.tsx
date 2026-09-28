type IconProps = { size?: number; stroke?: string; strokeWidth?: number };

const base = (size: number, stroke: string, strokeWidth: number) => ({
  width: size,
  height: size,
  viewBox: "0 0 24 24",
  fill: "none",
  stroke,
  strokeWidth,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
  "aria-hidden": true,
});

export function ShieldCheck({ size = 18, stroke = "currentColor", strokeWidth = 2 }: IconProps) {
  return (
    <svg {...base(size, stroke, strokeWidth)}>
      <path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  );
}

export function ArrowRight({ size = 18, stroke = "currentColor", strokeWidth = 2 }: IconProps) {
  return (
    <svg {...base(size, stroke, strokeWidth)}>
      <path d="M5 12h14M13 6l6 6-6 6" />
    </svg>
  );
}

export function ArrowLeft({ size = 16, stroke = "currentColor", strokeWidth = 2 }: IconProps) {
  return (
    <svg {...base(size, stroke, strokeWidth)}>
      <path d="M19 12H5M11 18l-6-6 6-6" />
    </svg>
  );
}

export function Check({ size = 12, stroke = "currentColor", strokeWidth = 2.5 }: IconProps) {
  return (
    <svg {...base(size, stroke, strokeWidth)}>
      <path d="M20 6 9 17l-5-5" />
    </svg>
  );
}

export function CheckCircle({ size = 18, stroke = "currentColor", strokeWidth = 2.2 }: IconProps) {
  return (
    <svg {...base(size, stroke, strokeWidth)}>
      <circle cx="12" cy="12" r="10" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  );
}

export function Globe({ size = 14, stroke = "currentColor", strokeWidth = 2 }: IconProps) {
  return (
    <svg {...base(size, stroke, strokeWidth)}>
      <circle cx="12" cy="12" r="10" />
      <path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20" />
      <path d="M2 12h20" />
    </svg>
  );
}

export function Mail({ size = 20, stroke = "currentColor", strokeWidth = 2 }: IconProps) {
  return (
    <svg {...base(size, stroke, strokeWidth)}>
      <rect width="20" height="16" x="2" y="4" rx="2" />
      <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
    </svg>
  );
}

export function Phone({ size = 20, stroke = "currentColor", strokeWidth = 2 }: IconProps) {
  return (
    <svg {...base(size, stroke, strokeWidth)}>
      <rect width="14" height="20" x="5" y="2" rx="2" />
      <path d="M12 18h.01" />
    </svg>
  );
}
