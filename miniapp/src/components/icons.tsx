import type { SVGProps } from "react";

/** Shared brand icon style per docs/brand.md: 1.8px stroke, rounded caps/joins. */
function Icon(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      width={22}
      height={22}
      {...props}
    />
  );
}

export function HomeIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <Icon {...props}>
      <path d="M4 11.5 12 4l8 7.5" />
      <path d="M6 10v9h12v-9" />
    </Icon>
  );
}

export function HistoryIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <Icon {...props}>
      <circle cx="12" cy="12" r="8" />
      <path d="M12 8v4l3 2" />
    </Icon>
  );
}

export function AnalyticsIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <Icon {...props}>
      <path d="M5 19V9" />
      <path d="M12 19V5" />
      <path d="M19 19v-6" />
    </Icon>
  );
}

export function PlanIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <Icon {...props}>
      <rect x="5" y="4" width="14" height="16" rx="2" />
      <path d="M8 9h8M8 13h8M8 17h5" />
    </Icon>
  );
}
