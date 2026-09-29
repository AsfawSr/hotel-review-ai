import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "./errors";
import { DEMO_CREDENTIALS, mockDataSource, reanchor } from "./mock-source";

class MemoryStorage {
  private data = new Map<string, string>();
  getItem = (key: string) => this.data.get(key) ?? null;
  setItem = (key: string, value: string) => void this.data.set(key, value);
  removeItem = (key: string) => void this.data.delete(key);
}

beforeEach(() => {
  vi.stubGlobal("window", { localStorage: new MemoryStorage() });
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("mockDataSource", () => {
  it("filters and paginates seeded reviews", async () => {
    const page = await mockDataSource.listReviews({ page: 0, size: 10, sentiment: "NEGATIVE" });
    expect(page.totalElements).toBeGreaterThan(0);
    expect(page.content.every((r) => r.sentiment === "NEGATIVE")).toBe(true);

    const all = await mockDataSource.listReviews({ page: 0, size: 10 });
    expect(all.content).toHaveLength(10);
    expect(all.totalPages).toBe(Math.ceil(all.totalElements / 10));
    const dates = all.content.map((r) => r.submittedAt);
    expect([...dates].sort().reverse()).toEqual(dates);
  });

  it("builds dashboard metrics consistent with the reviews", async () => {
    const metrics = await mockDataSource.getDashboard();
    const analyzed = Object.values(metrics.sentimentCounts).reduce((a, b) => a + b, 0);
    expect(metrics.totalReviews).toBeGreaterThanOrEqual(analyzed);
    expect(metrics.averageRating).toBeGreaterThan(0);
  });

  it("rejects invalid submissions with field errors", async () => {
    await expect(mockDataSource.submitReview({ guestName: "", reviewText: "", rating: null })).rejects.toMatchObject({
      status: 400,
      fieldErrors: { guestName: expect.any(String), reviewText: expect.any(String) },
    });
  });

  it("simulates async analysis from PENDING to COMPLETED", async () => {
    const created = await mockDataSource.submitReview({ guestName: "Test", reviewText: "Dirty room, rude staff", rating: 1 });
    expect(created.analysisStatus).toBe("PENDING");

    vi.useFakeTimers({ toFake: ["Date"], now: Date.now() + 10_000 });
    const detail = await mockDataSource.getReview(created.id);

    expect(detail.review.analysisStatus).toBe("COMPLETED");
    expect(detail.review.analysis?.sentiment).toBe("NEGATIVE");
    expect(detail.review.analysis?.source).toBe("FALLBACK");
    // Like the backend, heuristic analyses are not grounded in retrieved policies.
    expect(detail.ragEnabled).toBe(false);
  });

  it("returns policy context for AI analyses", async () => {
    const aiReview = (await mockDataSource.listReviews({ page: 0, size: 50, status: "COMPLETED" })).content[0];
    const detail = await mockDataSource.getReview(aiReview.id);

    expect(detail.review.analysis?.source).toBe("AI");
    expect(detail.policyContext).toContain("Title:");
  });

  it("returns 404 for unknown reviews", async () => {
    await expect(mockDataSource.getReview(999_999)).rejects.toBeInstanceOf(ApiError);
  });

  it("manages policies and uses active ones as review context", async () => {
    const seeded = await mockDataSource.listPolicies();
    expect(seeded.length).toBe(8);

    await expect(
      mockDataSource.createPolicy({ title: "", category: "", content: "", tags: [], source: null, effectiveDate: null, active: true }),
    ).rejects.toMatchObject({ status: 400, fieldErrors: { title: expect.any(String) } });

    const created = await mockDataSource.createPolicy({
      title: " Pet Policy ", category: "Front Office", content: "Dogs welcome.", tags: ["pets", "pets"], source: "", effectiveDate: null, active: true,
    });
    expect(created).toMatchObject({ id: 9, title: "Pet Policy", tags: ["pets"], source: null });

    const updated = await mockDataSource.updatePolicy(created.id, { ...created, active: false });
    expect(updated.active).toBe(false);
    expect(await mockDataSource.reindexPolicies()).toEqual({ ragEnabled: true, indexed: 8 });

    await mockDataSource.deletePolicy(created.id);
    expect(await mockDataSource.listPolicies()).toHaveLength(8);
    await expect(mockDataSource.deletePolicy(created.id)).rejects.toMatchObject({ status: 404 });
  });

  it("shifts a stale timeline forward on return visits", async () => {
    const newest = (await mockDataSource.listReviews({ page: 0, size: 10 })).content[0].submittedAt;

    vi.useFakeTimers({ toFake: ["Date"], now: Date.now() + 30 * 24 * 60 * 60 * 1000 });
    const later = (await mockDataSource.listReviews({ page: 0, size: 10 })).content[0].submittedAt;

    const shiftedDays = (Date.parse(later) - Date.parse(newest)) / (24 * 60 * 60 * 1000);
    expect(Math.round(shiftedDays)).toBe(30);
  });

  it("does not re-anchor within a day", () => {
    const state = { reviews: [], policies: [], users: [], replies: {}, anchoredAt: 1_000 };
    expect(reanchor(state, 1_000 + 60_000)).toBe(state);
  });

  it("manages users and protects the last admin", async () => {
    expect((await mockDataSource.listUsers()).map((u) => u.username)).toEqual(["auditor", "demo", "frontdesk"]);

    await expect(mockDataSource.createUser({ username: "x", password: "short", role: "VIEWER" })).rejects.toMatchObject({
      status: 400,
      fieldErrors: { username: expect.any(String), password: expect.any(String) },
    });
    await expect(mockDataSource.createUser({ username: "Demo", password: "long-enough-pass", role: "VIEWER" })).rejects.toMatchObject({ status: 409 });

    const created = await mockDataSource.createUser({ username: "night.audit", password: "long-enough-pass", role: "VIEWER" });
    expect(created).toMatchObject({ username: "night.audit", role: "VIEWER", enabled: true });
    expect(JSON.stringify(window.localStorage.getItem("hotel-review-ai:demo:v1"))).not.toContain("long-enough-pass");

    const demo = (await mockDataSource.listUsers()).find((u) => u.username === "demo")!;
    await expect(mockDataSource.updateUser(demo.id, { role: "VIEWER", enabled: true })).rejects.toMatchObject({ status: 409 });
  });

  it("runs the reply workflow with the backend's transition rules", async () => {
    const reviewId = (await mockDataSource.listReviews({ page: 0, size: 10, status: "COMPLETED" })).content[0].id;

    const draft = await mockDataSource.getReply(reviewId);
    expect(draft).toMatchObject({ status: "DRAFT", updatedBy: "AI", history: [] });

    await expect(mockDataSource.markReplySent(reviewId)).rejects.toMatchObject({ status: 409 });
    const edited = await mockDataSource.editReply(reviewId, "  Thanks for staying!  ");
    expect(edited).toMatchObject({ status: "EDITED", text: "Thanks for staying!" });
    await mockDataSource.approveReply(reviewId);
    const sent = await mockDataSource.markReplySent(reviewId);
    expect(sent.status).toBe("SENT");
    expect(sent.history.map((r) => r.status)).toEqual(["EDITED", "APPROVED", "SENT"]);
    await expect(mockDataSource.editReply(reviewId, "late")).rejects.toMatchObject({ status: 409 });
  });

  it("only accepts the demo credentials", async () => {
    await expect(mockDataSource.login("demo", "wrong")).rejects.toMatchObject({ status: 401 });
    const user = await mockDataSource.login(DEMO_CREDENTIALS.username, DEMO_CREDENTIALS.password);
    expect(user.username).toBe("demo");
    expect(await mockDataSource.getCurrentUser()).toMatchObject({ username: "demo" });
    await mockDataSource.logout();
    expect(await mockDataSource.getCurrentUser()).toBeNull();
  });
});
