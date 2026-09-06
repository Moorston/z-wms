import { useMemo } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, StatusBadge, Button, Card, CardContent } from "@xwms/ui";
import type { InboundOrder } from "@xwms/shared";
import { Plus, Search, Download } from "lucide-react";

// Mock数据
const mockData: InboundOrder[] = Array.from({ length: 50 }, (_, i) => ({
  id: `IB${1000 + i}`,
  orderNo: `IN202608${String(1800 + i).padStart(4, "0")}`,
  orderType: i % 3 === 0 ? "PO" : i % 3 === 1 ? "ASN" : "RETURN",
  status: ["CREATED", "RECEIVED", "PUTAWAY", "COMPLETED"][i % 4],
  ownerCode: "OWNER001",
  warehouseCode: "WH001",
  expectedDate: "2026-08-18",
  totalQty: 100 + i * 10,
  receivedQty: i % 4 === 0 ? 0 : 80 + i * 5,
  putawayQty: i % 4 < 2 ? 0 : 60 + i * 3,
  createdBy: "admin",
  createdAt: "2026-08-18 10:00:00",
  updatedAt: "2026-08-18 14:00:00",
}));

export default function InboundList() {
  const columns = useMemo<ColumnDef<InboundOrder>[]>(
    () => [
      { accessorKey: "orderNo", header: "入库单号", cell: ({ row }) => <span className="font-medium text-blue-600">{row.original.orderNo}</span> },
      { accessorKey: "orderType", header: "类型", cell: ({ row }) => <span className="px-2 py-0.5 rounded text-xs bg-slate-100">{row.original.orderType}</span> },
      { accessorKey: "status", header: "状态", cell: ({ row }) => <StatusBadge status={row.original.status} /> },
      { accessorKey: "totalQty", header: "应收数量" },
      { accessorKey: "receivedQty", header: "实收数量" },
      { accessorKey: "putawayQty", header: "已上架" },
      { accessorKey: "expectedDate", header: "预计到货" },
      { accessorKey: "createdAt", header: "创建时间" },
      {
        id: "actions",
        header: "操作",
        cell: () => (
          <div className="flex gap-2">
            <Button variant="ghost" size="sm">详情</Button>
            <Button variant="ghost" size="sm">编辑</Button>
          </div>
        ),
      },
    ],
    []
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-800">入库管理</h1>
        <div className="flex gap-2">
          <Button variant="outline" size="sm"><Download className="w-4 h-4 mr-1" />导出</Button>
          <Button size="sm"><Plus className="w-4 h-4 mr-1" />新建入库单</Button>
        </div>
      </div>

      {/* 筛选区 */}
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">入库单号</label>
              <input className="h-9 px-3 border border-slate-200 rounded-md text-sm w-48" placeholder="请输入" />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">状态</label>
              <select className="h-9 px-3 border border-slate-200 rounded-md text-sm w-36">
                <option value="">全部</option>
                <option value="CREATED">已创建</option>
                <option value="RECEIVED">已收货</option>
                <option value="PUTAWAY">已上架</option>
                <option value="COMPLETED">已完成</option>
              </select>
            </div>
            <Button size="sm"><Search className="w-4 h-4 mr-1" />查询</Button>
            <Button variant="outline" size="sm">重置</Button>
          </div>
        </CardContent>
      </Card>

      {/* 数据表格 */}
      <Card>
        <CardContent className="p-4">
          <DataTable columns={columns} data={mockData} pageSize={20} />
        </CardContent>
      </Card>
    </div>
  );
}
