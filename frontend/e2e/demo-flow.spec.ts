import { expect, test } from "@playwright/test";

test("demo flow: landing -> login -> submit review -> analysis completes", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("actionable insight");

  await page.getByRole("link", { name: "Try the live demo" }).click();
  await expect(page).toHaveURL(/\/login/);
  await page.getByRole("button", { name: "Sign in" }).click();

  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByText("Total reviews")).toBeVisible();

  await page.getByRole("link", { name: "Submit Review" }).first().click();
  await page.getByLabel("Guest name").fill("E2E Guest");
  await page.getByLabel("Review").fill("The room was dirty and the staff were rude. Never again.");
  await page.getByRole("button", { name: "Submit review" }).click();

  await expect(page).toHaveURL(/\/reviews\/\d+$/);
  await expect(page.getByText("Suggested manager response")).toBeVisible({ timeout: 20_000 });
  await expect(page.getByText("Negative").first()).toBeVisible();
});

test("protected pages redirect to login", async ({ page }) => {
  await page.goto("/reviews");
  await expect(page).toHaveURL(/\/login\?next=%2Freviews/);
});
