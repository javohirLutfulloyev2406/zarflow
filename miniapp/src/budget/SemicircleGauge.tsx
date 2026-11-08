const RADIUS = 36;
const CIRCUMFERENCE = Math.PI * RADIUS;
const TRACK_COLOR = "#15243a"; // navy-800
const USED_COLOR = "#3ccb95"; // success-400
const OVER_COLOR = "#e8a85a"; // warn-400

/** usedFraction in [0,1]: how much of today's allowance has been spent. Fills left-to-right over the top. */
export function SemicircleGauge({ usedFraction, overspent }: { usedFraction: number; overspent: boolean }) {
  const clamped = Math.max(0, Math.min(1, usedFraction));
  const dashOffset = CIRCUMFERENCE * (1 - clamped);
  const color = overspent ? OVER_COLOR : USED_COLOR;

  return (
    <svg width="80" height="48" viewBox="0 0 80 48" aria-hidden="true">
      <path d="M4 44 A36 36 0 0 1 76 44" fill="none" stroke={TRACK_COLOR} strokeWidth="8" strokeLinecap="round" />
      <path
        d="M4 44 A36 36 0 0 1 76 44"
        fill="none"
        stroke={color}
        strokeWidth="8"
        strokeLinecap="round"
        strokeDasharray={`${CIRCUMFERENCE} ${CIRCUMFERENCE}`}
        strokeDashoffset={dashOffset}
      />
    </svg>
  );
}
