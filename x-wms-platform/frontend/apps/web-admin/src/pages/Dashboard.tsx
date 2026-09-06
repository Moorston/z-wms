import { useEffect, useRef } from "react";
import * as echarts from "echarts";
import { KpiCard } from "@xwms/ui";
import { Package, Truck, Boxes, AlertTriangle } from "lucide-react";

export default function Dashboard() {
  const chartRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!chartRef.current) return;
    const chart = echarts.init(chartRef.current);
    chart.setOption({
      title: { text: "近7天出入库趋势", left: "center", textStyle: { fontSize: 14 } },
      tooltip: { trigger: "axis" },
      legend: { data: ["入库", "出库"], bottom: 0 },
      grid: { left: "3%", right: "4%", bottom: "12%", top: "15%", containLabel: true },
      xAxis: { type: "category", data: ["周一", "周二", "周三", "周四", "周五", "周六", "周日"] },
      yAxis: { type: "value" },
      series: [
        { name: "入库", type: "line", smooth: true, data: [120, 132, 101, 134, 90, 230, 210], itemStyle: { color: "#2563eb" }, areaStyle: { opacity: 0.1 } },
        { name: "出库", type: "line", smooth: true, data: [220, 182, 191, 234, 290, 330, 310], itemStyle: { color: "#10b981" }, areaStyle: { opacity: 0.1 } },
      ],
    });
    const handleResize = () => chart.resize();
    window.addEventListener("resize", handleResize);
    return () => {
      window.removeEventListener("resize", handleResize);
      chart.dispose();
    };
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-slate-800">工作台</h1>

      {/* KPI卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard title="今日入库" value={128} unit="单" trend={12.5} icon={<Package className="w-5 h-5" />} color="blue" />
        <KpiCard title="今日出库" value={256} unit="单" trend={8.3} icon={<Truck className="w-5 h-5" />} color="green" />
        <KpiCard title="库存总量" value={15842} unit="件" icon={<Boxes className="w-5 h-5" />} color="purple" />
        <KpiCard title="库存预警" value={23} unit="项" trend={-5.2} icon={<AlertTriangle className="w-5 h-5" />} color="orange" />
      </div>

      {/* 图表 */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2 bg-white rounded-lg border border-slate-200 p-4">
          <div ref={chartRef} style={{ height: 350 }} />
        </div>
        <div className="bg-white rounded-lg border border-slate-200 p-4">
          <h3 className="text-base font-semibold text-slate-700 mb-4">待处理任务</h3>
          <div className="space-y-3">
            {[
              { label: "待收货", count: 12, color: "bg-blue-100 text-blue-700" },
              { label: "待上架", count: 8, color: "bg-orange-100 text-orange-700" },
              { label: "待拣货", count: 25, color: "bg-green-100 text-green-700" },
              { label: "待复核", count: 15, color: "bg-purple-100 text-purple-700" },
              { label: "待发货", count: 10, color: "bg-pink-100 text-pink-700" },
            ].map((item) => (
              <div key={item.label} className="flex items-center justify-between py-2 border-b border-slate-100 last:border-0">
                <span className="text-sm text-slate-600">{item.label}</span>
                <span className={`px-2 py-0.5 rounded text-sm font-medium ${item.color}`}>{item.count}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
