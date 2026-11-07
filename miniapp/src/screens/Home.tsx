import { Link } from "react-router-dom";

import { EmptyStateCard } from "../components/EmptyStateCard";
import { SkeletonCard } from "../components/SkeletonCard";
import { useAuth } from "../auth/AuthProvider";
import { useDictionary } from "../i18n/dictionary";

export function Home() {
  const dictionary = useDictionary();
  const { user } = useAuth();
  const name = user?.displayName ?? user?.firstName ?? "";

  return (
    <div className="flex flex-col gap-4 p-4">
      <header className="flex items-center justify-between">
        <h1 className="font-heading text-lg font-semibold">{dictionary.greeting(name)}</h1>
        <Link
          to="/profile"
          aria-label="Profile"
          className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-700"
        >
          <span className="font-heading text-sm font-bold text-gold-400">
            {(name || "Z").charAt(0).toUpperCase()}
          </span>
        </Link>
      </header>

      <EmptyStateCard>{dictionary.home.emptyState}</EmptyStateCard>

      <SkeletonCard label={`${dictionary.home.todayLimitLabel} — ${dictionary.home.comingSoon}`} />
      <SkeletonCard label={`${dictionary.home.balanceLabel} — ${dictionary.home.comingSoon}`} />
    </div>
  );
}
