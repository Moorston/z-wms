import { Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "@xwms/ui";
import PackStation from "./pages/PackStation";
import ExpressManage from "./pages/ExpressManage";
import Settings from "./pages/Settings";

function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<Navigate to="/pack" replace />} />
        <Route path="/pack" element={<PackStation />} />
        <Route path="/express" element={<ExpressManage />} />
        <Route path="/settings" element={<Settings />} />
      </Routes>
      <Toaster position="top-center" />
    </>
  );
}

export default App;
