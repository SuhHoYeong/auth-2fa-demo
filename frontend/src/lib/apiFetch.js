import { API_BASE_URL } from "../config";
import { AuthStage, setStage } from "./auth";

export async function apiFetch(path, options = {}) {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    credentials: "include", // ✅ 세션/쿠키 동반
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
  });

  const text = await res.text();
  const toJson = () => {
    try {
      return text ? JSON.parse(text) : undefined;
    } catch {
      return undefined;
    }
  };

  if (!res.ok) {
    const err = new Error(text || `Request failed: ${res.status}`);
    err.status = res.status;
    err.body = toJson();
    // 중앙 처리: 세션 소멸 또는 인증 필요 시 화면 전환
    if (res.status === 401) {
      // 로그인 필요
      try {
        setStage(AuthStage.NONE);
        window.location.assign('/login');
      } catch (_) { }
    } else if (res.status === 403) {
      // 2FA 필요
      try {
        setStage(AuthStage.PENDING_2FA);
        window.location.assign('/2fa');
      } catch (_) { }
    }
    throw err;
  }

  return toJson();
}
