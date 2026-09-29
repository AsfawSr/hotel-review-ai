import { describe, expect, it } from "vitest";
import { CsvImportError, parseCsv, parseReviewCsv } from "./review-import";

describe("parseCsv", () => {
  it("handles quoted commas, escaped quotes, CRLF and multi-line fields with physical line numbers", () => {
    const records = parseCsv('a,b\r\n"x, y","say ""hi"""\r\n\r\n"multi\nline",z\n');
    expect(records).toEqual([
      { line: 1, fields: ["a", "b"] },
      { line: 2, fields: ["x, y", 'say "hi"'] },
      { line: 4, fields: ["multi\nline", "z"] },
    ]);
  });

  it("rejects an unterminated quote", () => {
    expect(() => parseCsv('a,b\n"open,1')).toThrow(CsvImportError);
  });
});

describe("parseReviewCsv", () => {
  it("mirrors the backend import rules", () => {
    const { rows, errors } = parseReviewCsv(
      [
        "\uFEFFGuest,Review,Stars",
        'Ada Lovelace,"Great breakfast, friendly staff",5',
        'Bob,"Multi-line',
        'review text",4.0',
        ",Missing name,3",
        "Carl,Too many stars,9",
        "Dana,No rating given,",
        "Eve,Bad rating,abc",
      ].join("\n"),
    );
    expect(rows.map((r) => r.submission)).toEqual([
      { guestName: "Ada Lovelace", reviewText: "Great breakfast, friendly staff", rating: 5 },
      { guestName: "Bob", reviewText: "Multi-line\nreview text", rating: 4 },
      { guestName: "Dana", reviewText: "No rating given", rating: null },
    ]);
    expect(errors).toEqual([
      { line: 5, message: "guestName: must not be blank" },
      { line: 6, message: "rating: must be less than or equal to 5" },
      { line: 8, message: "rating: not a number" },
    ]);
  });

  it("requires guest name and review columns", () => {
    expect(() => parseReviewCsv("rating,notes\n5,hi")).toThrow("guestName and reviewText");
  });
});
