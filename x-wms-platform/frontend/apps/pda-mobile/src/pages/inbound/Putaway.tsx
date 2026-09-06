import { useState } from "react";
import { ScanInput, Button, Card, CardContent, StatusBadge } from "@xwms/ui";
import { toast } from "sonner";
import { Package, MapPin, CheckCircle } from "lucide-react";

export default function Putaway() {
  const [lpn, setLpn] = useState("");
  const [location, setLocation] = useState("");
  const [done, setDone] = useState(false);

  const handleLpnScan = (value: string) => {
    setLpn(value);
    toast.success(`托盘 ${value} 已加载，推荐库位: A-01-03`);
    setLocation("A-01-03");
  };

  const handleLocationScan = (value: string) => {
    setLocation(value);
  };

  const handleConfirm = () => {
    if (!lpn || !location) {
      toast.error("请扫描托盘和库位");
      return;
    }
    setDone(true);
    toast.success(`上架成功：${lpn} → ${location}`);
    setTimeout(() => {
      setLpn("");
      setLocation("");
      setDone(false);
    }, 1500);
  };

  return (
    <div className="p-4 space-y-4">
      <h2 className="text-lg font-bold text-slate-800">标准上架</h2>

      <Card>
        <CardContent className="p-4 space-y-3">
          <label className="text-sm font-medium text-slate-600 flex items-center gap-2">
            <Package className="w-4 h-4" /> 扫描托盘/LPN
          </label>
          <ScanInput value={lpn} onChange={setLpn} onScan={handleLpnScan} placeholder="扫描托盘条码" />
        </CardContent>
      </Card>

      {lpn && (
        <>
          <Card>
            <CardContent className="p-4 space-y-3">
              <label className="text-sm font-medium text-slate-600 flex items-center gap-2">
                <MapPin className="w-4 h-4" /> 扫描目标库位
              </label>
              <ScanInput value={location} onChange={setLocation} onScan={handleLocationScan} placeholder="扫描库位条码" autoFocus={false} />
              <div className="text-xs text-slate-500 bg-blue-50 p-2 rounded">
                系统推荐库位：<span className="font-medium text-blue-600">A-01-03</span>（按上架规则自动分配）
              </div>
            </CardContent>
          </Card>

          <Button onClick={handleConfirm} className="w-full h-14 text-lg" disabled={done}>
            {done ? (
              <span className="flex items-center gap-2"><CheckCircle className="w-5 h-5" /> 上架成功</span>
            ) : (
              "确认上架"
            )}
          </Button>
        </>
      )}
    </div>
  );
}
