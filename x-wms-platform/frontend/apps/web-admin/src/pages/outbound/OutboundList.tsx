import { useMemo } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, StatusBadge, Button, Card, CardContent } from "@xwms/ui";
import type { OutboundOrder } from "@xwms/shared";
import { Plus, Search, Download } from "lucide-react";

const mockData: OutboundOrder[] = Array.from({ length: 50 }, (_, i) => ({
  id: `OB${1000 + i}`,
  orderNo: `OUT202608${String(1800 + i).padStart(4, "0")}`,
  orderType: i % 3 === 0 ? "SO" : i % 3 === 1 ? "TRANSFER" : "CROSSDOCK",
  status: ["CREATED", "ALLOCATED", "PICKED", "SHIPPED"][i % 4],
  ownerCode: "OWNER001",
  warehouseCode: "WH001",
  customerCode: `CUST${String(100 + (i % 10)).padStart(3, "0")}`,
  expectedShipDate: "2026-08-19",
  totalQty: 50 + i * 5,
  allocatedQty: i % 4 === 0 ? 0 : 40 + i * 3,
  pickedQty: i % 4 < 2 ? 0 : 30 + i * 2,
  shippedQty: i % 4 < 3 ? 0 : 50 + i * 5,
  carrier: i % 2 === 0 ? "SF" : "YTO",
  createdBy: "admin",
  createdAt: "2026-08-18 09:00:00",
  updatedAt: "2026-08-18 15:00:00",
}));

export default function OutboundList() {
  const columns = useMemo<ColumnDef<OutboundOrder>[]>(
    () => [
      { accessorKey: "orderNo", header: "出库单号", cell: ({ row }) => <span className="font-medium text-blue-600">{row.original.orderNo}</span> },
      { accessorKey: "orderType", header: "类型", cell: ({ row }) => <span className="px-2 py-0.5 rounded text-xs bg-slate-100">{row.original.orderType}</span> },
      { accessorKey: "status", header: "状态", cell: ({ row }) => <StatusBadge status={row.original.status} /> },
      { accessorKey: "customerCode", header: "客户" },
      { accessorKey: "totalQty", header: "应发数量" },
      { accessorKey: "pickedQty", header: "已拣数量" },
      { accessorKey: "shippedQty", header: "已发数量" },
      { accessorKey: "carrier", header: "快递" },
      { accessorKey: "expectedShipDate", header: "预计发货" },
      {
        id: "actions",
        header: "操作",
        cell: () => (
          <div className="flex gap-2">
            <Button variant="ghost" size="sm">详情</Button>
            <Button variant="ghost" size="sm">分配</Button>
          </div>
        ),
      },
    ],
    []
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-800">出库管理</h1>
        <div className="flex gap-2">
          <Button variant="outline" size="sm"><Download className="w-4 h-4 mr-1" />导出</Button>
          <Button size="sm"><Plus className="w-4 h-4 mr-1" />新建出库单</Button>
        </div>
      </div>
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">出库单号</label>
              <input className="h-9 px-3 border border-slate-200 rounded-md text-sm w-48" placeholder="请输入" />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">状态</label>
              <select className="h-9 px-3 border border-slate-200 rounded-md text-sm w-36">
                <option value="">全部</option>
                <option value="CREATED">已创建</option>
                <option value="ALLOCATED">已分配</option>
                <option value="PICKED">已拣货</option>
                <option value="SHIPPED">已发货</option>
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
