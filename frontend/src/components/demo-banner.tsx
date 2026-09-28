"use client";

import { FlaskConicalIcon, RotateCcwIcon } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { dataSourceMode } from "@/lib/data";
import { useResetDemo } from "@/lib/data/hooks";

export function DemoBanner() {
  const reset = useResetDemo();
  if (dataSourceMode !== "mock") return null;

  return (
    <div className="border-b bg-amber-50 text-amber-900 dark:bg-amber-950/40 dark:text-amber-200">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2 px-4 py-1.5 text-xs sm:px-6">
        <span className="inline-flex items-center gap-1.5">
          <FlaskConicalIcon className="size-3.5" />
          Demo mode — sample data stored in your browser. AI analysis is simulated.
        </span>
        <Button
          variant="ghost"
          size="xs"
          disabled={reset.isPending}
          onClick={() => reset.mutate(undefined, { onSuccess: () => toast.success("Demo data has been reset.") })}
        >
          <RotateCcwIcon />
          Reset demo data
        </Button>
      </div>
    </div>
  );
}
