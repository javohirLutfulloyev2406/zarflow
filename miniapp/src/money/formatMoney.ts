import type { Language } from "../api/types";

const NON_BREAKING_SPACE = " ";

function groupThousands(digits: string): string {
  const groups = digits
    .split("")
    .reverse()
    .reduce<string[]>((acc, digit, index) => {
      if (index % 3 === 0) {
        acc.push(digit);
      } else {
        acc[acc.length - 1] = digit + acc[acc.length - 1];
      }
      return acc;
    }, []);
  return groups.reverse().join(NON_BREAKING_SPACE);
}

function unitFor(language: Language): string {
  return language === "ru" ? "сум" : "so'm";
}

/**
 * Formats a backend money string (e.g. "8500000.00", always scale-2 per
 * MoneyJsonModule) exactly like the backend's own MoneyFormatter.java:
 * space-grouped thousands, comma decimal separator, ",00" hidden when the
 * amount is a whole number. Pure string splitting + regex-free digit
 * grouping - this function never converts the amount to a JS number, so
 * there is no floating-point precision risk for large sums.
 */
export function formatMoney(amount: string, language: Language): string {
  const trimmed = amount.trim();
  const negative = trimmed.startsWith("-");
  const unsigned = negative ? trimmed.slice(1) : trimmed;
  const [integerPartRaw = "", fractionRaw = ""] = unsigned.split(".");
  const integerPart =
    integerPartRaw === "" ? "0" : integerPartRaw.replace(/^0+(?=\d)/, "");
  const fraction = (fractionRaw + "00").slice(0, 2);

  // Mirrors MoneyFormatter.java's own quirk: truncating toward zero at the
  // whole-number level means a magnitude under 1 (e.g. "-0.50") has no sign
  // on the whole part, same as Java's BigDecimal#setScale(0, DOWN) would.
  const showSign = negative && /[1-9]/.test(integerPart);
  const centsSuffix = fraction === "00" ? "" : `,${fraction}`;

  return `${showSign ? "-" : ""}${groupThousands(integerPart)}${centsSuffix} ${unitFor(language)}`;
}
