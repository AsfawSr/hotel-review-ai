import type { Review } from "@/lib/types";

// Mirrors ReviewExportService on the backend (demo mode builds the same file in the browser).
export const EXPORT_HEADERS = [
  "id",
  "submittedAt",
  "guestName",
  "rating",
  "reviewText",
  "analysisStatus",
  "sentiment",
  "sentimentScore",
  "mainTopic",
  "topics",
  "language",
  "analysisSource",
  "managerResponse",
] as const;

/** Prevents spreadsheet formula injection for guest-controlled text. */
export function neutralizeFormula(value: string): string {
  return /^[=+\-@\t\r]/.test(value) ? `'${value}` : value;
}

function cell(value: string | number | null | undefined): string {
  if (value == null) return "";
  const text = String(value);
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

export function reviewsToCsv(reviews: Review[]): string {
  const rows = reviews.map((r) => {
    const a = r.analysis;
    return [
      r.id,
      r.submittedAt,
      neutralizeFormula(r.guestName),
      r.rating,
      neutralizeFormula(r.reviewText),
      r.analysisStatus,
      a?.sentiment,
      a?.sentimentScore,
      a?.mainTopic,
      a?.topics.join("|"),
      a?.language,
      a?.source,
      a && neutralizeFormula(a.managerResponse),
    ]
      .map(cell)
      .join(",");
  });
  return `\uFEFF${[EXPORT_HEADERS.join(","), ...rows].join("\r\n")}\r\n`;
}

export function exportFileName(now = new Date()): string {
  return `reviews-${now.toISOString().slice(0, 10)}.csv`;
}

/** Triggers a browser download for a Blob. */
export function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
