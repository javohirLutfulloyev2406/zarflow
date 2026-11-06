import { useBackButton } from "../telegram/useBackButton";

export function Profile() {
  useBackButton(true);

  return (
    <div className="p-4">
      <h1 className="font-heading text-lg font-semibold">Profile</h1>
    </div>
  );
}
