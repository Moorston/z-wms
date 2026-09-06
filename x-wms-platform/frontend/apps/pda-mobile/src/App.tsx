import { Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "@xwms/ui";
import PdaLayout from "./layouts/PdaLayout";
import Login from "./pages/Login";
import Home from "./pages/Home";
import Receive from "./pages/inbound/Receive";
import Putaway from "./pages/inbound/Putaway";
import Picking from "./pages/outbound/Picking";
import Review from "./pages/outbound/Review";
import InventoryMove from "./pages/inventory/Move";
import Stocktake from "./pages/inventory/Stocktake";

function App() {
  return (
    <>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<PdaLayout />}>
          <Route index element={<Navigate to="/home" replace />} />
          <Route path="home" element={<Home />} />
          <Route path="inbound/receive" element={<Receive />} />
          <Route path="inbound/putaway" element={<Putaway />} />
          <Route path="outbound/picking" element={<Picking />} />
          <Route path="outbound/review" element={<Review />} />
          <Route path="inventory/move" element={<InventoryMove />} />
          <Route path="inventory/stocktake" element={<Stocktake />} />
        </Route>
      </Routes>
      <Toaster position="top-center" />
    </>
  );
}

export default App;
