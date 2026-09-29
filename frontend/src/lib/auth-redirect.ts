import type { DataSourceMode } from "@/lib/data/data-source";

export const SESSION_COOKIE = "JSESSIONID";
export const PROTECTED_PREFIXES = ["/dashboard", "/reviews", "/policies", "/users", "/ai"] as const;

const isProtected = (pathname: string) =>
  PROTECTED_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`));

/**
 * Optimistic server-side check: in API mode, a protected page without the backend session cookie
 * redirects to login before rendering. The backend still authorizes every API call.
 */
export function loginRedirectFor(pathname: string, search: string, hasSession: boolean, mode: DataSourceMode): string | null {
  if (mode !== "api" || hasSession || !isProtected(pathname)) return null;
  return `/login?next=${encodeURIComponent(pathname + search)}`;
}
