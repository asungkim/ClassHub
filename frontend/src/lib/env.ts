const FALLBACK_BASE_URL = "http://localhost:8080/api/v1";

const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL ?? FALLBACK_BASE_URL;
const mockToken = process.env.NEXT_PUBLIC_MOCK_TOKEN ?? "";

export const env = {
  apiBaseUrl: apiBase,
  mockToken
};

export function describeEnv() {
  return {
    apiBaseUrl: env.apiBaseUrl,
    mockTokenConfigured: env.mockToken.length > 0
  };
}
