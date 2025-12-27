"use client";

import { useCallback, useEffect, useState } from "react";
import { fetchMyFeedbacks } from "@/lib/dashboard-api";
import type { FeedbackResponse } from "@/types/dashboard";

const DEFAULT_SIZE = 3;

type FeedbackSummaryState = {
  items: FeedbackResponse[];
  isLoading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
};

export function useFeedbackSummary(size = DEFAULT_SIZE): FeedbackSummaryState {
  const [items, setItems] = useState<FeedbackResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchSummary = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await fetchMyFeedbacks({ status: "ALL", page: 0, size });
      setItems(result.items ?? []);
    } catch (err) {
      const message = err instanceof Error ? err.message : "피드백 요약을 불러오지 못했습니다.";
      setError(message);
      setItems([]);
    } finally {
      setIsLoading(false);
    }
  }, [size]);

  useEffect(() => {
    void fetchSummary();
  }, [fetchSummary]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const handleCreated = () => {
      void fetchSummary();
    };
    window.addEventListener("feedback:created", handleCreated as EventListener);
    return () => window.removeEventListener("feedback:created", handleCreated as EventListener);
  }, [fetchSummary]);

  return { items, isLoading, error, refresh: fetchSummary };
}
