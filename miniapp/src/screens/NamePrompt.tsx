import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";

import { ApiError } from "../api/problemDetail";
import { updateDisplayName } from "../api/users";
import { useAuth } from "../auth/AuthProvider";
import { useDictionary } from "../i18n/dictionary";

export function NamePrompt() {
  const dictionary = useDictionary();
  const { setDisplayName } = useAuth();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (displayName: string) => updateDisplayName({ displayName }),
    onSuccess: (profile) => {
      setDisplayName(profile.displayName ?? name.trim());
    },
    onError: (err: unknown) => {
      setError(
        err instanceof ApiError ? (err.problem.detail ?? dictionary.error.generic) : dictionary.error.generic,
      );
    },
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      return;
    }
    setError(null);
    mutation.mutate(trimmed);
  }

  return (
    <div className="flex h-full flex-col items-center justify-center gap-4 bg-navy-900 px-6 text-center text-ivory-50">
      <p className="font-heading text-lg font-semibold">{dictionary.namePrompt.title}</p>
      <form onSubmit={handleSubmit} className="flex w-full max-w-xs flex-col gap-3">
        <input
          autoFocus
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder={dictionary.namePrompt.placeholder}
          maxLength={50}
          className="w-full rounded-lg bg-navy-800 px-3 py-2 text-center text-sm text-ivory-50"
        />
        {error ? <p className="text-xs text-warn-400">{error}</p> : null}
        <button
          type="submit"
          disabled={mutation.isPending || name.trim() === ""}
          className="rounded-full bg-gold-400 px-6 py-2 font-semibold text-navy-900 disabled:opacity-50"
        >
          {dictionary.namePrompt.save}
        </button>
      </form>
    </div>
  );
}
