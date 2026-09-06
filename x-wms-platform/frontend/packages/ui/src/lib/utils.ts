import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * 合并Tailwind CSS类名，解决类名冲突
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * 格式化日期时间
 */
export function formatDateTime(date: string | Date): string {
  const d = new Date(date);
  return d.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * 状态颜色映射（Tailwind类名）
 */
export const statusColors: Record<string, string> = {
  CREATED: "bg-blue-100 text-blue-800",
  PROCESSING: "bg-orange-100 text-orange-800",
  COMPLETED: "bg-green-100 text-green-800",
  CANCELLED: "bg-gray-100 text-gray-800",
  SHIPPED: "bg-emerald-100 text-emerald-800",
  PENDING: "bg-yellow-100 text-yellow-800",
  EXCEPTION: "bg-red-100 text-red-800",
  ALLOCATED: "bg-cyan-100 text-cyan-800",
  PICKED: "bg-teal-100 text-teal-800",
};
