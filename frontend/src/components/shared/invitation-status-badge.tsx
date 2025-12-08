import { Badge } from "@/components/ui/badge";

type InvitationStatus = "PENDING" | "ACCEPTED" | "REVOKED" | "EXPIRED";

type InvitationStatusBadgeProps = {
  status: InvitationStatus;
};

const statusConfig: Record<
  InvitationStatus,
  { variant: "default" | "success" | "secondary" | "destructive"; label: string }
> = {
  PENDING: { variant: "default", label: "대기 중" },
  ACCEPTED: { variant: "success", label: "수락됨" },
  REVOKED: { variant: "secondary", label: "취소됨" },
  EXPIRED: { variant: "destructive", label: "만료됨" }
};

export function InvitationStatusBadge({ status }: InvitationStatusBadgeProps) {
  const config = statusConfig[status];

  return <Badge variant={config.variant}>{config.label}</Badge>;
}
