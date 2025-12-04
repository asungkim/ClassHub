export type ApiErrorBody = {
  message?: string;
};

export type FetchErrorLike = {
  status?: number;
  message?: string;
};

export function getApiErrorMessage(error: unknown, fallback: string) {
  if (!error) {
    return fallback;
  }

  if (typeof error === "object" && error !== null) {
    const body = error as ApiErrorBody;
    if (body.message && typeof body.message === "string") {
      return body.message;
    }
  }

  return fallback;
}

export function getFetchError(response: unknown): FetchErrorLike | null {
  if (!response || typeof response !== "object") {
    return null;
  }

  const candidate = response as { error?: FetchErrorLike };
  return candidate.error ?? null;
}
