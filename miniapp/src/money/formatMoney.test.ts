import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

import { formatMoney } from "./formatMoney";

const NBSP = " ";

describe("formatMoney", () => {
  it("formats a whole uz amount with space-grouped thousands and hides ,00", () => {
    expect(formatMoney("8500000.00", "uz")).toBe(`8${NBSP}500${NBSP}000 so'm`);
  });

  it("formats a whole ru amount with the ru unit", () => {
    expect(formatMoney("8500000.00", "ru")).toBe(`8${NBSP}500${NBSP}000 сум`);
  });

  it("shows the comma decimal part when the amount is not whole", () => {
    expect(formatMoney("8500000.50", "uz")).toBe(`8${NBSP}500${NBSP}000,50 so'm`);
  });

  it("formats large values correctly", () => {
    expect(formatMoney("123456789012.34", "uz")).toBe(
      `123${NBSP}456${NBSP}789${NBSP}012,34 so'm`,
    );
  });

  it("places the negative sign in front of a negative amount", () => {
    expect(formatMoney("-150000.50", "uz")).toBe(`-150${NBSP}000,50 so'm`);
  });

  it("formats zero as a plain whole amount", () => {
    expect(formatMoney("0.00", "uz")).toBe("0 so'm");
  });

  it("formats small amounts under 1000 without a grouping separator", () => {
    expect(formatMoney("500.00", "uz")).toBe("500 so'm");
  });

  it("never calls Number() or parseFloat() on the amount (source-level check)", () => {
    const source = readFileSync(resolve(process.cwd(), "src/money/formatMoney.ts"), "utf8");
    expect(source).not.toMatch(/\bNumber\(/);
    expect(source).not.toMatch(/\bparseFloat\(/);
    expect(source).not.toMatch(/\bparseInt\(/);
  });
});
