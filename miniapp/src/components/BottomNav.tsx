import { NavLink } from "react-router-dom";

import { useDictionary } from "../i18n/dictionary";
import { hapticSelectionChanged } from "../telegram/haptics";
import { AnalyticsIcon, HistoryIcon, HomeIcon, PlanIcon } from "./icons";

function tabClass(isActive: boolean): string {
  return `flex flex-1 flex-col items-center gap-1 py-2 text-xs ${
    isActive ? "text-gold-400" : "text-slate-400"
  }`;
}

export function BottomNav({ onAddClick }: { onAddClick: () => void }) {
  const dictionary = useDictionary();

  return (
    <nav className="flex items-center border-t border-navy-800 bg-navy-900 px-2 pb-[env(safe-area-inset-bottom)]">
      <NavLink to="/" end className={({ isActive }) => tabClass(isActive)} onClick={hapticSelectionChanged}>
        <HomeIcon />
        {dictionary.nav.home}
      </NavLink>
      <NavLink to="/history" className={({ isActive }) => tabClass(isActive)} onClick={hapticSelectionChanged}>
        <HistoryIcon />
        {dictionary.nav.history}
      </NavLink>
      <div className="flex flex-1 flex-col items-center">
        <button
          type="button"
          onClick={() => {
            hapticSelectionChanged();
            onAddClick();
          }}
          aria-label="+"
          className="-mt-6 flex h-12 w-12 items-center justify-center rounded-full bg-gold-400 text-2xl font-bold text-navy-900 shadow-lg"
        >
          +
        </button>
      </div>
      <NavLink to="/analytics" className={({ isActive }) => tabClass(isActive)} onClick={hapticSelectionChanged}>
        <AnalyticsIcon />
        {dictionary.nav.analytics}
      </NavLink>
      <NavLink to="/plan" className={({ isActive }) => tabClass(isActive)} onClick={hapticSelectionChanged}>
        <PlanIcon />
        {dictionary.nav.plan}
      </NavLink>
    </nav>
  );
}
