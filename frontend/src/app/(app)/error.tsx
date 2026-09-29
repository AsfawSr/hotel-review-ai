"use client";

import { AlertTriangleIcon, RotateCwIcon } from "lucide-react";
import Link from "next/link";
import { useEffect } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export default function AppError({ error, retry }: { error: Error & { digest?: string }; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <Card className="mx-auto mt-10 max-w-lg">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <AlertTriangleIcon className="size-5 text-destructive" />
          Something went wrong
        </CardTitle>
        <CardDescription>
          This page failed to load. You can try again or go back to the dashboard.
          {error.digest && <span className="mt-1 block font-mono text-xs">Reference: {error.digest}</span>}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex gap-2">
        <Button onClick={() => retry()}>
          <RotateCwIcon />
          Try again
        </Button>
        <Link href="/dashboard" className={buttonVariants({ variant: "outline" })}>
          Go to dashboard
        </Link>
      </CardContent>
    </Card>
  );
}
