import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { cn } from "../../lib/utils";

interface KpiCardProps {
  title: string;
  value: string | number;
  unit?: string;
  trend?: number; // 百分比，正数上升，负数下降
  icon?: React.ReactNode;
  color?: "blue" | "green" | "orange" | "red" | "purple";
  description?: string;
}

const colorMap = {
  blue: "text-blue-600 bg-blue-50",
  green: "text-green-600 bg-green-50",
  orange: "text-orange-600 bg-orange-50",
  red: "text-red-600 bg-red-50",
  purple: "text-purple-600 bg-purple-50",
};

export function KpiCard({ title, value, unit, trend, icon, color = "blue", description }: KpiCardProps) {
  return (
    <Card className="overflow-hidden hover:shadow-md transition-shadow">
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">{title}</CardTitle>
        {icon && <div className={cn("p-2 rounded-lg", colorMap[color])}>{icon}</div>}
      </CardHeader>
      <CardContent>
        <div className="flex items-baseline gap-1">
          <span className="text-3xl font-bold">{value}</span>
          {unit && <span className="text-sm text-muted-foreground">{unit}</span>}
        </div>
        {trend !== undefined && (
          <div className="flex items-center gap-1 mt-1">
            <span className={cn("text-xs font-medium", trend >= 0 ? "text-green-600" : "text-red-600")}>
              {trend >= 0 ? "↑" : "↓"} {Math.abs(trend)}%
            </span>
            <span className="text-xs text-muted-foreground">较昨日</span>
          </div>
        )}
        {description && <p className="text-xs text-muted-foreground mt-1">{description}</p>}
      </CardContent>
    </Card>
  );
}
