import { useState } from "react";
import { ScanInput, Button, Card, CardContent } from "@xwms/ui";
import { toast } from "sonner";
import { Package, CheckCircle, AlertCircle } from "lucide-react";

export default function Receive() {
  const [asnNo, setAsnNo] = useState("");
  const [sku, setSku] = useState("");
  const [qty, setQty] = useState("");
  const [received, setReceived] = useState(false);

  const handleAsnScan = (value: string) => {
    setAsnNo(value);
    toast.success(`ASN ${value} 已加载`);
  };

  const handleSkuScan = (value: string) => {
    setSku(value);
  };

  const handleConfirm = () => {
    if (!asnNo || !sku || !qty) {
      toast.error("请完整填写收货信息");
      return;
    }
    setReceived(true);
    toast.success(`收货成功：${sku} x ${qty}`);
    setTimeout(() => {
      setSku("");
      setQty("");
      setReceived(false);
    }, 1500);
  };

  return (
    <div className="p-4 space-y-4">
      <h2 className="text-lg font-bold text-slate-800">标准收货</h2>

      {/* ASN扫码 */}
      <Card>
        <CardContent className="p-4 space-y-3">
          <label className="text-sm font-medium text-slate-600">扫描ASN单号</label>
          <ScanInput value={asnNo} onChange={setAsnNo} onScan={handleAsnScan} placeholder="扫描ASN条码" />
        </CardContent>
      </Card>

      {asnNo && (
        <>
          {/* 商品扫码 */}
          <Card>
            <CardContent className="p-4 space-y-3">
              <label className="text-sm font-medium text-slate-600">扫描商品条码</label>
              <ScanInput value={sku} onChange={setSku} onScan={handleSkuScan} placeholder="扫描SKU条码" autoFocus={false} />
            </CardContent>
          </Card>

          {/* 数量输入 */}
          <Card>
            <CardContent className="p-4 space-y-3">
              <label className="text-sm font-medium text-slate-600">收货数量</label>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => setQty(String(Math.max(0, parseInt(qty || "0") - 1)))}
                  className="w-12 h-12 bg-slate-100 rounded-lg text-2xl font-bold text-slate-600 active:bg-slate-200"
                >
                  -
                </button>
                <input
                  type="number"
                  value={qty}
                  onChange={(e) => setQty(e.target.value)}
                  className="flex-1 h-12 text-center text-xl font-bold border-2 border-slate-200 rounded-lg focus:border-blue-500 focus:outline-none"
                  placeholder="0"
                />
                <button
                  onClick={() => setQty(String(parseInt(qty || "0") + 1))}
                  className="w-12 h-12 bg-slate-100 rounded-lg text-2xl font-bold text-slate-600 active:bg-slate-200"
                >
                  +
                </button>
              </div>
            </CardContent>
          </Card>

          {/* 确认按钮 */}
          <Button
            onClick={handleConfirm}
            className="w-full h-14 text-lg"
            disabled={received}
          >
            {received ? (
              <span className="flex items-center gap-2"><CheckCircle className="w-5 h-5" /> 收货成功</span>
            ) : (
              <span className="flex items-center gap-2"><Package className="w-5 h-5" /> 确认收货</span>
            )}
          </Button>
        </>
      )}
    </div>
  );
}
