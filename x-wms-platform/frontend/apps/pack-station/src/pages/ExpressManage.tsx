import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, CardContent, DataTable } from "@xwms/ui";
import { ColumnDef } from "@tanstack/react-table";
import { toast } from "sonner";
import { ArrowLeft, Printer, Download, RefreshCw, CheckCircle } from "lucide-react";

const mockData = Array.from({ length: 20 }, (_, i) => ({
  id: i + 1,
  orderNo: `OUT202608${String(1800 + i).padStart(4, "0")}`,
  carrier: ["顺丰", "圆通", "中通", "韵达"][i % 4],
  expressNo: `SF${1000000000 + i}`,
  status: ["待打印", "已打印", "已发货"][i % 3],
  createdAt: "2026-08-18 10:00",
}));

export default function ExpressManage() {
  const navigate = useNavigate();
  const [selected, setSelected] = useState<number[]>([]);

  const columns = useMemo<ColumnDef<typeof mockData[0]>[]>(
    () => [
      {
        id: "select",
        header: ({ table }) => (
          <input
            type="checkbox"
            checked={table.getIsAllPageRowsSelected()}
            onChange={(e) => table.toggleAllPageRowsSelected(!!e.target.checked)}
          />
        ),
        cell: ({ row }) => (
          <input
            type="checkbox"
            checked={row.getIsSelected()}
            onChange={(e) => row.toggleSelected(!!e.target.checked)}
          />
        ),
      },
      { accessorKey: "orderNo", header: "订单号" },
      { accessorKey: "carrier", header: "快递公司" },
      { accessorKey: "expressNo", header: "快递单号" },
      { accessorKey: "status", header: "状态" },
      { accessorKey: "createdAt", header: "创建时间" },
    ],
    []
  );

  const handleBatchPrint = () => {
    if (selected.length === 0) {
      toast.error("请先选择要打印的订单");
      return;
    }
    toast.success(`已发送 ${selected.length} 张面单到打印机`);
  };

  return (
    <div className="h-screen flex flex-col bg-slate-100">
      <header className="h-14 bg-white border-b border-slate-200 flex items-center justify-between px-6">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate("/pack")} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <span className="text-lg font-bold text-slate-800">快递单管理</span>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm"><RefreshCw className="w-4 h-4 mr-1" />刷新</Button>
          <Button variant="outline" size="sm"><Download className="w-4 h-4 mr-1" />导出</Button>
          <Button size="sm" onClick={handleBatchPrint}><Printer className="w-4 h-4 mr-1" />批量打印 ({selected.length})</Button>
        </div>
      </header>
      <div className="flex-1 p-4 overflow-hidden">
        <Card className="h-full">
          <CardContent className="p-4 h-full overflow-hidden">
            <DataTable columns={columns} data={mockData} pageSize={15} />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
