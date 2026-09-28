"use client";

import { AlertTriangleIcon, RefreshCwIcon } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";

export function QueryError({ error, onRetry }: { error: Error; onRetry?: () => void }) {
  return (
    <Alert variant="destructive">
      <AlertTriangleIcon />
      <AlertTitle>Something went wrong</AlertTitle>
      <AlertDescription className="flex flex-wrap items-center gap-3">
        {error.message}
        {onRetry && (
          <Button variant="outline" size="xs" onClick={onRetry}>
            <RefreshCwIcon />
            Try again
          </Button>
        )}
      </AlertDescription>
    </Alert>
  );
}
