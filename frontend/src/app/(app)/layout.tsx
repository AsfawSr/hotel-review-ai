import { AuthGuard } from "@/components/auth-guard";
import { DemoBanner } from "@/components/demo-banner";
import { Navbar } from "@/components/navbar";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <DemoBanner />
      <Navbar />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6 sm:px-6 sm:py-8">
        <AuthGuard>{children}</AuthGuard>
      </main>
      <footer className="border-t py-4 text-center text-xs text-muted-foreground">
        HotelReviewAI — Spring Boot · Spring AI · Next.js
      </footer>
    </>
  );
}
