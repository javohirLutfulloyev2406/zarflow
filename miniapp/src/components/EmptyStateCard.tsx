import type { ReactNode } from "react";

export function EmptyStateCard({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-2xl bg-navy-800 p-4 text-sm text-ivory-50">{children}</div>
  );
}
