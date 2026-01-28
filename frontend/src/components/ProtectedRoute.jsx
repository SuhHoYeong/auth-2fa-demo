import { Navigate } from "react-router-dom";
import { AuthStage, getStage } from "../lib/auth";

export default function ProtectedRoute({ children }) {
  const stage = getStage();

  // 로그인도 안 함
  if (stage === AuthStage.NONE) return <Navigate to="/login" replace />;

  // 로그인은 했는데 2FA가 남음
  if (stage === AuthStage.PENDING_2FA) return <Navigate to="/2fa" replace />;

  // AUTHENTICATED면 통과
  return children;
}
