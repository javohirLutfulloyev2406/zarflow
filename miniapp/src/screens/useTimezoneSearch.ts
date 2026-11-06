import { useMemo, useState } from "react";

const ALL_TIMEZONES: readonly string[] =
  typeof Intl.supportedValuesOf === "function" ? Intl.supportedValuesOf("timeZone") : ["Asia/Tashkent"];

/** Simple client-side search over the real IANA list — no bundled data file. */
export function useTimezoneSearch(initial: string) {
  const [query, setQuery] = useState(initial);

  const matches = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (needle === "") {
      return ALL_TIMEZONES.slice(0, 8);
    }
    return ALL_TIMEZONES.filter((zone) => zone.toLowerCase().includes(needle)).slice(0, 8);
  }, [query]);

  return { query, setQuery, matches };
}
