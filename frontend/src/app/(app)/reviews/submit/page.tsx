import type { Metadata } from "next";
import { SubmitReviewForm } from "./submit-review-form";

export const metadata: Metadata = { title: "Submit review" };

export default function SubmitReviewPage() {
  return <SubmitReviewForm />;
}
