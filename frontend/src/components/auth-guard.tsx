"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { useCurrentUser } from "@/lib/data/hooks";

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const { data: user, isPending } = useCurrentUser();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isPending && !user) router.replace(`/login?next=${encodeURIComponent(pathname)}`);
  }, [isPending, user, router, pathname]);

  if (isPending || !user) {
    return (
      <div className="mx-auto w-full max-w-7xl space-y-4 px-4 py-8 sm:px-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-32 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }
  return <>{children}</>;
}
