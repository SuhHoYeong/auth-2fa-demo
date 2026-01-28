import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { AuthStage, setStage } from "../lib/auth";

export default function TwoFactorPage() {
  const navigate = useNavigate();
  const [code, setCode] = useState("");

  const onVerify = (e) => {
    e.preventDefault();

    // TODO: 나중에 백엔드 붙이면 /2fa/verify API 호출
    if (code.trim().length !== 6) {
      alert("인증코드 6자리를 입력해줘!");
      return;
    }

    setStage(AuthStage.AUTHENTICATED);
    navigate("/");
  };

  return (
    <div style={styles.container}>
      <h2>2단계 인증</h2>
      <p>휴대폰으로 전송된 6자리 인증코드를 입력하세요.</p>

      <form onSubmit={onVerify} style={styles.card}>
        <input
          style={styles.input}
          placeholder="예: 123456"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          maxLength={6}
        />
        <button style={styles.button} type="submit">
          인증 확인
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
};
