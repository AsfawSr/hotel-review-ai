import type { Metadata } from "next";
import { AiStatusView } from "./ai-status-view";

export const metadata: Metadata = { title: "AI status" };

export default function AiStatusPage() {
  return <AiStatusView />;
}
