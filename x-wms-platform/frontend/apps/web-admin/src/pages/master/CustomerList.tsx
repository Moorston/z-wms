import { useMemo } from "react";
import { ColumnDef } from "@tanstack/react-table";
import { DataTable, Button, Card, CardContent, StatusBadge } from "@xwms/ui";
import type { Customer } from "@xwms/shared";
import { Plus, Search } from "lucide-react";

const mockData: Customer[] = Array.from({ length: 20 }, (_, i) => ({
  id: `C${1000 + i}`,
  customerCode: `CUST${String(100 + i).padStart(3, "0")}`,
  customerName: `客户${i + 1}有限公司`,
  contact: `联系人${i + 1}`,
  phone: `138${String(10000000 + i).padStart(8, "0")}`,
  address: `广东省广州市XX区XX路${i + 1}号`,
  status: i % 4 === 0 ? "DISABLED" : "ACTIVE",
  ownerCode: "OWNER001",
}));

export default function CustomerList() {
  const columns = useMemo<ColumnDef<Customer>[]>(
    () => [
      { accessorKey: "customerCode", header: "客户编码", cell: ({ row }) => <span className="font-medium text-blue-600">{row.original.customerCode}</span> },
      { accessorKey: "customerName", header: "客户名称" },
      { accessorKey: "contact", header: "联系人" },
      { accessorKey: "phone", header: "联系电话" },
      { accessorKey: "address", header: "地址" },
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
        <h1 className="text-2xl font-bold text-slate-800">客户档案</h1>
        <Button size="sm"><Plus className="w-4 h-4 mr-1" />新建客户</Button>
      </div>
      <Card>
        <CardContent className="p-4">
          <div className="flex flex-wrap gap-4 items-end">
            <div className="flex flex-col gap-1">
              <label className="text-sm text-slate-500">客户编码/名称</label>
              <input className="h-9 px-3 border border-slate-200 rounded-md text-sm w-48" placeholder="请输入" />
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
