"use client";

import { HotelIcon, LogInIcon } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { dataSourceMode } from "@/lib/data";
import { useCurrentUser, useLogin } from "@/lib/data/hooks";
import { DEMO_CREDENTIALS } from "@/lib/data/mock-source";

// Only allow same-origin relative paths to prevent open redirects.
function safeNext(value: string | null): string {
  return value && value.startsWith("/") && !value.startsWith("//") ? value : "/dashboard";
}

export function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const next = safeNext(params.get("next"));
  const isDemo = dataSourceMode === "mock";

  const { data: user } = useCurrentUser();
  const login = useLogin();
  const [username, setUsername] = useState<string>(isDemo ? DEMO_CREDENTIALS.username : "");
  const [password, setPassword] = useState<string>(isDemo ? DEMO_CREDENTIALS.password : "");

  useEffect(() => {
    if (user) router.replace(next);
  }, [user, next, router]);

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    login.mutate({ username, password }, { onSuccess: () => router.replace(next) });
  };

  return (
    <Card className="w-full max-w-sm shadow-lg">
      <CardHeader className="text-center">
        <div className="mx-auto mb-2 flex size-12 items-center justify-center rounded-full bg-primary/10 text-primary">
          <HotelIcon className="size-6" />
        </div>
        <CardTitle className="text-xl">HotelReviewAI</CardTitle>
        <CardDescription>Sign in to analyze guest reviews</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {params.has("logout") && !login.isError && (
          <Alert>
            <AlertDescription>You have been signed out.</AlertDescription>
          </Alert>
        )}
        {login.isError && (
          <Alert variant="destructive">
            <AlertDescription>{login.error.message}</AlertDescription>
          </Alert>
        )}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="username">Username</Label>
            <Input
              id="username"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <Button type="submit" size="lg" className="w-full" disabled={login.isPending}>
            <LogInIcon />
            {login.isPending ? "Signing in…" : "Sign in"}
          </Button>
        </form>
        {isDemo && (
          <p className="text-center text-xs text-muted-foreground">
            Demo credentials are pre-filled: <strong>{DEMO_CREDENTIALS.username}</strong> /{" "}
            <strong>{DEMO_CREDENTIALS.password}</strong>
          </p>
        )}
      </CardContent>
    </Card>
  );
}
