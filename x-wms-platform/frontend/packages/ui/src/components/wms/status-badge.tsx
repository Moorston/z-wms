import { Badge } from "../ui/badge";
import { statusColors } from "../../lib/utils";

interface StatusBadgeProps {
  status: string;
  label?: string;
}

const statusLabels: Record<string, string> = {
  CREATED: "已创建",
  PROCESSING: "处理中",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
  SHIPPED: "已发货",
  PENDING: "待处理",
  EXCEPTION: "异常",
  ALLOCATED: "已分配",
  PICKED: "已拣货",
  RECEIVED: "已收货",
  PUTAWAY: "已上架",
  QC_PASSED: "质检通过",
  QC_FAILED: "质检不通过",
};

export function StatusBadge({ status, label }: StatusBadgeProps) {
  const colorClass = statusColors[status] || "bg-gray-100 text-gray-800";
  const displayLabel = label || statusLabels[status] || status;
  return <Badge className={colorClass}>{displayLabel}</Badge>;
}
