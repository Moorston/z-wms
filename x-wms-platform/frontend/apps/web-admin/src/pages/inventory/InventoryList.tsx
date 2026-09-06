import { useMemo } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, Button, Card, CardContent, Badge } from "@xwms/ui";
import type { Inventory } from "@xwms/shared";
import { Search, Download, RefreshCw } from "lucide-react";

const mockData: Inventory[] = Array.from({ length: 50 }, (_, i) => ({
  id: `INV${1000 + i}`,
  ownerCode: "OWNER001",
  warehouseCode: "WH001",
  locationCode: `A-${String(1 + (i % 10)).padStart(2, "0")}-${String(1 + Math.floor(i / 10)).padStart(2, "0")}`,
  sku: `SKU${String(1000 + i).padStart(4, "0")}`,
  productName: `商品${i + 1}`,
  batchNo: `BATCH202608${String(i).padStart(3, "0")}`,
  qty: 100 + i * 5,
  availableQty: 80 + i * 4,
  allocatedQty: 20 + i,
  frozenQty: i % 5 === 0 ? 10 : 0,
  unit: "件",
  updatedAt: "2026-08-18 16:00:00",
}));

export default function InventoryList() {
  const columns = useMemo<ColumnDef<Inventory>[]>(
    () => [
      { accessorKey: "sku", header: "SKU", cell: ({ row }) => <span className="font-medium text-blue-600">{row.original.sku}</span> },
      { accessorKey: "productName", header: "商品名称" },
      { accessorKey: "locationCode", header: "库位" },
      { accessorKey: "batchNo", header: "批次" },
      { accessorKey: "qty", header: "总库存" },
      { accessorKey: "availableQty", header: "可用", cell: ({ row }) => <span className="text-green-600 font-medium">{row.original.availableQty}</span> },
      { accessorKey: "allocatedQty", header: "已分配", cell: ({ row }) => <span className="text-orange-600">{row.original.allocatedQty}</span> },
      { accessorKey: "frozenQty", header: "冻结", cell: ({ row }) => row.original.frozenQty > 0 ? <span className="text-red-600">{row.original.frozenQty}</span> : "-" },
      { accessorKey: "unit", header: "单位" },
      {
        id: "actions",
        header: "操作",
        cell: () => (
          <div className="flex gap-2">
            <Button variant="ghost" size="sm">详情</Button>
            <Button variant="ghost" size="sm">移动</Button>
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
        <h1 className="text-2xl font-bold text-slate-800">库存查询</h1>
        <div className="flex gap-2">
          <Button variant="outline" size="sm"><RefreshCw className="w-4 h-4 mr-1" />刷新</Button>
          <Button variant="outline" size="sm"><Download className="w-4 h-4 mr-1" />导出</Button>
        </div>
      </div>
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">SKU/商品</label>
              <input className="h-9 px-3 border border-slate-200 rounded-md text-sm w-48" placeholder="请输入" />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">库位</label>
              <input className="h-9 px-3 border border-slate-200 rounded-md text-sm w-36" placeholder="请输入" />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">批次</label>
              <input className="h-9 px-3 border border-slate-200 rounded-md text-sm w-36" placeholder="请输入" />
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
