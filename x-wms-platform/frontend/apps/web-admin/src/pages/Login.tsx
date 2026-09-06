import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Warehouse, Eye, EyeOff } from "lucide-react";
import { Button, Input, Form, FormField, FormItem, FormLabel, FormControl, FormMessage } from "@xwms/ui";
import { authApi } from "@xwms/api";
import { storage } from "@xwms/shared";
import { toast } from "sonner";

const loginSchema = z.object({
  username: z.string().min(1, "请输入用户名"),
  password: z.string().min(1, "请输入密码"),
});

type LoginForm = z.infer<typeof loginSchema>;

export default function Login() {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const form = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: "admin", password: "admin123" },
  });

  const onSubmit = async (data: LoginForm) => {
    setLoading(true);
    try {
      const result = await authApi.login(data);
      storage.set("access_token", result.accessToken);
      storage.set("refresh_token", result.refreshToken);
      storage.set("user", result.user);
      toast.success("登录成功");
      navigate("/dashboard");
    } catch (error: any) {
      toast.error(error.message || "登录失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-600 via-blue-700 to-indigo-800">
      <div className="w-full max-w-md mx-4">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-white/10 backdrop-blur rounded-2xl mb-4">
            <Warehouse className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white">X WMS</h1>
          <p className="text-blue-200 mt-2">企业级仓储管理系统</p>
        </div>

        {/* 登录表单 */}
        <div className="bg-white rounded-2xl shadow-2xl p-8">
          <h2 className="text-xl font-semibold text-slate-800 mb-6">账号登录</h2>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
              <FormField
                control={form.control}
                name="username"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>用户名</FormLabel>
                    <FormControl>
                      <Input placeholder="请输入用户名" {...field} className="h-11" />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>密码</FormLabel>
                    <FormControl>
                      <div className="relative">
                        <Input
                          type={showPassword ? "text" : "password"}
                          placeholder="请输入密码"
                          {...field}
                          className="h-11 pr-10"
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                        >
                          {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                        </button>
                      </div>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <Button type="submit" className="w-full h-11 text-base" disabled={loading}>
                {loading ? "登录中..." : "登 录"}
              </Button>
            </form>
          </Form>
          <div className="mt-6 text-center text-sm text-slate-500">
            默认账号：admin / admin123
          </div>
        </div>
      </div>
    </div>
  );
}
