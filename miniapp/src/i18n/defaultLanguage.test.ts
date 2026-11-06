import { describe, expect, it } from "vitest";

import { defaultLanguageFor } from "./defaultLanguage";

describe("defaultLanguageFor", () => {
  it("maps a ru-prefixed language code to ru", () => {
    expect(defaultLanguageFor("ru")).toBe("ru");
    expect(defaultLanguageFor("ru-RU")).toBe("ru");
  });

  it("maps anything else to uz", () => {
    expect(defaultLanguageFor("uz")).toBe("uz");
    expect(defaultLanguageFor("en")).toBe("uz");
    expect(defaultLanguageFor(undefined)).toBe("uz");
  });
});
