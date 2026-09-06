import { useMemo } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, Button, Card, CardContent, StatusBadge } from "@xwms/ui";
import type { Location } from "@xwms/shared";
import { Plus, Search, Grid3x3 } from "lucide-react";

const mockData: Location[] = Array.from({ length: 50 }, (_, i) => {
  const lane = String.fromCharCode(65 + (i % 5));
  const row = String(Math.floor(i / 5) + 1).padStart(2, "0");
  return {
    id: `L${1000 + i}`,
    locationCode: `${lane}-${row}-01`,
    warehouseCode: "WH001",
    areaCode: `AREA${lane}`,
    locationType: ["STORAGE", "PICKING", "RECEIVING", "SHIPPING"][i % 4],
    status: ["EMPTY", "OCCUPIED", "FROZEN", "DISABLED"][i % 4],
    capacity: 100,
    usedCapacity: (i % 4) * 25,
    lane,
    row,
    column: "01",
    level: "01",
  };
});

export default function LocationList() {
  const columns = useMemo<ColumnDef<Location>[]>(
    () => [
      { accessorKey: "locationCode", header: "库位编码", cell: ({ row }) => <span className="font-medium text-blue-600">{row.original.locationCode}</span> },
      { accessorKey: "areaCode", header: "库区" },
      { accessorKey: "locationType", header: "类型", cell: ({ row }) => <span className="px-2 py-0.5 rounded text-xs bg-slate-100">{row.original.locationType}</span> },
      { accessorKey: "status", header: "状态", cell: ({ row }) => <StatusBadge status={row.original.status} label={{ EMPTY: "空闲", OCCUPIED: "占用", FROZEN: "冻结", DISABLED: "禁用" }[row.original.status]} /> },
      { accessorKey: "capacity", header: "容量" },
      { accessorKey: "usedCapacity", header: "已用" },
      {
        id: "usage",
        header: "使用率",
        cell: ({ row }) => {
          const pct = (row.original.usedCapacity! / row.original.capacity!) * 100;
          return (
            <div className="flex items-center gap-2">
              <div className="w-20 h-2 bg-slate-200 rounded-full overflow-hidden">
                <div className={`h-full ${pct > 80 ? "bg-red-500" : pct > 50 ? "bg-orange-500" : "bg-green-500"}`} style={{ width: `${pct}%` }} />
              </div>
              <span className="text-xs text-slate-500">{pct.toFixed(0)}%</span>
            </div>
          );
        },
      },
      {
        id: "actions",
        header: "操作",
        cell: () => (
          <div className="flex gap-2">
            <Button variant="ghost" size="sm">编辑</Button>
            <Button variant="ghost" size="sm">冻结</Button>
          </div>
        ),
      },
    ],
    []
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-800">库位管理</h1>
        <div className="flex gap-2">
          <Button variant="outline" size="sm"><Grid3x3 className="w-4 h-4 mr-1" />图形化</Button>
          <Button size="sm"><Plus className="w-4 h-4 mr-1" />新建库位</Button>
        </div>
      </div>
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">库位编码</label>
              <input className="h-9 px-3 border border-slate-200 rounded-md text-sm w-40" placeholder="请输入" />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">库区</label>
              <select className="h-9 px-3 border border-slate-200 rounded-md text-sm w-32">
                <option value="">全部</option>
                <option>AREAA</option>
                <option>AREAB</option>
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">状态</label>
              <select className="h-9 px-3 border border-slate-200 rounded-md text-sm w-32">
                <option value="">全部</option>
                <option value="EMPTY">空闲</option>
                <option value="OCCUPIED">占用</option>
                <option value="FROZEN">冻结</option>
              </select>
            </div>
            <Button size="sm"><Search className="w-4 h-4 mr-1" />查询</Button>
            <Button variant="outline" size="sm">重置</Button>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent className="p-4">
          <DataTable columns={columns} data={mockData} pageSize={20} />
        </CardContent>
      </Card>
    </div>
  );
}
