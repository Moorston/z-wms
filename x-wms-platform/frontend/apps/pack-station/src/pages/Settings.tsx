import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Card, CardContent, Input } from "@xwms/ui";
import { toast } from "sonner";
import { ArrowLeft, Scale, Printer, Save, TestTube } from "lucide-react";

export default function Settings() {
  const navigate = useNavigate();
  const [scalePort, setScalePort] = useState("COM3");
  const [scaleBaud, setScaleBaud] = useState("9600");
  const [printerName, setPrinterName] = useState("热敏打印机-前台");
  const [stationNo, setStationNo] = useState("01");

  const handleSave = () => {
    toast.success("设置已保存");
  };

  const handleTestScale = () => {
    toast.success("电子秤测试成功，读数: 2.350 kg");
  };

  const handleTestPrinter = () => {
    toast.success("测试页已发送到打印机");
  };

  return (
    <div className="h-screen flex flex-col bg-slate-100">
      <header className="h-14 bg-white border-b border-slate-200 flex items-center gap-3 px-6">
        <button onClick={() => navigate("/pack")} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <span className="text-lg font-bold text-slate-800">系统设置</span>
      </header>

      <div className="flex-1 p-6 overflow-y-auto">
        <div className="max-w-2xl mx-auto space-y-6">
          {/* 打包台设置 */}
          <Card>
            <CardContent className="p-6 space-y-4">
              <h3 className="text-base font-semibold text-slate-800">打包台设置</h3>
              <div>
                <label className="text-sm text-slate-600 mb-1 block">打包台编号</label>
                <Input value={stationNo} onChange={(e) => setStationNo(e.target.value)} className="max-w-xs" />
              </div>
            </CardContent>
          </Card>

          {/* 电子秤设置 */}
          <Card>
            <CardContent className="p-6 space-y-4">
              <h3 className="text-base font-semibold text-slate-800 flex items-center gap-2">
                <Scale className="w-5 h-5 text-blue-600" /> 电子秤设置
              </h3>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm text-slate-600 mb-1 block">串口号</label>
                  <select value={scalePort} onChange={(e) => setScalePort(e.target.value)} className="w-full h-10 px-3 border border-slate-200 rounded-md text-sm">
                    <option>COM1</option>
                    <option>COM2</option>
                    <option>COM3</option>
                    <option>COM4</option>
                    <option>COM5</option>
                  </select>
                </div>
                <div>
                  <label className="text-sm text-slate-600 mb-1 block">波特率</label>
                  <select value={scaleBaud} onChange={(e) => setScaleBaud(e.target.value)} className="w-full h-10 px-3 border border-slate-200 rounded-md text-sm">
                    <option>2400</option>
                    <option>4800</option>
                    <option>9600</option>
                    <option>19200</option>
                    <option>38400</option>
                    <option>115200</option>
                  </select>
                </div>
              </div>
              <Button variant="outline" size="sm" onClick={handleTestScale}>
                <TestTube className="w-4 h-4 mr-1" /> 测试连接
              </Button>
            </CardContent>
          </Card>

          {/* 打印机设置 */}
          <Card>
            <CardContent className="p-6 space-y-4">
              <h3 className="text-base font-semibold text-slate-800 flex items-center gap-2">
                <Printer className="w-5 h-5 text-green-600" /> 打印机设置
              </h3>
              <div>
                <label className="text-sm text-slate-600 mb-1 block">默认打印机</label>
                <select value={printerName} onChange={(e) => setPrinterName(e.target.value)} className="w-full h-10 px-3 border border-slate-200 rounded-md text-sm max-w-md">
                  <option>热敏打印机-前台</option>
                  <option>热敏打印机-后台</option>
                  <option>激光打印机-办公</option>
                </select>
              </div>
              <Button variant="outline" size="sm" onClick={handleTestPrinter}>
                <TestTube className="w-4 h-4 mr-1" /> 打印测试页
              </Button>
            </CardContent>
          </Card>

          {/* 保存按钮 */}
          <div className="flex justify-end">
            <Button onClick={handleSave} className="px-8">
              <Save className="w-4 h-4 mr-2" /> 保存设置
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
