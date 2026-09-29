import { describe, expect, it } from "vitest";
import { parseCsv } from "./review-import";
import { EXPORT_HEADERS, neutralizeFormula, reviewsToCsv } from "./review-export";
import type { Review } from "./types";

const review: Review = {
  id: 7,
  guestName: "=cmd|' /C calc'!A0",
  reviewText: 'Nice, "really"\nnice',
  rating: 4,
  analysisStatus: "COMPLETED",
  analysisError: null,
  analysisUpdatedAt: null,
  submittedAt: "2025-01-02T03:04:05.000Z",
  updatedAt: "2025-01-02T03:04:05.000Z",
  analysis: {
    sentiment: "POSITIVE",
    sentimentScore: 80,
    topics: ["STAFF", "FOOD"],
    mainTopic: "STAFF",
    managerResponse: "Thanks!",
    source: "AI",
    modelName: "m",
    promptVersion: "v",
    language: "en",
    createdAt: "2025-01-02T03:04:05.000Z",
    updatedAt: "2025-01-02T03:04:05.000Z",
  },
};

describe("reviewsToCsv", () => {
  it("round-trips through the CSV parser with quoting and formula neutralization", () => {
    const [header, row] = parseCsv(reviewsToCsv([review]));
    expect(header.fields).toEqual([...EXPORT_HEADERS]);
    const record = Object.fromEntries(EXPORT_HEADERS.map((h, i) => [h, row.fields[i]]));
    expect(record.guestName).toBe("'=cmd|' /C calc'!A0");
    expect(record.reviewText).toBe('Nice, "really"\nnice');
    expect(record.topics).toBe("STAFF|FOOD");
    expect(record.language).toBe("en");
  });

  it("only prefixes dangerous leading characters", () => {
    expect(neutralizeFormula("-1")).toBe("'-1");
    expect(neutralizeFormula("@SUM(A1)")).toBe("'@SUM(A1)");
    expect(neutralizeFormula("Fine = ok")).toBe("Fine = ok");
  });
});
