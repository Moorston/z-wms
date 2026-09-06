import { useState } from "react";
import { ScanInput, Button, Card, CardContent } from "@xwms/ui";
import { toast } from "sonner";
import { ArrowRight, MapPin, CheckCircle } from "lucide-react";

export default function Move() {
  const [fromLocation, setFromLocation] = useState("");
  const [sku, setSku] = useState("");
  const [toLocation, setToLocation] = useState("");
  const [qty, setQty] = useState("");
  const [done, setDone] = useState(false);

  const handleFromScan = (v: string) => { setFromLocation(v); toast.success(`源库位: ${v}`); };
  const handleSkuScan = (v: string) => { setSku(v); toast.success(`商品: ${v}`); };
  const handleToScan = (v: string) => { setToLocation(v); toast.success(`目标库位: ${v}`); };

  const handleConfirm = () => {
    if (!fromLocation || !sku || !toLocation || !qty) {
      toast.error("请完整填写移库信息");
      return;
    }
    setDone(true);
    toast.success(`移库成功: ${sku} x ${qty} 从 ${fromLocation} 到 ${toLocation}`);
    setTimeout(() => {
      setFromLocation(""); setSku(""); setToLocation(""); setQty(""); setDone(false);
    }, 1500);
  };

  return (
    <div className="p-4 space-y-4">
      <h2 className="text-lg font-bold text-slate-800">库存移库</h2>

      <Card>
        <CardContent className="p-4 space-y-3">
          <label className="text-sm font-medium text-slate-600 flex items-center gap-2"><MapPin className="w-4 h-4" /> 源库位</label>
          <ScanInput value={fromLocation} onChange={setFromLocation} onScan={handleFromScan} placeholder="扫描源库位" />
        </CardContent>
      </Card>

      {fromLocation && (
        <Card>
          <CardContent className="p-4 space-y-3">
            <label className="text-sm font-medium text-slate-600">商品条码</label>
            <ScanInput value={sku} onChange={setSku} onScan={handleSkuScan} placeholder="扫描SKU" autoFocus={false} />
          </CardContent>
        </Card>
      )}

      {sku && (
        <Card>
          <CardContent className="p-4 space-y-3">
            <label className="text-sm font-medium text-slate-600 flex items-center gap-2"><ArrowRight className="w-4 h-4" /> 目标库位</label>
            <ScanInput value={toLocation} onChange={setToLocation} onScan={handleToScan} placeholder="扫描目标库位" autoFocus={false} />
          </CardContent>
        </Card>
      )}

      {toLocation && (
        <>
          <Card>
            <CardContent className="p-4 space-y-3">
              <label className="text-sm font-medium text-slate-600">移库数量</label>
              <div className="flex items-center gap-3">
                <button onClick={() => setQty(String(Math.max(0, parseInt(qty || "0") - 1)))} className="w-12 h-12 bg-slate-100 rounded-lg text-2xl font-bold">-</button>
                <input type="number" value={qty} onChange={(e) => setQty(e.target.value)} className="flex-1 h-12 text-center text-xl font-bold border-2 border-slate-200 rounded-lg focus:border-blue-500 focus:outline-none" placeholder="0" />
                <button onClick={() => setQty(String(parseInt(qty || "0") + 1))} className="w-12 h-12 bg-slate-100 rounded-lg text-2xl font-bold">+</button>
              </div>
            </CardContent>
          </Card>
          <Button onClick={handleConfirm} className="w-full h-14 text-lg" disabled={done}>
            {done ? <span className="flex items-center gap-2"><CheckCircle className="w-5 h-5" /> 移库成功</span> : "确认移库"}
          </Button>
        </>
      )}
    </div>
  );
}
