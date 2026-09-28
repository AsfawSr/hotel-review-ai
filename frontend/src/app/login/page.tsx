import type { Metadata } from "next";
import { Suspense } from "react";
import { ThemeToggle } from "@/components/theme-toggle";
import { LoginForm } from "./login-form";

export const metadata: Metadata = { title: "Sign in" };

export default function LoginPage() {
  return (
    <main className="relative flex flex-1 items-center justify-center bg-gradient-to-br from-primary/10 via-background to-background px-4 py-12">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>
      <Suspense>
        <LoginForm />
      </Suspense>
    </main>
  );
}
