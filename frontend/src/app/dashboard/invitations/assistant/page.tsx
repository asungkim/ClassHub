"use client";

import { useRoleGuard } from "@/hooks/use-role-guard";
import { useAssistantInvitations, useCreateAssistantLink } from "@/hooks/queries/invitations";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { toast } from "sonner";

export default function AssistantInvitationsPage() {
  const { canRender, fallback } = useRoleGuard(["TEACHER"]);

  const { data: invitations, isLoading } = useAssistantInvitations({ status: "PENDING" });
  const createLink = useCreateAssistantLink();

  if (!canRender) {
    return fallback;
  }

  // code가 있는 유효한 초대만 활성 초대로 간주
  const activeInvitation = invitations?.find((inv) => inv.code);
  const appUrl = process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000";
  const inviteUrl = activeInvitation?.code
    ? `${appUrl}/auth/invitation/verify?code=${activeInvitation.code}`
    : "";

  const handleCopyLink = async () => {
    if (!inviteUrl) return;

    try {
      await navigator.clipboard.writeText(inviteUrl);
      toast.success("초대 링크가 복사되었습니다.");
    } catch (err) {
      toast.error("링크 복사에 실패했습니다.");
    }
  };

  const handleRefreshLink = () => {
    createLink.mutate();
  };

  return (
    <DashboardShell
      title="조교 초대"
      subtitle="조교 초대 링크를 생성하고 관리합니다."
    >
      <Card>
        <div className="p-8 space-y-6">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold text-gray-900">조교 초대 링크</h3>
              {activeInvitation && (
                <Button
                  variant="secondary"
                  onClick={handleRefreshLink}
                  disabled={createLink.isPending}
                  className="h-9"
                >
                  {createLink.isPending ? "교체 중..." : "링크 교체"}
                </Button>
              )}
            </div>

            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="text-sm text-gray-500">로딩 중...</div>
              </div>
            ) : activeInvitation ? (
              <div className="space-y-4">
                <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-lg border border-gray-200">
                  <div className="flex-1 font-mono text-sm text-gray-700 break-all">
                    {inviteUrl}
                  </div>
                  <Button
                    onClick={handleCopyLink}
                    className="h-9 px-4 flex-shrink-0"
                  >
                    복사
                  </Button>
                </div>
                <div className="text-xs text-gray-500">
                  만료일: {activeInvitation.expiredAt
                    ? new Date(activeInvitation.expiredAt).toLocaleDateString("ko-KR")
                    : "없음"
                  }
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="text-center py-12 text-gray-500">
                  생성된 초대 링크가 없습니다.
                </div>
                <div className="flex justify-center">
                  <Button
                    onClick={handleRefreshLink}
                    disabled={createLink.isPending}
                  >
                    {createLink.isPending ? "생성 중..." : "초대 링크 생성"}
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      </Card>
    </DashboardShell>
  );
}
