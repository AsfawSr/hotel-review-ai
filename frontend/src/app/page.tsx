import type { Metadata } from "next";
import LandingPage from "@/components/landing-page";

export const metadata: Metadata = {
  title: { absolute: "HotelReviewAI — AI-powered hotel review analysis" },
};

export default function Home() {
  return <LandingPage />;
}
