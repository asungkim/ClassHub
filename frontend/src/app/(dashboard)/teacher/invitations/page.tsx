"use client";

import { useCallback, useEffect, useState } from "react";
import type { components } from "@/types/openapi";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { InlineError } from "@/components/ui/inline-error";
import { EmptyState } from "@/components/shared/empty-state";
import { TextField } from "@/components/ui/text-field";
import { Modal } from "@/components/ui/modal";
import { useToast } from "@/components/ui/toast";
import { api } from "@/lib/api";
import { getApiErrorMessage } from "@/lib/api-error";
import { InvitationStatusBadge } from "@/components/shared/invitation-status-badge";

const invitationFilters = [
  { label: "진행 중", value: "PENDING" },
  { label: "전체", value: "ALL" },
  { label: "수락됨", value: "ACCEPTED" },
  { label: "만료", value: "EXPIRED" },
  { label: "취소", value: "REVOKED" }
] as const;

type InvitationFilter = (typeof invitationFilters)[number]["value"];

type InvitationSummaryResponse = components["schemas"]["InvitationSummaryResponse"];
type AssistantInvitationCreateRequest = components["schemas"]["AssistantInvitationCreateRequest"];
type PageInvitationResponse = components["schemas"]["PageResponseInvitationSummaryResponse"];

const pageSize = 10;

const invitationRoute = "/auth/register/invited" as const;

export default function TeacherInvitationsPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  if (!canRender) {
    return fallback;
  }
  return <TeacherInvitationsContent />;
}

