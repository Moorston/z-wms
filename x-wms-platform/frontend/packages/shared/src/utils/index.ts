// 条码校验工具
export function validateBarcode(barcode: string): boolean {
  if (!barcode || barcode.length < 3) return false;
  if (barcode.length === 13) {
    let sum = 0;
    for (let i = 0; i < 12; i++) {
      const digit = parseInt(barcode[i]);
      sum += i % 2 === 0 ? digit : digit * 3;
    }
    const checkDigit = (10 - (sum % 10)) % 10;
    return checkDigit === parseInt(barcode[12]);
  }
  return /^[A-Za-z0-9_-]+$/.test(barcode);
}

// 本地存储封装
export const storage = {
  get: <T>(key: string, defaultValue?: T): T | null => {
    try {
      const value = localStorage.getItem(key);
      return value ? JSON.parse(value) : defaultValue ?? null;
    } catch {
      return defaultValue ?? null;
    }
  },
  set: (key: string, value: any) => {
    localStorage.setItem(key, JSON.stringify(value));
  },
  remove: (key: string) => {
    localStorage.removeItem(key);
  },
  clear: () => {
    localStorage.clear();
  },
};

// 权限判断
export function hasPermission(userPermissions: string[], required: string): boolean {
  if (!required) return true;
  if (userPermissions.includes("*")) return true;
  return userPermissions.includes(required);
}

// 格式化文件大小
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}
