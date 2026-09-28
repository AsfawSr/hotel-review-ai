import type { Metadata } from "next";
import { Suspense } from "react";
import { LoginForm } from "./login-form";

export const metadata: Metadata = { title: "Sign in" };

export default function LoginPage() {
  return (
    <main className="flex flex-1 items-center justify-center bg-gradient-to-br from-primary/10 via-background to-background px-4 py-12">
      <Suspense>
        <LoginForm />
      </Suspense>
    </main>
  );
}
