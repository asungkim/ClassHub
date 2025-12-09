"use client";

import { useState } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import {
  useStudentCandidates,
  useCreateStudentInvitations,
  useStudentInvitations
} from "@/hooks/queries/invitations";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { InvitationStatusBadge } from "@/components/shared/invitation-status-badge";
import { EmptyState } from "@/components/shared/empty-state";
import { LoadingSkeleton } from "@/components/shared/loading-skeleton";
import { toast } from "sonner";

type InvitationStatus = "PENDING" | "ACCEPTED" | "REVOKED" | "EXPIRED";

export default function StudentInvitationsPage() {
  const { canRender, fallback } = useRoleGuard(["TEACHER", "ASSISTANT"]);

  // Tab 1: 후보 목록
  const [searchName, setSearchName] = useState("");
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const { data: candidates, isLoading: candidatesLoading, error: candidatesError, refetch: refetchCandidates } = useStudentCandidates({ name: searchName });
  const createInvitations = useCreateStudentInvitations();

  // Tab 2: 생성된 초대
  const [statusFilter, setStatusFilter] = useState<InvitationStatus | "ALL">("ALL");
  const invitationFilters = statusFilter === "ALL" ? {} : { status: statusFilter };
  const { data: invitations, isLoading: invitationsLoading, error: invitationsError, refetch: refetchInvitations } = useStudentInvitations(invitationFilters);

  if (!canRender) {
    return fallback;
  }

  const handleSelectAll = () => {
    if (!candidates) return;
    if (selectedIds.length === candidates.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(candidates.map((c) => c.id || "").filter(Boolean));
    }
  };

  const handleSelectOne = (id: string) => {
    if (selectedIds.includes(id)) {
      setSelectedIds(selectedIds.filter((sid) => sid !== id));
    } else {
      setSelectedIds([...selectedIds, id]);
    }
  };

  const handleCreateInvitations = () => {
    createInvitations.mutate(
      { studentProfileIds: selectedIds },
      {
        onSuccess: () => {
          setSelectedIds([]);
        }
      }
    );
  };

  const handleCopyInvitationLink = async (code: string) => {
    const appUrl = process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000";
    const inviteUrl = `${appUrl}/auth/invitation/verify?code=${code}`;
    try {
      await navigator.clipboard.writeText(inviteUrl);
      toast.success("초대 링크가 복사되었습니다.");
    } catch (err) {
      toast.error("링크 복사에 실패했습니다.");
    }
  };

  return (
    <DashboardShell
      title="학생 초대"
      subtitle="학생 초대를 생성하고 관리합니다."
    >
      <Tabs defaultValue="candidates" className="space-y-4">
        <TabsList>
          <TabsTrigger value="candidates">초대 가능한 학생</TabsTrigger>
          <TabsTrigger value="invitations">생성된 초대</TabsTrigger>
        </TabsList>

        {/* 탭 1: 초대 가능한 학생 */}
        <TabsContent value="candidates">
          <Card>
            <div className="p-6 space-y-4">
              {/* 검색 + 버튼 */}
              <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                <Input
                  placeholder="학생 이름으로 검색..."
                  value={searchName}
                  onChange={(e) => setSearchName(e.target.value)}
                  className="md:w-80"
                />
                <Button
                  onClick={handleCreateInvitations}
                  disabled={selectedIds.length === 0 || createInvitations.isPending}
                >
                  {createInvitations.isPending
                    ? "생성 중..."
                    : `초대 생성 (${selectedIds.length})`}
                </Button>
              </div>

              {/* 로딩 */}
              {candidatesLoading && <LoadingSkeleton rows={5} columns={4} />}

              {/* 에러 */}
              {candidatesError && (
                <div className="py-8 text-center space-y-3">
                  <p className="text-sm text-red-600">후보 목록을 불러오는데 실패했습니다.</p>
                  <Button variant="secondary" onClick={() => refetchCandidates()}>
                    재시도
                  </Button>
                </div>
              )}

              {/* 빈 상태 */}
              {!candidatesLoading && !candidatesError && candidates && candidates.length === 0 && (
                <EmptyState message="초대할 수 있는 학생이 없습니다." />
              )}

              {/* Desktop 테이블 */}
              {!candidatesLoading && !candidatesError && candidates && candidates.length > 0 && (
                <>
                  <div className="hidden md:block">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead className="w-12">
                            <Checkbox
                              checked={candidates.length > 0 && selectedIds.length === candidates.length}
                              onChange={handleSelectAll}
                            />
                          </TableHead>
                          <TableHead>이름</TableHead>
                          <TableHead>학년</TableHead>
                          <TableHead>나이</TableHead>
                          <TableHead>코스명</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {candidates.map((candidate) => (
                          <TableRow key={candidate.id}>
                            <TableCell>
                              <Checkbox
                                checked={selectedIds.includes(candidate.id || "")}
                                onChange={() => handleSelectOne(candidate.id || "")}
                              />
                            </TableCell>
                            <TableCell className="font-medium">{candidate.name}</TableCell>
                            <TableCell>{candidate.grade}</TableCell>
                            <TableCell>{candidate.age}세</TableCell>
                            <TableCell className="text-sm text-gray-500">
                              {(candidate as any).courseName || "N/A"}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>

                  {/* Mobile 카드 */}
                  <div className="md:hidden space-y-3">
                    {candidates.map((candidate) => (
                      <div
                        key={candidate.id}
                        className="border rounded-lg p-4 space-y-3"
                      >
                        <div className="flex items-start gap-3">
                          <Checkbox
                            checked={selectedIds.includes(candidate.id || "")}
                            onChange={() => handleSelectOne(candidate.id || "")}
                          />
                          <div className="flex-1">
                            <div className="font-medium text-gray-900">{candidate.name}</div>
                            <div className="mt-1 text-sm text-gray-600">
                              {candidate.grade} · {candidate.age}세
                            </div>
                            <div className="mt-1 text-xs text-gray-500">
                              {(candidate as any).courseName || "N/A"}
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </div>
          </Card>
        </TabsContent>

        {/* 탭 2: 생성된 초대 */}
        <TabsContent value="invitations">
          <Card>
            <div className="p-6 space-y-4">
              {/* 필터 */}
              <div className="flex gap-2">
                <button
                  onClick={() => setStatusFilter("ALL")}
                  className={`px-3 py-1.5 text-sm rounded-lg transition ${
                    statusFilter === "ALL"
                      ? "bg-blue-50 text-blue-700 font-medium"
                      : "text-gray-600 hover:bg-gray-50"
                  }`}
                >
                  전체
                </button>
                <button
                  onClick={() => setStatusFilter("PENDING")}
                  className={`px-3 py-1.5 text-sm rounded-lg transition ${
                    statusFilter === "PENDING"
                      ? "bg-blue-50 text-blue-700 font-medium"
                      : "text-gray-600 hover:bg-gray-50"
                  }`}
                >
                  대기
                </button>
                <button
                  onClick={() => setStatusFilter("ACCEPTED")}
                  className={`px-3 py-1.5 text-sm rounded-lg transition ${
                    statusFilter === "ACCEPTED"
                      ? "bg-blue-50 text-blue-700 font-medium"
                      : "text-gray-600 hover:bg-gray-50"
                  }`}
                >
                  수락
                </button>
              </div>


              {/* 로딩 */}
              {invitationsLoading && <LoadingSkeleton rows={5} columns={4} />}

              {/* 에러 */}
              {invitationsError && (
                <div className="py-8 text-center space-y-3">
                  <p className="text-sm text-red-600">초대 목록을 불러오는데 실패했습니다.</p>
                  <Button variant="secondary" onClick={() => refetchInvitations()}>
                    재시도
                  </Button>
                </div>
              )}

              {/* 빈 상태 */}
              {!invitationsLoading && !invitationsError && invitations && invitations.length === 0 && (
                <EmptyState message="생성된 학생 초대가 없습니다." />
              )}

              {/* Desktop 테이블 */}
              {!invitationsLoading && !invitationsError && invitations && invitations.length > 0 && (
                <>
                  <div className="hidden md:block">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>학생 이름</TableHead>
                          <TableHead>상태</TableHead>
                          <TableHead className="text-right">액션</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {invitations.map((invitation, index) => (
                          <TableRow key={invitation.code || index}>
                            <TableCell className="font-medium">
                              {invitation.studentName || invitation.targetEmail || "N/A"}
                            </TableCell>
                            <TableCell>
                              <InvitationStatusBadge status={invitation.status as InvitationStatus} />
                            </TableCell>
                            <TableCell className="text-right">
                              {invitation.status === "PENDING" && invitation.code && (
                                <Button
                                  variant="secondary"
                                  onClick={() => handleCopyInvitationLink(invitation.code!)}
                                  className="h-8 px-3 text-sm"
                                >
                                  복사
                                </Button>
                              )}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>

                  {/* Mobile 카드 */}
                  <div className="md:hidden space-y-3">
                    {invitations.map((invitation, index) => (
                      <div
                        key={invitation.code || index}
                        className="border rounded-lg p-4 space-y-3"
                      >
                        <div className="flex items-start justify-between">
                          <div className="font-medium text-gray-900">
                            {invitation.studentName || invitation.targetEmail || "N/A"}
                          </div>
                          <InvitationStatusBadge status={invitation.status as InvitationStatus} />
                        </div>
                        {invitation.status === "PENDING" && invitation.code && (
                          <Button
                            variant="secondary"
                            onClick={() => handleCopyInvitationLink(invitation.code!)}
                            className="w-full"
                          >
                            복사
                          </Button>
                        )}
                      </div>
                    ))}
                  </div>
                </>
              )}
            </div>
          </Card>
        </TabsContent>
      </Tabs>
    </DashboardShell>
  );
}
