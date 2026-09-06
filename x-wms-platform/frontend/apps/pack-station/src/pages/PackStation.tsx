import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { ScanInput, Button, Card, CardContent, StatusBadge } from "@xwms/ui";
import { toast } from "sonner";
import {
  Package,
  Scale,
  Printer,
  CheckCircle,
  XCircle,
  Settings,
  FileText,
  User,
  LogOut,
  RefreshCw,
} from "lucide-react";

// Mock订单数据
const mockOrder = {
  orderNo: "OUT20260818001",
  customer: "某某科技有限公司",
  carrier: "顺丰速运",
  expressNo: "SF1234567890",
  items: [
    { sku: "SKU001", name: "商品A", qty: 2, scanned: 2, weight: 0.5 },
    { sku: "SKU002", name: "商品B", qty: 1, scanned: 1, weight: 1.2 },
    { sku: "SKU003", name: "商品C", qty: 3, scanned: 2, weight: 0.3 },
  ],
  totalWeight: 2.3,
};

export default function PackStation() {
  const navigate = useNavigate();
  const [orderNo, setOrderNo] = useState("");
  const [order, setOrder] = useState<any>(null);
  const [scanSku, setScanSku] = useState("");
  const [weight, setWeight] = useState(0);
  const [scaleConnected, setScaleConnected] = useState(false);
  const [packing, setPacking] = useState(false);

  // 模拟电子秤实时读数
  useEffect(() => {
    if (!scaleConnected) return;
    const timer = setInterval(() => {
      setWeight(2.3 + Math.random() * 0.05);
    }, 500);
    return () => clearInterval(timer);
  }, [scaleConnected]);

  const handleOrderScan = (value: string) => {
    setOrderNo(value);
    setOrder(mockOrder);
    toast.success(`订单 ${value} 已加载`);
  };

  const handleSkuScan = (value: string) => {
    if (!order) return;
    const idx = order.items.findIndex((i: any) => i.sku === value);
    if (idx >= 0) {
      const newItems = [...order.items];
      if (newItems[idx].scanned < newItems[idx].qty) {
        newItems[idx] = { ...newItems[idx], scanned: newItems[idx].scanned + 1 };
        setOrder({ ...order, items: newItems });
        toast.success(`${value} 已扫描 (${newItems[idx].scanned}/${newItems[idx].qty})`);
      } else {
        toast.warning(`${value} 已扫描完毕`);
      }
    } else {
      toast.error(`商品 ${value} 不在订单中`);
    }
    setScanSku("");
  };

  const allScanned = order?.items.every((i: any) => i.scanned === i.qty);
  const weightDiff = Math.abs(weight - (order?.totalWeight || 0));
  const weightOk = weightDiff < 0.1;

  const handlePrint = () => {
    if (!allScanned) {
      toast.error("请先完成全部商品扫描");
      return;
    }
    toast.success("快递面单已发送到打印机");
  };

  const handleComplete = () => {
    if (!allScanned || !weightOk) {
      toast.error("请确认商品扫描和重量校验");
      return;
    }
    setPacking(true);
    toast.success("打包完成，已通知发货");
    setTimeout(() => {
      setOrder(null);
      setOrderNo("");
      setScanSku("");
      setWeight(0);
      setPacking(false);
    }, 1500);
  };

  return (
    <div className="h-screen flex flex-col bg-slate-100">
      {/* 顶部栏 */}
      <header className="h-14 bg-white border-b border-slate-200 flex items-center justify-between px-6">
        <div className="flex items-center gap-3">
          <Package className="w-6 h-6 text-blue-600" />
          <span className="text-lg font-bold text-slate-800">X WMS 打包台</span>
          <span className="text-sm text-slate-400">|</span>
          <span className="text-sm text-slate-500">打包台 #01</span>
        </div>
        <div className="flex items-center gap-4">
          <button
            onClick={() => setScaleConnected(!scaleConnected)}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-md text-sm ${scaleConnected ? "bg-green-50 text-green-700" : "bg-slate-100 text-slate-500"}`}
          >
            <Scale className="w-4 h-4" />
            {scaleConnected ? "电子秤已连接" : "连接电子秤"}
          </button>
          <button onClick={() => navigate("/express")} className="flex items-center gap-2 px-3 py-1.5 rounded-md text-sm bg-slate-100 text-slate-600 hover:bg-slate-200">
            <FileText className="w-4 h-4" /> 快递单
          </button>
          <button onClick={() => navigate("/settings")} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded">
            <Settings className="w-5 h-5" />
          </button>
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center">
              <User className="w-4 h-4 text-blue-600" />
            </div>
            <span className="text-sm text-slate-700">打包员A</span>
          </div>
        </div>
      </header>

      {/* 主内容 */}
      <div className="flex-1 p-4 grid grid-cols-3 gap-4 overflow-hidden">
        {/* 左侧：订单信息 + 扫码 */}
        <div className="col-span-2 flex flex-col gap-4 overflow-hidden">
          {/* 订单扫码 */}
          <Card className="shrink-0">
            <CardContent className="p-4">
              <label className="text-sm font-medium text-slate-600 mb-2 block">扫描订单号/面单号</label>
              <ScanInput value={orderNo} onChange={setOrderNo} onScan={handleOrderScan} placeholder="扫描订单条码开始打包" />
            </CardContent>
          </Card>

          {/* 订单信息 */}
          {order && (
            <Card className="flex-1 overflow-hidden flex flex-col">
              <CardContent className="p-4 flex-1 overflow-y-auto">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <div className="text-xl font-bold text-slate-800">{order.orderNo}</div>
                    <div className="text-sm text-slate-500">{order.customer}</div>
                  </div>
                  <div className="text-right">
                    <div className="text-sm text-slate-500">{order.carrier}</div>
                    <div className="text-sm font-mono text-blue-600">{order.expressNo}</div>
                  </div>
                </div>

                <div className="text-sm font-medium text-slate-600 mb-2">商品明细</div>
                <div className="space-y-2">
                  {order.items.map((item: any, idx: number) => (
                    <div key={idx} className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
                      <div className="flex items-center gap-3">
                        {item.scanned === item.qty ? (
                          <CheckCircle className="w-5 h-5 text-green-500" />
                        ) : (
                          <XCircle className="w-5 h-5 text-slate-300" />
                        )}
                        <div>
                          <div className="text-sm font-medium">{item.sku}</div>
                          <div className="text-xs text-slate-500">{item.name}</div>
                        </div>
                      </div>
                      <div className={`text-lg font-bold ${item.scanned === item.qty ? "text-green-600" : "text-orange-600"}`}>
                        {item.scanned}/{item.qty}
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* 商品扫码 */}
          {order && (
            <Card className="shrink-0">
              <CardContent className="p-4">
                <label className="text-sm font-medium text-slate-600 mb-2 block">扫描商品条码</label>
                <ScanInput value={scanSku} onChange={setScanSku} onScan={handleSkuScan} placeholder="扫描SKU条码" autoFocus={false} />
              </CardContent>
            </Card>
          )}
        </div>

        {/* 右侧：称重 + 操作 */}
        <div className="flex flex-col gap-4 overflow-hidden">
          {/* 称重显示 */}
          <Card className="shrink-0">
            <CardContent className="p-4 text-center">
              <div className="text-sm text-slate-500 mb-2 flex items-center justify-center gap-2">
                <Scale className="w-4 h-4" /> 实时重量
              </div>
              <div className={`weight-display text-6xl font-bold ${weightOk ? "text-green-600" : "text-slate-800"}`}>
                {weight.toFixed(3)}
              </div>
              <div className="text-sm text-slate-500 mt-1">kg</div>
              {order && (
                <div className="mt-3 pt-3 border-t border-slate-100">
                  <div className="flex justify-between text-sm">
                    <span className="text-slate-500">理论重量</span>
                    <span className="font-medium">{order.totalWeight.toFixed(3)} kg</span>
                  </div>
                  <div className="flex justify-between text-sm mt-1">
                    <span className="text-slate-500">差异</span>
                    <span className={`font-medium ${weightOk ? "text-green-600" : "text-red-600"}`}>
                      {weightDiff > 0 ? "+" : ""}{weightDiff.toFixed(3)} kg {weightOk ? "✓" : "✗"}
                    </span>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* 包装材料选择 */}
          {order && (
            <Card className="shrink-0">
              <CardContent className="p-4">
                <label className="text-sm font-medium text-slate-600 mb-2 block">包装材料</label>
                <select className="w-full h-10 px-3 border border-slate-200 rounded-md text-sm">
                  <option>标准纸箱 (S)</option>
                  <option>标准纸箱 (M)</option>
                  <option>标准纸箱 (L)</option>
                  <option>防水袋</option>
                  <option>气泡信封</option>
                </select>
              </CardContent>
            </Card>
          )}

          {/* 操作按钮 */}
          {order && (
            <div className="flex-1 flex flex-col gap-3 justify-end">
              <Button
                onClick={handlePrint}
                variant="outline"
                className="w-full h-12 text-base"
                disabled={!allScanned}
              >
                <Printer className="w-5 h-5 mr-2" /> 打印面单
              </Button>
              <Button
                onClick={handleComplete}
                className="w-full h-14 text-lg"
                disabled={!allScanned || !weightOk || packing}
              >
                {packing ? (
                  <span className="flex items-center gap-2"><RefreshCw className="w-5 h-5 animate-spin" /> 打包完成</span>
                ) : (
                  <span className="flex items-center gap-2"><CheckCircle className="w-5 h-5" /> 确认打包完成</span>
                )}
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
