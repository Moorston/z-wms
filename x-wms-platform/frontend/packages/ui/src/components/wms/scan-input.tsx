import { useEffect, useRef, useState } from "react";
import { Input } from "../ui/input";
import { cn } from "../../lib/utils";

interface ScanInputProps {
  value: string;
  onChange: (value: string) => void;
  onScan?: (value: string) => void;
  placeholder?: string;
  autoFocus?: boolean;
  className?: string;
  disabled?: boolean;
}

/**
 * 扫码输入框组件
 * - 自动聚焦
 * - 扫码枪模拟键盘输入，回车自动触发onScan
 * - 支持PDA物理扫码键（F1/F2）
 */
export function ScanInput({
  value,
  onChange,
  onScan,
  placeholder = "请扫描或输入条码",
  autoFocus = true,
  className,
  disabled,
}: ScanInputProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isScanning, setIsScanning] = useState(false);

  useEffect(() => {
    if (autoFocus && !disabled) {
      inputRef.current?.focus();
    }
  }, [autoFocus, disabled]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    // 扫码枪通常以回车结束
    if (e.key === "Enter") {
      e.preventDefault();
      if (value.trim()) {
        setIsScanning(true);
        onScan?.(value.trim());
        setTimeout(() => setIsScanning(false), 300);
      }
    }
    // PDA物理扫码键 F1/F2
    if (e.key === "F1" || e.key === "F2") {
      e.preventDefault();
      inputRef.current?.focus();
    }
  };

  return (
    <div className="relative">
      <Input
        ref={inputRef}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        disabled={disabled}
        className={cn(
          "h-12 text-lg border-2",
          isScanning && "border-green-500 bg-green-50",
          className
        )}
      />
      {isScanning && (
        <div className="absolute right-3 top-1/2 -translate-y-1/2 text-green-600 text-sm font-medium">
          ✓ 已扫描
        </div>
      )}
    </div>
  );
}
