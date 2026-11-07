import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";

import { EmptyStateCard } from "../components/EmptyStateCard";
import { SkeletonCard } from "../components/SkeletonCard";
import { useAuth } from "../auth/AuthProvider";
import { setAuthState } from "../auth/store";
import { getNotificationSettings, updateNotificationSettings } from "../api/notifications";
import { ApiError } from "../api/problemDetail";
import type { Language, NotificationSettings, UpdateSettingsRequest, UserProfile } from "../api/types";
import { getMe, updateDisplayName, updateSettings } from "../api/users";
import { useDictionary } from "../i18n/dictionary";
import { useBackButton } from "../telegram/useBackButton";
import { useTimezoneSearch } from "./useTimezoneSearch";

export function Profile() {
  useBackButton(true);
  const dictionary = useDictionary();
  const { data, isPending, isError, refetch } = useQuery({
    queryKey: ["me"],
    queryFn: getMe,
  });

  return (
    <div className="flex flex-col gap-4 p-4">
      <h1 className="font-heading text-lg font-semibold">{dictionary.profile.title}</h1>
      {isPending ? <SkeletonCard label="" /> : null}
      {isError ? <EmptyStateCard>{dictionary.error.generic}</EmptyStateCard> : null}
      {data ? <ProfileForm profile={data} onReload={refetch} /> : null}
    </div>
  );
}

function ProfileForm({
  profile,
  onReload,
}: {
  profile: UserProfile;
  onReload: () => void;
}) {
  const dictionary = useDictionary();
  const [language, setLanguage] = useState<Language>(profile.language);
  const [timezone, setTimezone] = useState(profile.timezone);
  const [income, setIncome] = useState(profile.expectedMonthlyIncome ?? "");
  const { query, setQuery, matches } = useTimezoneSearch(profile.timezone);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (body: UpdateSettingsRequest) => updateSettings(body),
    onSuccess: (updated) => {
      setAuthState({ language: updated.language });
      setFieldErrors({});
      setMessage(dictionary.profile.saved);
    },
    onError: (err: unknown) => {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setMessage(dictionary.profile.conflict);
          onReload();
          return;
        }
        const errors: Record<string, string> = {};
        for (const fieldError of err.problem.errors ?? []) {
          errors[fieldError.field] = fieldError.message;
        }
        setFieldErrors(errors);
        setMessage(err.problem.detail ?? null);
        return;
      }
      setMessage(dictionary.error.generic);
    },
  });

  function handleSave() {
    setMessage(null);
    mutation.mutate({
      language,
      timezone,
      expectedMonthlyIncome: income.trim() === "" ? null : income.trim(),
    });
  }

  return (
    <div className="flex flex-col gap-4">
      <DisplayNameField initialValue={profile.displayName ?? ""} />

      <div>
        <p className="text-xs text-slate-400">{dictionary.profile.firstName}</p>
        <p className="text-sm">{profile.firstName}</p>
      </div>

      <div>
        <p className="mb-1 text-xs text-slate-400">{dictionary.profile.language}</p>
        <div className="flex gap-2">
          {(["uz", "ru"] as const).map((lang) => (
            <button
              key={lang}
              type="button"
              onClick={() => setLanguage(lang)}
              className={`rounded-full px-4 py-1 text-sm ${
                language === lang ? "bg-emerald-700 text-ivory-50" : "bg-navy-800 text-slate-400"
              }`}
            >
              {lang.toUpperCase()}
            </button>
          ))}
        </div>
      </div>

      <div>
        <label className="mb-1 block text-xs text-slate-400" htmlFor="timezone-search">
          {dictionary.profile.timezone}
        </label>
        <input
          id="timezone-search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder={dictionary.profile.timezoneSearchPlaceholder}
          className="w-full rounded-lg bg-navy-800 px-3 py-2 text-sm text-ivory-50"
        />
        <ul className="mt-1 max-h-32 overflow-y-auto rounded-lg bg-navy-800">
          {matches.map((zone) => (
            <li key={zone}>
              <button
                type="button"
                onClick={() => {
                  setTimezone(zone);
                  setQuery(zone);
                }}
                className={`w-full px-3 py-1 text-left text-sm ${
                  zone === timezone ? "text-gold-400" : "text-slate-400"
                }`}
              >
                {zone}
              </button>
            </li>
          ))}
        </ul>
        {fieldErrors.timezone ? (
          <p className="text-xs text-warn-400">{fieldErrors.timezone}</p>
        ) : null}
      </div>

      <div>
        <label className="mb-1 block text-xs text-slate-400" htmlFor="income">
          {dictionary.profile.expectedMonthlyIncome}
        </label>
        <input
          id="income"
          type="text"
          inputMode="decimal"
          value={income}
          onChange={(event) => setIncome(event.target.value)}
          placeholder={dictionary.profile.incomePlaceholder}
          className="w-full rounded-lg bg-navy-800 px-3 py-2 text-sm text-ivory-50"
        />
        {fieldErrors.expectedMonthlyIncome ? (
          <p className="text-xs text-warn-400">{fieldErrors.expectedMonthlyIncome}</p>
        ) : null}
      </div>

      {message ? <p className="text-sm text-success-400">{message}</p> : null}

      <button
        type="button"
        onClick={handleSave}
        disabled={mutation.isPending}
        className="rounded-full bg-emerald-700 py-3 text-sm font-medium text-ivory-50 disabled:opacity-50"
      >
        {dictionary.profile.save}
      </button>

      <DailyReportField />
    </div>
  );
}

