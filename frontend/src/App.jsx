import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import TwoFactorPage from "./pages/TwoFactorPage";
import MainPage from "./pages/MainPage";
import ProtectedRoute from "./components/ProtectedRoute";
import { useEffect } from "react";
import { apiFetch } from "./lib/apiFetch";
import { setStage, AuthStage } from "./lib/auth";

export default function App() {
  console.log("API BASE URL =", import.meta.env.VITE_API_BASE_URL);
  useEffect(() => {
    // 앱 시작 시 서버 세션 상태 동기화(선택)
    (async () => {
      try {
        const me = await apiFetch("/api/auth/me");
        const serverStage = me?.stage;
        if (serverStage === AuthStage.AUTHENTICATED || serverStage === AuthStage.PENDING_2FA) {
          setStage(serverStage);
        } else {
          setStage(AuthStage.NONE);
        }
      } catch {
        setStage(AuthStage.NONE);
      }
    })();
  }, []);
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/2fa" element={<TwoFactorPage />} />

      <Route
        path="/"
        element={
          <ProtectedRoute>
            <MainPage />
          </ProtectedRoute>
        }
      />

      {/* 없는 주소 들어오면 메인으로 */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
