import React from "react";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import { logout } from "../lib/auth";
import { apiFetch } from "../lib/apiFetch";

export default function MainPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");

  const onLogout = async () => {
    try {
      await apiFetch("/api/auth/logout", { method: "POST" });
    } catch (e) {
      // 서버 세션이 이미 없을 수도 있으니, 클라이언트 상태만 정리하고 진행
      console.warn("logout API failed or session missing", e);
    } finally {
      logout();
      navigate("/login");
    }
  };

  const onSearchEmail = async () => {
    setErrorMsg("");
    setLoading(true);
    try {
      const res = await apiFetch("/api/auth/profile");
      setEmail(res?.email || "");
    } catch (e) {
      console.error(e);
      if (e?.status === 401) {
        setErrorMsg("세션이 만료되었어요. 다시 로그인해 주세요.");
        navigate("/login");
      } else if (e?.status === 403) {
        setErrorMsg("권한이 부족합니다. 2단계 인증을 완료해 주세요.");
        navigate("/2fa");
      } else if (e?.status === 404) {
        setErrorMsg("프로필 API가 없습니다. 서버를 재시작(배포)해 주세요.");
      } else {
        setErrorMsg("이메일 조회 실패. 잠시 후 다시 시도해 주세요.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <h2>메인 화면 ✅</h2>
      <p>로그인 + 2FA 통과하면 여기로 온다!</p>

      <button style={styles.button} onClick={onLogout}>
        로그아웃
      </button>

      <div style={{ marginTop: 16 }}>
        <button style={styles.button} onClick={onSearchEmail} disabled={loading}>
          {loading ? "조회 중..." : "내 이메일 검색"}
        </button>
        {email && <p>내 이메일: {email}</p>}
        {errorMsg && <p style={{ color: "tomato" }}>{errorMsg}</p>}
      </div>
    </div>
  );
}

const styles = {
  container: { maxWidth: 420, margin: "60px auto", padding: 16 },
  button: { padding: 12, borderRadius: 10, border: "none", cursor: "pointer" },
};
