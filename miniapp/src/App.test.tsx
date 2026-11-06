import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { App } from "./App";

describe("App outside Telegram", () => {
  it("renders OutsideTelegramScreen instead of crashing when isTMA() is false", () => {
    render(<App />);

    expect(screen.getByText("ZarFlow")).toBeInTheDocument();
    expect(screen.getByText("Ilovani Telegram orqali oching.")).toBeInTheDocument();
  });
});
