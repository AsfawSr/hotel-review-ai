import { CompassIcon } from "lucide-react";
import type { Metadata } from "next";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";

export const metadata: Metadata = { title: "Page not found" };

export default function NotFound() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-4 px-4 py-24 text-center">
      <CompassIcon className="size-10 text-primary" />
      <h1 className="text-3xl font-semibold tracking-tight">Page not found</h1>
      <p className="max-w-md text-muted-foreground">The page you are looking for does not exist or has been moved.</p>
      <div className="flex gap-2">
        <Link href="/" className={buttonVariants({ variant: "outline" })}>
          Home
        </Link>
        <Link href="/dashboard" className={buttonVariants()}>
          Dashboard
        </Link>
      </div>
    </main>
  );
}