function DailyReportField() {
  const { data } = useQuery({ queryKey: ["notificationSettings"], queryFn: getNotificationSettings });

  if (!data) {
    return null;
  }
  return <DailyReportForm initial={data} />;
}

function DailyReportForm({ initial }: { initial: NotificationSettings }) {
  const dictionary = useDictionary();
  const [enabled, setEnabled] = useState(initial.dailyReportEnabled);
  const [time, setTime] = useState(initial.dailyReportTime);
  const [message, setMessage] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (body: { dailyReportEnabled: boolean; dailyReportTime: string }) => updateNotificationSettings(body),
    onSuccess: () => setMessage(dictionary.profile.saved),
    onError: (err: unknown) => {
      setMessage(err instanceof ApiError ? (err.problem.detail ?? dictionary.error.generic) : dictionary.error.generic);
    },
  });

  function save(nextEnabled: boolean, nextTime: string) {
    setMessage(null);
    mutation.mutate({ dailyReportEnabled: nextEnabled, dailyReportTime: nextTime });
  }

  return (
    <div className="flex flex-col gap-3 rounded-2xl bg-navy-800 p-4">
      <p className="text-sm font-medium">{dictionary.notifications.dailyReportTitle}</p>
      <div className="flex items-center justify-between">
        <span className="text-xs text-slate-400">{dictionary.notifications.dailyReportEnabledLabel}</span>
        <button
          type="button"
          onClick={() => {
            const next = !enabled;
            setEnabled(next);
            save(next, time);
          }}
          className={`rounded-full px-4 py-1 text-xs ${enabled ? "bg-emerald-700 text-ivory-50" : "bg-navy-900 text-slate-400"}`}
        >
          {enabled ? "ON" : "OFF"}
        </button>
      </div>
      <div className="flex items-center justify-between">
        <label className="text-xs text-slate-400" htmlFor="daily-report-time">
          {dictionary.notifications.dailyReportTimeLabel}
        </label>
        <input
          id="daily-report-time"
          type="time"
          value={time}
          onChange={(event) => {
            const next = event.target.value;
            setTime(next);
            save(enabled, next);
          }}
          className="rounded-lg bg-navy-900 px-3 py-1 text-sm text-ivory-50"
        />
      </div>
      {message ? <p className="text-xs text-success-400">{message}</p> : null}
    </div>
  );
}

function DisplayNameField({ initialValue }: { initialValue: string }) {
  const dictionary = useDictionary();
  const { setDisplayName } = useAuth();
  const [value, setValue] = useState(initialValue);
  const [message, setMessage] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: (displayName: string) => updateDisplayName({ displayName }),
    onSuccess: (updated) => {
      setDisplayName(updated.displayName ?? value.trim());
      setMessage(dictionary.profile.saved);
    },
    onError: (err: unknown) => {
      setMessage(err instanceof ApiError ? (err.problem.detail ?? dictionary.error.generic) : dictionary.error.generic);
    },
  });

  return (
    <div>
      <label className="mb-1 block text-xs text-slate-400" htmlFor="display-name">
        {dictionary.profile.displayName}
      </label>
      <div className="flex gap-2">
        <input
          id="display-name"
          value={value}
          onChange={(event) => setValue(event.target.value)}
          placeholder={dictionary.profile.displayNamePlaceholder}
          maxLength={50}
          className="w-full rounded-lg bg-navy-800 px-3 py-2 text-sm text-ivory-50"
        />
        <button
          type="button"
          onClick={() => {
            setMessage(null);
            const trimmed = value.trim();
            if (trimmed) {
              mutation.mutate(trimmed);
            }
          }}
          disabled={mutation.isPending || value.trim() === ""}
          className="rounded-lg bg-emerald-700 px-4 py-2 text-sm font-medium text-ivory-50 disabled:opacity-50"
        >
          {dictionary.profile.save}
        </button>
      </div>
      {message ? <p className="mt-1 text-xs text-success-400">{message}</p> : null}
    </div>
  );
}
