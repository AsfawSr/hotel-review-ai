import { NextResponse, type NextRequest } from "next/server";
import { loginRedirectFor, SESSION_COOKIE } from "@/lib/auth-redirect";

// Read the flag directly; importing the data layer would bundle mock data into the proxy.
const mode = process.env.NEXT_PUBLIC_DATA_SOURCE === "api" ? "api" : "mock";

export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  const target = loginRedirectFor(pathname, search, request.cookies.has(SESSION_COOKIE), mode);
  return target ? NextResponse.redirect(new URL(target, request.url)) : NextResponse.next();
}

export const config = {
  matcher: ["/dashboard", "/reviews/:path*", "/policies/:path*", "/ai/:path*"],
};
