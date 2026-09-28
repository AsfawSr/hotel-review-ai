export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export const isNotFound = (error: unknown) => error instanceof ApiError && error.status === 404;
export const isUnauthorized = (error: unknown) => error instanceof ApiError && error.status === 401;
