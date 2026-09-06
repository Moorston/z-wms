import { useNavigate } from "react-router-dom";
import {
  Package,
  ArrowDownToLine,
  ArrowUpFromLine,
  Boxes,
  ClipboardList,
  Search,
  RefreshCw,
  Truck,
} from "lucide-react";

const menuGroups = [
  {
    title: "入库作业",
    items: [
      { label: "收货", icon: ArrowDownToLine, path: "/inbound/receive", color: "bg-blue-500" },
      { label: "上架", icon: Package, path: "/inbound/putaway", color: "bg-cyan-500" },
    ],
  },
  {
    title: "出库作业",
    items: [
      { label: "拣货", icon: ArrowUpFromLine, path: "/outbound/picking", color: "bg-green-500" },
      { label: "复核", icon: ClipboardList, path: "/outbound/review", color: "bg-emerald-500" },
    ],
  },
  {
    title: "库存作业",
    items: [
      { label: "移库", icon: RefreshCw, path: "/inventory/move", color: "bg-orange-500" },
      { label: "盘点", icon: Boxes, path: "/inventory/stocktake", color: "bg-amber-500" },
    ],
  },
  {
    title: "查询",
    items: [
      { label: "库存查询", icon: Search, path: "/inventory/query", color: "bg-purple-500" },
      { label: "任务查询", icon: Truck, path: "/task/query", color: "bg-pink-500" },
    ],
  },
];

export default function Home() {
  const navigate = useNavigate();

  return (
    <div className="p-4 space-y-5">
      {/* 今日统计 */}
      <div className="bg-white rounded-xl p-4 shadow-sm">
        <div className="grid grid-cols-3 gap-4 text-center">
          <div>
            <div className="text-2xl font-bold text-blue-600">12</div>
            <div className="text-xs text-slate-500 mt-1">待收货</div>
          </div>
          <div>
            <div className="text-2xl font-bold text-green-600">25</div>
            <div className="text-xs text-slate-500 mt-1">待拣货</div>
          </div>
          <div>
            <div className="text-2xl font-bold text-orange-600">8</div>
            <div className="text-xs text-slate-500 mt-1">待上架</div>
          </div>
        </div>
      </div>

      {/* 功能菜单 */}
      {menuGroups.map((group) => (
        <div key={group.title}>
          <h3 className="text-sm font-medium text-slate-500 mb-2 px-1">{group.title}</h3>
          <div className="grid grid-cols-4 gap-3">
            {group.items.map((item) => (
              <button
                key={item.label}
                onClick={() => navigate(item.path)}
                className="flex flex-col items-center gap-2 p-3 bg-white rounded-xl shadow-sm active:scale-95 transition-transform"
              >
                <div className={`w-12 h-12 ${item.color} rounded-xl flex items-center justify-center`}>
                  <item.icon className="w-6 h-6 text-white" />
                </div>
                <span className="text-xs text-slate-700">{item.label}</span>
              </button>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
