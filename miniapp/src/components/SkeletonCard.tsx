export function SkeletonCard({ label }: { label: string }) {
  return (
    <div className="rounded-2xl bg-navy-800 p-4">
      <div className="mb-2 h-3 w-1/3 animate-pulse rounded-full bg-slate-400/30" />
      <div className="h-6 w-2/3 animate-pulse rounded-full bg-slate-400/20" />
      <p className="mt-2 text-xs text-slate-400">{label}</p>
    </div>
  );
}
