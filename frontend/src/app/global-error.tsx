"use client";

// Replaces the root layout when it fails, so it must render its own <html> and <body>.
export default function GlobalError({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  return (
    <html lang="en">
      <body style={{ fontFamily: "system-ui, sans-serif", display: "grid", placeItems: "center", minHeight: "100vh", margin: 0 }}>
        <div style={{ textAlign: "center", maxWidth: 420, padding: 24 }}>
          <h1 style={{ fontSize: 24, marginBottom: 8 }}>HotelReviewAI is temporarily unavailable</h1>
          <p style={{ color: "#555" }}>An unexpected error occurred.{error.digest ? ` Reference: ${error.digest}` : ""}</p>
          <button
            onClick={() => retry()}
            style={{ marginTop: 16, padding: "8px 16px", borderRadius: 8, border: "none", background: "#15803d", color: "#fff", cursor: "pointer" }}
          >
            Try again
          </button>
        </div>
      </body>
    </html>
  );
}
