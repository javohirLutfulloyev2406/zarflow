import { createHashRouter, RouterProvider } from "react-router-dom";

import { TabsLayout } from "./TabsLayout";
import { Home } from "../screens/Home";
import { History } from "../screens/History";
import { Analytics } from "../screens/Analytics";
import { Plan } from "../screens/Plan";
import { Profile } from "../screens/Profile";

const router = createHashRouter([
  {
    element: <TabsLayout />,
    children: [
      { path: "/", element: <Home /> },
      { path: "/history", element: <History /> },
      { path: "/analytics", element: <Analytics /> },
      { path: "/plan", element: <Plan /> },
    ],
  },
  { path: "/profile", element: <Profile /> },
]);

export function AppRouter() {
  return <RouterProvider router={router} />;
}
