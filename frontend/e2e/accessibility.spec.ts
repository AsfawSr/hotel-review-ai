import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";

async function expectNoSeriousViolations(page: Page) {
  const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  const serious = results.violations
    .filter((v) => v.impact === "serious" || v.impact === "critical")
    .map((v) => `${v.id}: ${v.help} (${v.nodes.map((n) => n.target.join(" ")).slice(0, 3).join(", ")})`);
  expect(serious, serious.join("\n")).toEqual([]);
}

test("public pages are accessible", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await expectNoSeriousViolations(page);

  await page.goto("/login");
  await expect(page.getByRole("button", { name: "Sign in" })).toBeVisible();
  await expectNoSeriousViolations(page);
});

test("app pages are accessible", async ({ page }) => {
  await page.goto("/login");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByText("Total reviews")).toBeVisible();
  await expectNoSeriousViolations(page);

  await page.goto("/reviews");
  await expect(page.locator("tbody tr td a").first()).toBeVisible();
  await expectNoSeriousViolations(page);

  await page.locator("tbody tr td a").first().click();
  await expect(page.getByRole("heading", { name: "Review detail" })).toBeVisible();
  await expect(page.getByText("Guest review")).toBeVisible();
  await expectNoSeriousViolations(page);

  await page.goto("/policies");
  await expect(page.getByText("Housekeeping Standards")).toBeVisible();
  await expectNoSeriousViolations(page);

  await page.goto("/reviews/submit");
  await expectNoSeriousViolations(page);

  await page.goto("/users");
  await expect(page.getByText("frontdesk")).toBeVisible();
  await expectNoSeriousViolations(page);
});
