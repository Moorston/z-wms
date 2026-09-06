import type { CapacitorConfig } from "@capacitor/cli";

const config: CapacitorConfig = {
  appId: "com.xwms.pda",
  appName: "X WMS PDA",
  webDir: "dist",
  server: {
    androidScheme: "https",
  },
  android: {
    allowMixedContent: true,
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      backgroundColor: "#2563eb",
      showSpinner: true,
    },
    StatusBar: {
      style: "light",
      backgroundColor: "#2563eb",
    },
  },
};

export default config;
