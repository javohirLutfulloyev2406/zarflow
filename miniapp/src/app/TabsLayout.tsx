import { useState } from "react";
import { Outlet } from "react-router-dom";

import { BottomNav } from "../components/BottomNav";
import { QuickAddSheet } from "../screens/QuickAddSheet";
import { useBackButton } from "../telegram/useBackButton";

export function TabsLayout() {
  useBackButton(false);
  const [addOpen, setAddOpen] = useState(false);

  return (
    <div className="flex h-full flex-col">
      <div className="flex-1 overflow-y-auto">
        <Outlet />
      </div>
      <BottomNav onAddClick={() => setAddOpen(true)} />
      {addOpen ? <QuickAddSheet onClose={() => setAddOpen(false)} /> : null}
    </div>
  );
}
