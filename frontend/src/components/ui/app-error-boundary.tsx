"use client";

import { Component, ReactNode } from "react";
import { ErrorState } from "@/components/ui/error-state";

type AppErrorBoundaryProps = {
  children: ReactNode;
};

type AppErrorBoundaryState = {
  hasError: boolean;
  message?: string;
};

export class AppErrorBoundary extends Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  state: AppErrorBoundaryState = {
    hasError: false
  };

  static getDerivedStateFromError(error: Error): AppErrorBoundaryState {
    return { hasError: true, message: error.message };
  }

  private handleReset = () => {
    this.setState({ hasError: false, message: undefined });
    if (typeof window !== "undefined") {
      window.location.reload();
    }
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="mx-auto max-w-3xl px-6 py-10">
          <ErrorState
            title="예상하지 못한 오류가 발생했어요"
            description={this.state.message ?? "페이지를 새로고침하거나 다시 시도해주세요."}
            retryLabel="새로 고침"
            onRetry={this.handleReset}
          />
        </div>
      );
    }

    return this.props.children;
  }
}
