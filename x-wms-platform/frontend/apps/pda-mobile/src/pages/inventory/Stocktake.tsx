import { useState } from "react";
import { ScanInput, Button, Card, CardContent } from "@xwms/ui";
import { toast } from "sonner";
import { ClipboardList, CheckCircle, AlertTriangle } from "lucide-react";

export default function Stocktake() {
  const [taskNo, setTaskNo] = useState("");
  const [location, setLocation] = useState("");
  const [sku, setSku] = useState("");
  const [actualQty, setActualQty] = useState("");
  const [items, setItems] = useState<any[]>([]);

  const handleTaskScan = (v: string) => { setTaskNo(v); toast.success(`盘点任务: ${v}`); };
  const handleLocationScan = (v: string) => { setLocation(v); toast.success(`库位: ${v}`); };
  const handleSkuScan = (v: string) => { setSku(v); };

  const handleAdd = () => {
    if (!location || !sku || !actualQty) {
      toast.error("请完整填写盘点信息");
      return;
    }
    const expected = Math.floor(Math.random() * 20) + 5;
    const diff = parseInt(actualQty) - expected;
    setItems([...items, { location, sku, expected, actual: parseInt(actualQty), diff }]);
    toast.success(`已记录: ${sku} 实盘 ${actualQty}`);
    setSku(""); setActualQty("");
  };

  const handleFinish = () => {
    const diffCount = items.filter((i) => i.diff !== 0).length;
    toast.success(`盘点完成，共 ${items.length} 项，差异 ${diffCount} 项`);
  };

  return (
    <div className="p-4 space-y-4">
      <h2 className="text-lg font-bold text-slate-800">库存盘点</h2>

      <Card>
        <CardContent className="p-4 space-y-3">
          <label className="text-sm font-medium text-slate-600">盘点任务号</label>
          <ScanInput value={taskNo} onChange={setTaskNo} onScan={handleTaskScan} placeholder="扫描任务条码" />
        </CardContent>
      </Card>

      {taskNo && (
        <>
          <Card>
            <CardContent className="p-4 space-y-3">
              <label className="text-sm font-medium text-slate-600">库位</label>
              <ScanInput value={location} onChange={setLocation} onScan={handleLocationScan} placeholder="扫描库位" autoFocus={false} />
            </CardContent>
          </Card>

          {location && (
            <>
              <Card>
                <CardContent className="p-4 space-y-3">
                  <label className="text-sm font-medium text-slate-600">商品条码</label>
                  <ScanInput value={sku} onChange={setSku} onScan={handleSkuScan} placeholder="扫描SKU" autoFocus={false} />
                </CardContent>
              </Card>

              {sku && (
                <>
                  <Card>
                    <CardContent className="p-4 space-y-3">
                      <label className="text-sm font-medium text-slate-600">实盘数量</label>
                      <div className="flex items-center gap-3">
                        <button onClick={() => setActualQty(String(Math.max(0, parseInt(actualQty || "0") - 1)))} className="w-12 h-12 bg-slate-100 rounded-lg text-2xl font-bold">-</button>
                        <input type="number" value={actualQty} onChange={(e) => setActualQty(e.target.value)} className="flex-1 h-12 text-center text-xl font-bold border-2 border-slate-200 rounded-lg focus:border-blue-500 focus:outline-none" placeholder="0" />
                        <button onClick={() => setActualQty(String(parseInt(actualQty || "0") + 1))} className="w-12 h-12 bg-slate-100 rounded-lg text-2xl font-bold">+</button>
                      </div>
                    </CardContent>
                  </Card>
                  <Button onClick={handleAdd} className="w-full h-12 text-base">添加盘点记录</Button>
                </>
              )}
            </>
          )}

          {items.length > 0 && (
            <Card>
              <CardContent className="p-4 space-y-2">
                <div className="text-sm font-medium text-slate-600 mb-2">已盘点 {items.length} 项</div>
                {items.map((item, idx) => (
                  <div key={idx} className="flex items-center justify-between py-2 border-b border-slate-100 last:border-0">
                    <div>
                      <div className="text-sm font-medium">{item.sku}</div>
                      <div className="text-xs text-slate-500">{item.location}</div>
                    </div>
                    <div className="text-right">
                      <div className="text-sm">账:{item.expected} 实:{item.actual}</div>
                      {item.diff !== 0 ? (
                        <span className="text-xs text-red-600 flex items-center gap-1"><AlertTriangle className="w-3 h-3" /> 差异 {item.diff > 0 ? "+" : ""}{item.diff}</span>
                      ) : (
                        <span className="text-xs text-green-600 flex items-center gap-1"><CheckCircle className="w-3 h-3" /> 一致</span>
                      )}
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}

          {items.length > 0 && (
            <Button onClick={handleFinish} className="w-full h-14 text-lg">
              <ClipboardList className="w-5 h-5 mr-2" /> 完成盘点
            </Button>
          )}
        </>
      )}
    </div>
  );
}
