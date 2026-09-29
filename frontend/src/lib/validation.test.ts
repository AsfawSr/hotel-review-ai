import { describe, expect, it } from "vitest";
import { REVIEW_TEXT_MAX, validateSubmission } from "./validation";

describe("validateSubmission", () => {
  it("accepts a valid submission", () => {
    expect(validateSubmission({ guestName: "Ann", reviewText: "Nice", rating: 4 })).toEqual({});
    expect(validateSubmission({ guestName: "Ann", reviewText: "Nice", rating: null })).toEqual({});
  });

  it("requires name and text", () => {
    const errors = validateSubmission({ guestName: "  ", reviewText: "", rating: null });
    expect(Object.keys(errors)).toEqual(["guestName", "reviewText"]);
  });

  it("enforces backend limits", () => {
    const errors = validateSubmission({ guestName: "Ann", reviewText: "x".repeat(REVIEW_TEXT_MAX + 1), rating: 6 });
    expect(errors.reviewText).toBeDefined();
    expect(errors.rating).toBeDefined();
  });
});
