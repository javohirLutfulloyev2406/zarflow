import { SkeletonCard } from "../components/SkeletonCard";

export function Splash() {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-6 bg-navy-900 p-6">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-emerald-700">
        <span className="font-heading text-2xl font-bold text-gold-400">Z</span>
      </div>
      <div className="w-full max-w-xs space-y-3">
        <SkeletonCard label="" />
        <SkeletonCard label="" />
      </div>
    </div>
  );
}
