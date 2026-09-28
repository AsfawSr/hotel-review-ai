import type { Metadata } from "next";
import { Suspense } from "react";
import { ReviewsView } from "./reviews-view";

export const metadata: Metadata = { title: "Reviews" };

export default function ReviewsPage() {
  return (
    <Suspense>
      <ReviewsView />
    </Suspense>
  );
}
