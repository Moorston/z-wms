import { Outlet, useNavigate, useLocation } from "react-router-dom";
import { ArrowLeft, Home, User, Wifi, WifiOff } from "lucide-react";
import { storage } from "@xwms/shared";
import { useState, useEffect } from "react";

export default function PdaLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [online, setOnline] = useState(navigator.onLine);

  useEffect(() => {
    const handleOnline = () => setOnline(true);
    const handleOffline = () => setOnline(false);
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
    };
  }, []);

  const user = storage.get<any>("user");
  const isHome = location.pathname === "/home";

  return (
    <div className="h-full flex flex-col bg-slate-50">
      {/* 顶部状态栏 */}
      <header
        className="bg-blue-600 text-white px-4 flex items-center justify-between"
        style={{ paddingTop: "var(--safe-top)", height: "calc(48px + var(--safe-top))" }}
      >
        <div className="flex items-center gap-2">
          {!isHome && (
            <button onClick={() => navigate(-1)} className="p-1 -ml-1">
              <ArrowLeft className="w-5 h-5" />
            </button>
          )}
          <span className="text-base font-medium">X WMS PDA</span>
        </div>
        <div className="flex items-center gap-3">
          {online ? <Wifi className="w-4 h-4" /> : <WifiOff className="w-4 h-4 text-red-300" />}
          <span className="text-sm">{user?.realName || "用户"}</span>
        </div>
      </header>

      {/* 内容区 */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>

      {/* 底部导航 */}
      <nav
        className="bg-white border-t border-slate-200 flex justify-around py-2"
        style={{ paddingBottom: "var(--safe-bottom)" }}
      >
        <button
          onClick={() => navigate("/home")}
          className={`flex flex-col items-center gap-1 px-4 py-1 ${isHome ? "text-blue-600" : "text-slate-400"}`}
        >
          <Home className="w-5 h-5" />
          <span className="text-xs">首页</span>
        </button>
        <button
          onClick={() => navigate("/home")}
          className="flex flex-col items-center gap-1 px-4 py-1 text-slate-400"
        >
          <User className="w-5 h-5" />
          <span className="text-xs">我的</span>
        </button>
      </nav>
    </div>
  );
}
