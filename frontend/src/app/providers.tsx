"use client";

import { QueryCache, QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactNode, useState } from "react";
import { SessionProvider } from "@/components/session/session-provider";
import { ToastProvider } from "@/components/ui/toast";
import { clearAuthToken } from "@/lib/api";

function createQueryClient() {
  let client: QueryClient | null = null;

  const queryCache = new QueryCache({
    onError: (error) => {
      const status = (error as { status?: number }).status;
      if (status === 401 || status === 419) {
        clearAuthToken();
        client?.invalidateQueries({ queryKey: ["session", "current"] }).catch(() => {});
      }
    }
  });

  client = new QueryClient({
    queryCache,
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000,
        refetchOnWindowFocus: false
      }
    }
  });

  return client;
}

export function Providers({ children }: { children: ReactNode }) {
  const [queryClient] = useState(createQueryClient);

  return (
    <QueryClientProvider client={queryClient}>
      <SessionProvider>
        <ToastProvider>{children}</ToastProvider>
      </SessionProvider>
    </QueryClientProvider>
  );
}
