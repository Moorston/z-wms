import { useState } from "react";
import { ScanInput, Button, Card, CardContent, StatusBadge } from "@xwms/ui";
import { toast } from "sonner";
import { ClipboardList, MapPin, Package, CheckCircle, Navigation } from "lucide-react";

// Mock拣货任务
const mockTask = {
  waveNo: "WAVE20260818001",
  orderCount: 5,
  items: [
    { id: 1, sku: "SKU001", name: "商品A", qty: 10, picked: 0, location: "A-01-03", status: "pending" },
    { id: 2, sku: "SKU002", name: "商品B", qty: 5, picked: 0, location: "A-02-01", status: "pending" },
    { id: 3, sku: "SKU003", name: "商品C", qty: 8, picked: 0, location: "B-01-05", status: "pending" },
  ],
};

export default function Picking() {
  const [task, setTask] = useState(mockTask);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [scanSku, setScanSku] = useState("");
  const [qty, setQty] = useState("");

  const currentItem = task.items[currentIndex];

  const handleSkuScan = (value: string) => {
    setScanSku(value);
    if (value === currentItem.sku) {
      toast.success("商品匹配");
      setQty(String(currentItem.qty));
    } else {
      toast.error("商品不匹配，请重新扫描");
    }
  };

  const handleConfirm = () => {
    if (!scanSku || !qty) {
      toast.error("请扫描商品并确认数量");
      return;
    }
    const newItems = [...task.items];
    newItems[currentIndex] = { ...newItems[currentIndex], picked: parseInt(qty), status: "picked" };
    setTask({ ...task, items: newItems });
    toast.success(`拣货成功：${currentItem.sku} x ${qty}`);

    if (currentIndex < task.items.length - 1) {
      setTimeout(() => {
        setCurrentIndex(currentIndex + 1);
        setScanSku("");
        setQty("");
      }, 800);
    } else {
      toast.success("波次拣货完成！");
    }
  };

  const progress = (task.items.filter((i) => i.status === "picked").length / task.items.length) * 100;

  return (
    <div className="p-4 space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-slate-800">订单拣货</h2>
        <StatusBadge status="PROCESSING" label="拣货中" />
      </div>

      {/* 进度条 */}
      <div className="bg-white rounded-xl p-3">
        <div className="flex justify-between text-sm mb-2">
          <span className="text-slate-600">波次: {task.waveNo}</span>
          <span className="text-blue-600 font-medium">{Math.round(progress)}%</span>
        </div>
        <div className="h-2 bg-slate-200 rounded-full overflow-hidden">
          <div className="h-full bg-blue-500 transition-all" style={{ width: `${progress}%` }} />
        </div>
        <div className="text-xs text-slate-500 mt-1">
          已拣 {task.items.filter((i) => i.status === "picked").length} / {task.items.length}
        </div>
      </div>

      {/* 当前商品 */}
      <Card className="border-2 border-blue-500">
        <CardContent className="p-4 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-sm text-slate-500">当前位置 ({currentIndex + 1}/{task.items.length})</span>
            <div className="flex items-center gap-1 text-blue-600">
              <Navigation className="w-4 h-4" />
              <span className="font-medium">{currentItem.location}</span>
            </div>
          </div>
          <div className="text-xl font-bold text-slate-800">{currentItem.sku}</div>
          <div className="text-sm text-slate-600">{currentItem.name}</div>
          <div className="text-lg text-orange-600 font-bold">应拣: {currentItem.qty}</div>
        </CardContent>
      </Card>

      {/* 扫码 */}
      <Card>
        <CardContent className="p-4 space-y-3">
          <label className="text-sm font-medium text-slate-600">扫描商品条码</label>
          <ScanInput value={scanSku} onChange={setScanSku} onScan={handleSkuScan} placeholder="扫描SKU" />
        </CardContent>
      </Card>

      {/* 数量 */}
      <Card>
        <CardContent className="p-4 space-y-3">
          <label className="text-sm font-medium text-slate-600">拣货数量</label>
          <div className="flex items-center gap-3">
            <button onClick={() => setQty(String(Math.max(0, parseInt(qty || "0") - 1)))} className="w-12 h-12 bg-slate-100 rounded-lg text-2xl font-bold">
              -
            </button>
            <input type="number" value={qty} onChange={(e) => setQty(e.target.value)} className="flex-1 h-12 text-center text-xl font-bold border-2 border-slate-200 rounded-lg focus:border-blue-500 focus:outline-none" />
            <button onClick={() => setQty(String(parseInt(qty || "0") + 1))} className="w-12 h-12 bg-slate-100 rounded-lg text-2xl font-bold">
              +
            </button>
          </div>
        </CardContent>
      </Card>

      <Button onClick={handleConfirm} className="w-full h-14 text-lg">
        <CheckCircle className="w-5 h-5 mr-2" /> 确认拣货
      </Button>
    </div>
  );
}
