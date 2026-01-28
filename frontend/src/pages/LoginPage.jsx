import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { AuthStage, setStage } from "../lib/auth";
import { apiFetch } from "../lib/apiFetch";

export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [pw, setPw] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");

  const onSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg("");

    if (!email.trim() || !pw.trim()) {
      setErrorMsg("이메일과 비밀번호를 입력해줘!");
      return;
    }

    try {
      setLoading(true);

      // ✅ 백엔드 로그인 API 호출
      // (예: Spring Boot에서 POST /api/auth/login 구현)
      const res = await apiFetch("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({
          email: email.trim(),
          password: pw,
        }),
      });

      /**
       * ✅ 백엔드 응답 형태는 너가 정하면 됨.
       * 아래는 가장 흔한 형태 예시:
       * - { stage: "PENDING_2FA" }  -> 2FA 필요
       * - { stage: "AUTHENTICATED" } -> 바로 로그인 완료
       *
       * 아직 백엔드 응답을 안 정했으면 그냥 성공하면 2FA로 보내도 됨.
       */
      const stageFromServer = res?.stage;

      if (stageFromServer === AuthStage.AUTHENTICATED) {
        setStage(AuthStage.AUTHENTICATED);
        navigate("/");
      } else {
        // 기본은 2FA로 보내기
        setStage(AuthStage.PENDING_2FA);
        navigate("/2fa");
      }
    } catch (err) {
      console.error(err);
      setErrorMsg("로그인 실패! 아이디/비번 확인하거나 서버를 확인해줘.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <h2>로그인</h2>

      <form onSubmit={onSubmit} style={styles.card}>
        <input
          style={styles.input}
          placeholder="이메일"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <input
          style={styles.input}
          placeholder="비밀번호"
          type="password"
          value={pw}
          onChange={(e) => setPw(e.target.value)}
        />

        {errorMsg && <p style={styles.error}>{errorMsg}</p>}

        <button style={styles.button} type="submit" disabled={loading}>
          {loading ? "로그인 중..." : "로그인"}
        </button>

        <button
          style={styles.subButton}
          type="button"
          disabled={loading}
          onClick={() => alert("구글 로그인은 백엔드 붙일 때 연결할게!")}
        >
          Google 로그인(추후)
        </button>
      </form>
    </div>
  );
}

const styles = {
  container: { maxWidth: 420, margin: "60px auto", padding: 16 },
  card: {
    display: "flex",
    flexDirection: "column",
    gap: 12,
    padding: 16,
    border: "1px solid #333",
    borderRadius: 12,
  },
  input: { padding: 12, borderRadius: 10, border: "1px solid #555" },
  button: { padding: 12, borderRadius: 10, border: "none", cursor: "pointer" },
  subButton: {
    padding: 12,
    borderRadius: 10,
    border: "1px solid #555",
    cursor: "pointer",
    background: "transparent",
  },
  error: { color: "tomato", margin: 0 },
};
