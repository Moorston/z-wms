import { useState } from "react";
import { Outlet, useNavigate, useLocation } from "react-router-dom";
import {
  LayoutDashboard,
  Package,
  Truck,
  Warehouse,
  Boxes,
  Users,
  MapPin,
  Settings,
  ChevronLeft,
  ChevronRight,
  Bell,
  Search,
  User,
  LogOut,
} from "lucide-react";
import { cn } from "@xwms/ui";
import { storage } from "@xwms/shared";

const menuItems = [
  { path: "/dashboard", label: "工作台", icon: LayoutDashboard },
  {
    label: "入库管理",
    icon: Package,
    children: [{ path: "/inbound", label: "入库单" }],
  },
  {
    label: "出库管理",
    icon: Truck,
    children: [{ path: "/outbound", label: "出库单" }],
  },
  {
    label: "库存管理",
    icon: Boxes,
    children: [{ path: "/inventory", label: "库存查询" }],
  },
  {
    label: "基础数据",
    icon: Warehouse,
    children: [
      { path: "/master/product", label: "产品档案" },
      { path: "/master/customer", label: "客户档案" },
      { path: "/master/location", label: "库位管理" },
    ],
  },
];

export default function MainLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [openMenus, setOpenMenus] = useState<string[]>(["入库管理"]);
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    storage.clear();
    navigate("/login");
  };

  const toggleMenu = (label: string) => {
    setOpenMenus((prev) =>
      prev.includes(label) ? prev.filter((m) => m !== label) : [...prev, label]
    );
  };

  return (
    <div className="flex h-screen bg-slate-50">
      {/* 侧边栏 */}
      <aside
        className={cn(
          "bg-white border-r border-slate-200 flex flex-col transition-all duration-300",
          collapsed ? "w-16" : "w-60"
        )}
      >
        {/* Logo */}
        <div className="h-14 flex items-center justify-center border-b border-slate-200">
          {!collapsed && (
            <span className="text-lg font-bold text-blue-600">X WMS</span>
          )}
          {collapsed && <span className="text-lg font-bold text-blue-600">X</span>}
        </div>

        {/* 菜单 */}
        <nav className="flex-1 overflow-y-auto py-2">
          {menuItems.map((item) => (
            <div key={item.label}>
              {item.children ? (
                <>
                  <button
                    onClick={() => toggleMenu(item.label)}
                    className="w-full flex items-center px-4 py-2.5 text-sm text-slate-600 hover:bg-slate-100 transition-colors"
                  >
                    <item.icon className="w-5 h-5 shrink-0" />
                    {!collapsed && (
                      <>
                        <span className="ml-3 flex-1 text-left">{item.label}</span>
                        <ChevronRight
                          className={cn(
                            "w-4 h-4 transition-transform",
                            openMenus.includes(item.label) && "rotate-90"
                          )}
                        />
                      </>
                    )}
                  </button>
                  {!collapsed && openMenus.includes(item.label) && (
                    <div className="bg-slate-50">
                      {item.children.map((child) => (
                        <button
                          key={child.path}
                          onClick={() => navigate(child.path)}
                          className={cn(
                            "w-full flex items-center pl-12 pr-4 py-2 text-sm transition-colors",
                            location.pathname === child.path
                              ? "text-blue-600 bg-blue-50 border-r-2 border-blue-600"
                              : "text-slate-500 hover:bg-slate-100"
                          )}
                        >
                          {child.label}
                        </button>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <button
                  onClick={() => navigate(item.path!)}
                  className={cn(
                    "w-full flex items-center px-4 py-2.5 text-sm transition-colors",
                    location.pathname === item.path
                      ? "text-blue-600 bg-blue-50 border-r-2 border-blue-600"
                      : "text-slate-600 hover:bg-slate-100"
                  )}
                >
                  <item.icon className="w-5 h-5 shrink-0" />
                  {!collapsed && <span className="ml-3">{item.label}</span>}
                </button>
              )}
            </div>
          ))}
        </nav>

        {/* 折叠按钮 */}
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="h-10 border-t border-slate-200 flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-50"
        >
          {collapsed ? <ChevronRight className="w-5 h-5" /> : <ChevronLeft className="w-5 h-5" />}
        </button>
      </aside>

      {/* 主内容区 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* 顶部导航 */}
        <header className="h-14 bg-white border-b border-slate-200 flex items-center justify-between px-6">
          <div className="flex items-center gap-4">
            <div className="relative">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                type="text"
                placeholder="搜索订单/商品/库位..."
                className="w-64 h-9 pl-9 pr-4 rounded-md border border-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>
          <div className="flex items-center gap-4">
            <button className="relative p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-md">
              <Bell className="w-5 h-5" />
              <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full" />
            </button>
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center">
                <User className="w-4 h-4 text-blue-600" />
              </div>
              <span className="text-sm text-slate-700">管理员</span>
              <button
                onClick={handleLogout}
                className="p-1.5 text-slate-400 hover:text-red-500 hover:bg-red-50 rounded"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </div>
        </header>

        {/* 内容区 */}
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
