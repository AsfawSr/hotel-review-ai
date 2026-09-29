import type { ImportRowError, ReviewSubmission } from "@/lib/types";
import { GUEST_NAME_MAX, REVIEW_TEXT_MAX } from "@/lib/validation";

// Mirrors ReviewImportService on the backend so demo mode behaves the same.
export const IMPORT_MAX_ROWS = 500;
export const IMPORT_MAX_BYTES = 2 * 1024 * 1024;

const HEADER_ALIASES: Record<string, "guestName" | "reviewText" | "rating"> = {
  guestname: "guestName",
  guest: "guestName",
  name: "guestName",
  reviewer: "guestName",
  reviewtext: "reviewText",
  review: "reviewText",
  text: "reviewText",
  comment: "reviewText",
  rating: "rating",
  stars: "rating",
  score: "rating",
};

export class CsvImportError extends Error {}

interface CsvRecord {
  line: number;
  fields: string[];
}

/** RFC 4180 parser that records the physical line each record starts on. Blank lines are skipped. */
export function parseCsv(text: string): CsvRecord[] {
  const input = text.replace(/^\uFEFF/, "");
  const records: CsvRecord[] = [];
  let fields: string[] = [];
  let field = "";
  let quoted = false;
  let line = 1;
  let start = 1;

  const endRecord = () => {
    fields.push(field);
    if (fields.length > 1 || fields[0].trim() !== "") records.push({ line: start, fields });
    fields = [];
    field = "";
  };

  for (let i = 0; i < input.length; i++) {
    const c = input[i];
    if (quoted) {
      if (c === '"') {
        if (input[i + 1] === '"') {
          field += '"';
          i++;
        } else quoted = false;
      } else {
        if (c === "\n") line++;
        field += c;
      }
    } else if (c === '"') {
      quoted = true;
    } else if (c === ",") {
      fields.push(field);
      field = "";
    } else if (c === "\r" || c === "\n") {
      if (c === "\r" && input[i + 1] === "\n") i++;
      endRecord();
      line++;
      start = line;
    } else {
      field += c;
    }
  }
  if (quoted) throw new CsvImportError("The file is not valid CSV: unterminated quoted field");
  if (field !== "" || fields.length) endRecord();
  return records;
}

export interface ParsedImport {
  rows: { line: number; submission: ReviewSubmission }[];
  errors: ImportRowError[];
}

export function parseReviewCsv(text: string): ParsedImport {
  const [header, ...records] = parseCsv(text);
  if (!header) throw new CsvImportError("CSV header must include guestName and reviewText columns");

  const columns: Partial<Record<"guestName" | "reviewText" | "rating", number>> = {};
  header.fields.forEach((name, index) => {
    const canonical = HEADER_ALIASES[name.toLowerCase().replace(/[^a-z]/g, "")];
    if (canonical && columns[canonical] === undefined) columns[canonical] = index;
  });
  if (columns.guestName === undefined || columns.reviewText === undefined) {
    throw new CsvImportError("CSV header must include guestName and reviewText columns");
  }
  if (records.length > IMPORT_MAX_ROWS) {
    throw new CsvImportError(`At most ${IMPORT_MAX_ROWS} rows can be imported at once`);
  }

  const value = (fields: string[], index: number | undefined) => (index === undefined ? "" : (fields[index] ?? "").trim());
  const rows: ParsedImport["rows"] = [];
  const errors: ImportRowError[] = [];

  for (const { line, fields } of records) {
    const guestName = value(fields, columns.guestName);
    const reviewText = value(fields, columns.reviewText);
    const rawRating = value(fields, columns.rating);
    let rating: number | null = null;
    if (rawRating) {
      const parsed = Number(rawRating);
      if (Number.isNaN(parsed)) {
        errors.push({ line, message: "rating: not a number" });
        continue;
      }
      rating = Math.round(parsed);
    }

    const violations: string[] = [];
    if (!guestName) violations.push("guestName: must not be blank");
    else if (guestName.length > GUEST_NAME_MAX) violations.push(`guestName: size must be between 0 and ${GUEST_NAME_MAX}`);
    if (rating != null && rating < 1) violations.push("rating: must be greater than or equal to 1");
    if (rating != null && rating > 5) violations.push("rating: must be less than or equal to 5");
    if (!reviewText) violations.push("reviewText: must not be blank");
    else if (reviewText.length > REVIEW_TEXT_MAX) violations.push(`reviewText: size must be between 0 and ${REVIEW_TEXT_MAX}`);

    if (violations.length) errors.push({ line, message: violations.join("; ") });
    else rows.push({ line, submission: { guestName, reviewText, rating } });
  }
  return { rows, errors };
}
