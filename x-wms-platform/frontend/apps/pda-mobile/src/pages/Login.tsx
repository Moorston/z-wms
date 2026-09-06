import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Warehouse, Eye, EyeOff } from "lucide-react";
import { Button, Input } from "@xwms/ui";
import { authApi } from "@xwms/api";
import { storage } from "@xwms/shared";
import { toast } from "sonner";

export default function Login() {
  const navigate = useNavigate();
  const [username, setUsername] = useState("picker");
  const [password, setPassword] = useState("picker123");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    if (!username || !password) {
      toast.error("请输入用户名和密码");
      return;
    }
    setLoading(true);
    try {
      const result = await authApi.login({ username, password });
      storage.set("access_token", result.accessToken);
      storage.set("refresh_token", result.refreshToken);
      storage.set("user", result.user);
      toast.success("登录成功");
      navigate("/home");
    } catch (error: any) {
      toast.error(error.message || "登录失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="h-full flex flex-col items-center justify-center bg-gradient-to-b from-blue-600 to-blue-800 px-6">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-20 h-20 bg-white/10 backdrop-blur rounded-3xl mb-4">
            <Warehouse className="w-10 h-10 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-white">X WMS</h1>
          <p className="text-blue-200 mt-1">仓储作业PDA端</p>
        </div>

        <div className="bg-white rounded-2xl p-6 shadow-xl space-y-4">
          <div>
            <label className="text-sm text-slate-600 mb-1 block">用户名</label>
            <Input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="请输入工号"
              className="h-12 text-lg"
            />
          </div>
          <div>
            <label className="text-sm text-slate-600 mb-1 block">密码</label>
            <div className="relative">
              <Input
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="请输入密码"
                className="h-12 text-lg pr-10"
                onKeyDown={(e) => e.key === "Enter" && handleLogin()}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400"
              >
                {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
              </button>
            </div>
          </div>
          <Button
            onClick={handleLogin}
            disabled={loading}
            className="w-full h-12 text-lg"
          >
            {loading ? "登录中..." : "登 录"}
          </Button>
        </div>
      </div>
    </div>
  );
}
