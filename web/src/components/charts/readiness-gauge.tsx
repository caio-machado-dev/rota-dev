import { readinessBand } from "@/lib/format";

/**
 * Gauge radial do índice de prontidão (0 a 100). O arco anima o preenchimento (150 a 250 ms, desligado por
 * `prefers-reduced-motion`) e o número fica em texto grande no centro, então a cor nunca é a única informação.
 */
export function ReadinessGauge({ score, size = 176 }: { score: number; size?: number }) {
  const stroke = 12;
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const clamped = Math.min(100, Math.max(0, score));
  return (
    <div className="relative inline-grid place-items-center" style={{ width: size, height: size }} role="img" aria-label={`Prontidão ${score} de 100, ${readinessBand(score)}`}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="var(--muted)" strokeWidth={stroke} />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="var(--brand)"
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={circumference * (1 - clamped / 100)}
          className="transition-[stroke-dashoffset] duration-[250ms] ease-out-soft"
        />
      </svg>
      <div className="absolute text-center">
        <p className="num text-display leading-none font-semibold">{score}</p>
        <p className="mt-1 text-small text-muted-foreground">{readinessBand(score)}</p>
      </div>
    </div>
  );
}
