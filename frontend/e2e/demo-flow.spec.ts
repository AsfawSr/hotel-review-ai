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
  await expect(page.getByText("Manager reply")).toBeVisible({ timeout: 20_000 });
  await expect(page.getByText("Negative").first()).toBeVisible();

  await page.getByRole("button", { name: "Edit" }).click();
  await page.getByLabel("Reply text").fill("We are sorry about your stay and have addressed the issues.");
  await page.getByRole("button", { name: "Save" }).click();
  await page.getByRole("button", { name: "Approve" }).click();
  await page.getByRole("button", { name: "Mark as sent" }).click();
  await expect(page.getByText("Sent", { exact: true })).toBeVisible();
});

test("CSV import reports imported and skipped rows", async ({ page }) => {
  await page.goto("/login?next=%2Freviews%2Fsubmit");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page).toHaveURL(/\/reviews\/submit/);

  await page.getByLabel("CSV file").setInputFiles({
    name: "reviews.csv",
    mimeType: "text/csv",
    buffer: Buffer.from('guest,review,stars\nAda,"Great stay, thanks",5\n,No name,3\n'),
  });
  await page.getByRole("button", { name: "Import" }).click();
  await expect(page.getByText("1 imported, 1 skipped")).toBeVisible();
  await expect(page.getByRole("cell", { name: "guestName: must not be blank" })).toBeVisible();
});

test("reviews can be searched and exported as CSV", async ({ page }) => {
  await page.goto("/login?next=%2Freviews");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page).toHaveURL(/\/reviews$/);

  await page.getByLabel("Search reviews").fill("breakfast cold");
  await page.getByRole("button", { name: "Apply filters" }).click();
  await expect(page).toHaveURL(/q=breakfast\+cold/);
  await expect(page.getByRole("cell", { name: "Jack Thompson" })).toBeVisible();

  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export CSV" }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toMatch(/^reviews-\d{4}-\d{2}-\d{2}\.csv$/);
});

test("protected pages redirect to login", async ({ page }) => {
  await page.goto("/reviews");
  await expect(page).toHaveURL(/\/login\?next=%2Freviews/);
});

test("unknown routes show the 404 page", async ({ page }) => {
  const response = await page.goto("/does-not-exist");
  expect(response?.status()).toBe(404);
  await expect(page.getByRole("heading", { name: "Page not found" })).toBeVisible();
});
