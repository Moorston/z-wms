import { useState } from "react";
import { ScanInput, Button, Card, CardContent } from "@xwms/ui";
import { toast } from "sonner";
import { ClipboardCheck, CheckCircle, XCircle } from "lucide-react";

export default function Review() {
  const [orderNo, setOrderNo] = useState("");
  const [scanSku, setScanSku] = useState("");
  const [items, setItems] = useState([
    { sku: "SKU001", name: "商品A", expected: 10, scanned: 0 },
    { sku: "SKU002", name: "商品B", expected: 5, scanned: 0 },
  ]);

  const handleOrderScan = (value: string) => {
    setOrderNo(value);
    toast.success(`订单 ${value} 已加载`);
  };

  const handleSkuScan = (value: string) => {
    setScanSku(value);
    const idx = items.findIndex((i) => i.sku === value);
    if (idx >= 0) {
      const newItems = [...items];
      newItems[idx] = { ...newItems[idx], scanned: newItems[idx].scanned + 1 };
      setItems(newItems);
      toast.success(`${value} 已扫描 (${newItems[idx].scanned}/${newItems[idx].expected})`);
    } else {
      toast.error(`商品 ${value} 不在订单中`);
    }
    setScanSku("");
  };

  const allDone = items.every((i) => i.scanned === i.expected);

  return (
    <div className="p-4 space-y-4">
      <h2 className="text-lg font-bold text-slate-800">出库复核</h2>

      <Card>
        <CardContent className="p-4 space-y-3">
          <label className="text-sm font-medium text-slate-600">扫描订单号/面单号</label>
          <ScanInput value={orderNo} onChange={setOrderNo} onScan={handleOrderScan} placeholder="扫描订单条码" />
        </CardContent>
      </Card>

      {orderNo && (
        <>
          <Card>
            <CardContent className="p-4 space-y-3">
              <label className="text-sm font-medium text-slate-600">扫描商品条码</label>
              <ScanInput value={scanSku} onChange={setScanSku} onScan={handleSkuScan} placeholder="扫描SKU" autoFocus={false} />
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4 space-y-2">
              <div className="text-sm font-medium text-slate-600 mb-2">复核明细</div>
              {items.map((item) => (
                <div key={item.sku} className="flex items-center justify-between py-2 border-b border-slate-100 last:border-0">
                  <div>
                    <div className="text-sm font-medium">{item.sku}</div>
                    <div className="text-xs text-slate-500">{item.name}</div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={`text-sm font-bold ${item.scanned === item.expected ? "text-green-600" : "text-orange-600"}`}>
                      {item.scanned}/{item.expected}
                    </span>
                    {item.scanned === item.expected ? (
                      <CheckCircle className="w-5 h-5 text-green-500" />
                    ) : (
                      <XCircle className="w-5 h-5 text-slate-300" />
                    )}
                  </div>
                </div>
              ))}
            </CardContent>
          </Card>

          <Button onClick={() => toast.success("复核通过")} className="w-full h-14 text-lg" disabled={!allDone}>
            <ClipboardCheck className="w-5 h-5 mr-2" /> {allDone ? "确认复核通过" : "请完成全部商品扫描"}
          </Button>
        </>
      )}
    </div>
  );
}