function TeacherInvitationsContent() {
  const { showToast } = useToast();

  const [invitationStatus, setInvitationStatus] = useState<InvitationFilter>("PENDING");
  const [invitationPage, setInvitationPage] = useState(0);
  const [invitations, setInvitations] = useState<InvitationSummaryResponse[]>([]);
  const [invitationTotal, setInvitationTotal] = useState(0);
  const [invitationLoading, setInvitationLoading] = useState(false);
  const [invitationError, setInvitationError] = useState<string | null>(null);

  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [inviteEmail, setInviteEmail] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [createSubmitting, setCreateSubmitting] = useState(false);

  const fetchInvitations = useCallback(
    async (status: InvitationFilter, page: number) => {
      setInvitationLoading(true);
      setInvitationError(null);
      try {
        const params = status === "ALL" ? { page, size: pageSize } : { status, page, size: pageSize };
        const response = await api.GET("/api/v1/teachers/me/invitations", {
          params: { query: params }
        });
        if (response.error || !response.data?.data) {
          throw new Error(getApiErrorMessage(response.error, "초대 목록을 가져오지 못했습니다."));
        }
        const pageData = response.data.data as PageInvitationResponse;
        setInvitations(pageData.content ?? []);
        setInvitationTotal(pageData.totalElements ?? 0);
      } catch (error) {
        setInvitationError(error instanceof Error ? error.message : "초대 목록을 가져오지 못했습니다.");
      } finally {
        setInvitationLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    void fetchInvitations(invitationStatus, invitationPage);
  }, [invitationStatus, invitationPage, fetchInvitations]);

  const handleCreateInvitation = async () => {
    if (!inviteEmail) {
      setCreateError("초대할 이메일을 입력해주세요.");
      return;
    }
    setCreateError(null);
    setCreateSubmitting(true);
    try {
      const requestBody: AssistantInvitationCreateRequest = { targetEmail: inviteEmail };
      const response = await api.POST("/api/v1/invitations", { body: requestBody });
      if (response.error || !response.data?.data?.code) {
        throw new Error(getApiErrorMessage(response.error, "초대 생성에 실패했습니다."));
      }
      const code = response.data.data.code;
      const inviteUrl = buildInvitationLink(code);
      await navigator.clipboard.writeText(inviteUrl);
      showToast("success", "초대 링크가 복사되었습니다.");
      setInviteEmail("");
      setCreateModalOpen(false);
      await fetchInvitations(invitationStatus, invitationPage);
    } catch (error) {
      setCreateError(error instanceof Error ? error.message : "초대 생성 중 오류가 발생했습니다.");
    } finally {
      setCreateSubmitting(false);
    }
  };

  const handleCopyInvitation = async (code?: string) => {
    if (!code) return;
    const inviteUrl = buildInvitationLink(code);
    await navigator.clipboard.writeText(inviteUrl);
    showToast("info", "초대 링크가 복사되었습니다.");
  };

  const handleRevokeInvitation = async (code?: string) => {
    if (!code) return;
    try {
      const response = (await api.PATCH("/api/v1/invitations/{code}/revoke", {
        params: { path: { code } }
      })) as { error?: unknown };
      if (response.error) {
        throw new Error(getApiErrorMessage(response.error, "초대를 취소하지 못했습니다."));
      }
      showToast("success", "초대를 취소했습니다.");
      await fetchInvitations(invitationStatus, invitationPage);
    } catch (error) {
      const message = error instanceof Error ? error.message : "초대 취소 중 오류가 발생했습니다.";
      setInvitationError(message);
      showToast("error", message);
    }
  };

  const invitationTotalPages = Math.ceil(invitationTotal / pageSize);

  return (
    <div className="space-y-6 lg:space-y-8">
      <PageHero onCreate={() => setCreateModalOpen(true)} />

      <Card
        title="초대 관리"
        description="진행 중인 초대를 확인하고 링크를 복사하거나 취소할 수 있습니다."
        actions={
          <Select
            value={invitationStatus}
            onChange={(event) => {
              setInvitationStatus(event.target.value as InvitationFilter);
              setInvitationPage(0);
            }}
            className="min-w-[160px]"
          >
            {invitationFilters.map((filter) => (
              <option key={filter.value} value={filter.value}>
                {filter.label}
              </option>
            ))}
          </Select>
        }
      >
        <InvitationTable
          invitations={invitations}
          loading={invitationLoading}
          error={invitationError}
          onCopy={handleCopyInvitation}
          onRevoke={handleRevokeInvitation}
        />

        <Pagination
          currentPage={invitationPage}
          totalPages={invitationTotalPages}
          onPageChange={setInvitationPage}
        />
      </Card>

      <CreateInvitationModal
        open={createModalOpen}
        email={inviteEmail}
        error={createError}
        submitting={createSubmitting}
        onClose={() => {
          setCreateModalOpen(false);
          setCreateError(null);
        }}
        onEmailChange={setInviteEmail}
        onSubmit={handleCreateInvitation}
      />
    </div>
  );
}

function PageHero({ onCreate }: { onCreate: () => void }) {
  return (
    <section className="rounded-3xl bg-white px-6 py-6 shadow-sm ring-1 ring-slate-100 sm:px-8">
      <p className="text-xs font-semibold uppercase tracking-wide text-blue-500">Invitation Management</p>
      <h1 className="mt-2 text-3xl font-bold text-slate-900">초대 관리</h1>
      <p className="mt-2 text-sm text-slate-500">
        조교 초대를 생성하고 링크를 발급하거나 복사해 공유할 수 있습니다.
      </p>
      <Button className="mt-4 w-full sm:w-auto" onClick={onCreate}>
        새 조교 초대 만들기
      </Button>
    </section>
  );
}

function InvitationTable({
  invitations,
  loading,
  error,
  onCopy,
  onRevoke
}: {
  invitations: InvitationSummaryResponse[];
  loading: boolean;
  error: string | null;
  onCopy: (code?: string) => void;
  onRevoke: (code?: string) => void;
}) {
  if (loading) {
    return <p className="mt-4 text-sm text-slate-500">초대 목록을 불러오는 중입니다…</p>;
  }

  if (error) {
    return <InlineError message={error} className="mt-4" />;
  }

  if (invitations.length === 0) {
    return (
      <EmptyState
        message="진행 중인 초대가 없습니다"
        description="새 초대를 생성해보세요."
        icon={<span className="text-2xl">✉️</span>}
      />
    );
  }

  return (
    <div className="mt-4 overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>코드</TableHead>
            <TableHead>대상 이메일</TableHead>
            <TableHead>만료일</TableHead>
            <TableHead>상태</TableHead>
            <TableHead className="text-right">액션</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {invitations.map((invite) => (
            <TableRow key={invite.code}>
              <TableCell className="font-mono text-xs text-slate-600">{invite.code}</TableCell>
              <TableCell>{invite.targetEmail ?? "-"}</TableCell>
              <TableCell>{invite.expiredAt ? new Date(invite.expiredAt).toLocaleString("ko-KR") : "-"}</TableCell>
              <TableCell>{invite.status ? <InvitationStatusBadge status={invite.status} /> : null}</TableCell>
              <TableCell>
                <div className="flex justify-end gap-2">
                  {invite.status !== "REVOKED" ? (
                    <Button variant="ghost" onClick={() => onCopy(invite.code)}>
                      링크 복사
                    </Button>
                  ) : null}
                  {invite.status === "PENDING" ? (
                    <Button variant="secondary" onClick={() => onRevoke(invite.code)}>
                      취소
                    </Button>
                  ) : null}
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function Pagination({
  currentPage,
  totalPages,
  onPageChange
}: {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  if (!Number.isFinite(totalPages) || totalPages <= 1) {
    return null;
  }

  return (
    <div className="mt-6 flex items-center justify-between text-sm text-slate-500">
      <span>
        페이지 {currentPage + 1} / {totalPages}
      </span>
      <div className="flex gap-2">
        <Button variant="ghost" disabled={currentPage === 0} onClick={() => onPageChange(currentPage - 1)}>
          이전
        </Button>
        <Button variant="ghost" disabled={currentPage + 1 >= totalPages} onClick={() => onPageChange(currentPage + 1)}>
          다음
        </Button>
      </div>
    </div>
  );
}

function CreateInvitationModal({
  open,
  email,
  error,
  submitting,
  onClose,
  onEmailChange,
  onSubmit
}: {
  open: boolean;
  email: string;
  error: string | null;
  submitting: boolean;
  onClose: () => void;
  onEmailChange: (value: string) => void;
  onSubmit: () => void;
}) {
  return (
    <Modal open={open} onClose={onClose} title="새 조교 초대" size="sm">
      <div className="space-y-4">
        <p className="text-sm text-slate-500">초대받을 조교 이메일을 입력하면 초대 링크가 생성됩니다.</p>
        <TextField
          label="조교 이메일"
          type="email"
          required
          placeholder="assistant@example.com"
          value={email}
          onChange={(event) => onEmailChange(event.target.value)}
          error={error ?? undefined}
        />
        <div className="flex justify-end gap-2">
          <Button variant="ghost" onClick={onClose}>
            취소
          </Button>
          <Button onClick={onSubmit} disabled={submitting}>
            {submitting ? "생성 중..." : "초대 발급"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

function buildInvitationLink(code: string) {
  const base = window.location.origin;
  const url = new URL(invitationRoute, base);
  url.searchParams.set("code", code);
  return url.toString();
}
