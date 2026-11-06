export function PlaceholderScreen({ text }: { text: string }) {
  return (
    <div className="flex h-full flex-col items-center justify-center gap-3 p-6 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-700">
        <span className="font-heading text-lg font-bold text-gold-400">Z</span>
      </div>
      <p className="text-sm text-slate-400">{text}</p>
    </div>
  );
}
