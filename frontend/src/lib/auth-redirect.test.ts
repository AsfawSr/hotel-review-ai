import { describe, expect, it } from "vitest";
import { loginRedirectFor } from "./auth-redirect";

describe("loginRedirectFor", () => {
  it("redirects protected pages without a session in API mode", () => {
    expect(loginRedirectFor("/reviews/12", "?tab=x", false, "api")).toBe("/login?next=%2Freviews%2F12%3Ftab%3Dx");
    expect(loginRedirectFor("/dashboard", "", false, "api")).toBe("/login?next=%2Fdashboard");
  });

  it("lets requests with a session through", () => {
    expect(loginRedirectFor("/dashboard", "", true, "api")).toBeNull();
  });

  it("never redirects in mock mode (session lives in the browser)", () => {
    expect(loginRedirectFor("/dashboard", "", false, "mock")).toBeNull();
  });

  it("ignores public and look-alike paths", () => {
    expect(loginRedirectFor("/login", "", false, "api")).toBeNull();
    expect(loginRedirectFor("/", "", false, "api")).toBeNull();
    expect(loginRedirectFor("/reviewsx", "", false, "api")).toBeNull();
  });
});
