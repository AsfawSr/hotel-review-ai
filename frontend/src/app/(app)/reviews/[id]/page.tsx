import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { ReviewDetailView } from "./review-detail-view";

export async function generateMetadata({ params }: PageProps<"/reviews/[id]">): Promise<Metadata> {
  const { id } = await params;
  return { title: `Review #${id}` };
}

export default async function ReviewDetailPage({ params }: PageProps<"/reviews/[id]">) {
  const { id } = await params;
  const reviewId = Number(id);
  if (!Number.isInteger(reviewId) || reviewId <= 0) notFound();

  return <ReviewDetailView id={reviewId} />;
}
