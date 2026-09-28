"use client";

import { HotelIcon, LogOutIcon, MenuIcon, XIcon } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { useCurrentUser, useLogout } from "@/lib/data/hooks";
import { cn } from "@/lib/utils";

const NAV_ITEMS = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/reviews", label: "Reviews" },
  { href: "/reviews/submit", label: "Submit Review" },
  { href: "/ai/status", label: "AI Status" },
] as const;

function isActive(pathname: string, href: string) {
  if (href === "/reviews") return pathname === "/reviews" || /^\/reviews\/\d+/.test(pathname);
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function Navbar() {
  const pathname = usePathname();
  const router = useRouter();
  const { data: user } = useCurrentUser();
  const logout = useLogout();
  const [open, setOpen] = useState(false);

  const handleLogout = () =>
    logout.mutate(undefined, { onSuccess: () => router.replace("/login?logout") });

  const links = NAV_ITEMS.map((item) => (
    <Link
      key={item.href}
      href={item.href}
      onClick={() => setOpen(false)}
      className={cn(
        "rounded-md px-3 py-1.5 text-sm font-medium transition-colors hover:bg-accent hover:text-accent-foreground",
        isActive(pathname, item.href) ? "bg-accent text-accent-foreground" : "text-muted-foreground",
      )}
    >
      {item.label}
    </Link>
  ));

  return (
    <header className="sticky top-0 z-40 border-b bg-background/90 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-7xl items-center gap-4 px-4 sm:px-6">
        <Link href="/dashboard" className="flex items-center gap-2 font-semibold text-primary">
          <HotelIcon className="size-5" />
          HotelReviewAI
        </Link>
        <nav className="hidden flex-1 items-center gap-1 md:flex">{links}</nav>
        <div className="ml-auto hidden items-center gap-3 md:flex">
          {user && <span className="text-sm text-muted-foreground">{user.username}</span>}
          <Button variant="outline" size="sm" onClick={handleLogout} disabled={logout.isPending}>
            <LogOutIcon />
            Logout
          </Button>
        </div>
        <Button
          variant="ghost"
          size="icon"
          className="ml-auto md:hidden"
          aria-label="Toggle navigation"
          onClick={() => setOpen((v) => !v)}
        >
          {open ? <XIcon /> : <MenuIcon />}
        </Button>
      </div>
      {open && (
        <nav className="flex flex-col gap-1 border-t px-4 py-3 md:hidden">
          {links}
          <Button variant="outline" size="sm" className="mt-2 w-fit" onClick={handleLogout}>
            <LogOutIcon />
            Logout
          </Button>
        </nav>
      )}
    </header>
  );
}
