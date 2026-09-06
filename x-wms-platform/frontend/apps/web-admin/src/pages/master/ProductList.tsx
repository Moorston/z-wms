import { useMemo } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, Button, Card, CardContent, StatusBadge } from "@xwms/ui";
import type { Product } from "@xwms/shared";
import { Plus, Search, Download } from "lucide-react";

const mockData: Product[] = Array.from({ length: 30 }, (_, i) => ({
  id: `P${1000 + i}`,
  sku: `SKU${String(1000 + i).padStart(4, "0")}`,
  productName: `商品${i + 1}`,
  category: ["电子产品", "日用品", "食品", "服装"][i % 4],
  unit: "件",
  weight: 0.5 + i * 0.1,
  volume: 0.001 + i * 0.0001,
  barcode: `690${String(100000000 + i).padStart(9, "0")}`,
  spec: `规格${i + 1}`,
  status: i % 5 === 0 ? "DISABLED" : "ACTIVE",
  ownerCode: "OWNER001",
  createdAt: "2026-08-01",
}));

export default function ProductList() {
  const columns = useMemo<ColumnDef<Product>[]>(
    () => [
      { accessorKey: "sku", header: "SKU", cell: ({ row }) => <span className="font-medium text-blue-600">{row.original.sku}</span> },
      { accessorKey: "productName", header: "商品名称" },
      { accessorKey: "category", header: "分类" },
      { accessorKey: "spec", header: "规格" },
      { accessorKey: "unit", header: "单位" },
      { accessorKey: "barcode", header: "条码" },
      { accessorKey: "weight", header: "重量(kg)" },
      { accessorKey: "status", header: "状态", cell: ({ row }) => <StatusBadge status={row.original.status} label={row.original.status === "ACTIVE" ? "启用" : "禁用"} /> },
      {
        id: "actions",
        header: "操作",
        cell: () => (
          <div className="flex gap-2">
            <Button variant="ghost" size="sm">编辑</Button>
            <Button variant="ghost" size="sm">禁用</Button>
          </div>
        ),
      },
    ],
    []
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-800">产品档案</h1>
        <div className="flex gap-2">
          <Button variant="outline" size="sm"><Download className="w-4 h-4 mr-1" />导入</Button>
          <Button size="sm"><Plus className="w-4 h-4 mr-1" />新建产品</Button>
        </div>
      </div>
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">SKU/名称</label>
              <input className="h-9 px-3 border border-slate-200 rounded-md text-sm w-48" placeholder="请输入" />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">分类</label>
              <select className="h-9 px-3 border border-slate-200 rounded-md text-sm w-36">
                <option value="">全部</option>
                <option>电子产品</option>
                <option>日用品</option>
                <option>食品</option>
                <option>服装</option>
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
