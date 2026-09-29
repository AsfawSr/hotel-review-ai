import { describe, expect, it } from "vitest";
import { analyzeReview, composeManagerResponse, detectTopics, estimateSentimentScore, sentimentFromScore } from "./analysis";

// Mirrors ReviewAnalysisProcessingService's fallback heuristic on the backend.
describe("estimateSentimentScore", () => {
  it("blends rating and text", () => {
    // rating 5 -> 100, text 50 + 2 positives -> 70; 100*0.55 + 70*0.45 = 86.5 -> 87
    expect(estimateSentimentScore("great and clean room", 5)).toBe(87);
    // rating 1 -> 20, text 50 - 2 negatives -> 30; 20*0.55 + 30*0.45 = 24.5 -> 25
    expect(estimateSentimentScore("dirty and rude", 1)).toBe(25);
  });

  it("uses text only when no rating is given", () => {
    expect(estimateSentimentScore("friendly staff", null)).toBe(60);
  });

  it("caps strongly negative text even with a high rating", () => {
    expect(estimateSentimentScore("worst stay, never again", 5)).toBeLessThanOrEqual(40);
  });

  it("stays within 0..100", () => {
    const text = "bad dirty rude noisy uncomfortable slow terrible hate poor worst";
    expect(estimateSentimentScore(text, 1)).toBeGreaterThanOrEqual(0);
  });
});

describe("sentimentFromScore", () => {
  it("uses the backend thresholds", () => {
    expect(sentimentFromScore(65)).toBe("POSITIVE");
    expect(sentimentFromScore(64)).toBe("NEUTRAL");
    expect(sentimentFromScore(36)).toBe("NEUTRAL");
    expect(sentimentFromScore(35)).toBe("NEGATIVE");
  });
});

describe("detectTopics", () => {
  it("detects multiple topics in order", () => {
    expect(detectTopics("the breakfast was great but the room was dirty")).toEqual(["CLEANLINESS", "FOOD"]);
  });

  it("falls back to OTHER", () => {
    expect(detectTopics("it was fine")).toEqual(["OTHER"]);
  });
});

describe("analyzeReview", () => {
  it("produces a consistent analysis", () => {
    const result = analyzeReview("Jane Doe", "Loved the pool and the friendly staff", 5, "2026-01-01T00:00:00Z");
    expect(result.sentiment).toBe("POSITIVE");
    expect(result.topics).toContain(result.mainTopic);
    expect(result.managerResponse).toContain("Dear Jane");
  });
});

describe("composeManagerResponse", () => {
  it("apologises for negative reviews", () => {
    expect(composeManagerResponse("Sam", "NEGATIVE", "NOISE")).toMatch(/apologise/);
  });
});
