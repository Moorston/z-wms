import { Routes, Route, Navigate } from "react-router-dom";
import { Toaster } from "@xwms/ui";
import MainLayout from "./layouts/MainLayout";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import InboundList from "./pages/inbound/InboundList";
import OutboundList from "./pages/outbound/OutboundList";
import InventoryList from "./pages/inventory/InventoryList";
import ProductList from "./pages/master/ProductList";
import CustomerList from "./pages/master/CustomerList";
import LocationList from "./pages/master/LocationList";

function App() {
  return (
    <>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<MainLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="inbound" element={<InboundList />} />
          <Route path="outbound" element={<OutboundList />} />
          <Route path="inventory" element={<InventoryList />} />
          <Route path="master/product" element={<ProductList />} />
          <Route path="master/customer" element={<CustomerList />} />
          <Route path="master/location" element={<LocationList />} />
        </Route>
      </Routes>
      <Toaster position="top-right" />
    </>
  );
}

export default App;
