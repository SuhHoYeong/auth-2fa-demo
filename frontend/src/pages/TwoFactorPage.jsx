import React from "react";
import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AuthStage, setStage } from "../lib/auth";
import { apiFetch } from "../lib/apiFetch";

export default function TwoFactorPage() {
  const navigate = useNavigate();
  const [code, setCode] = useState("");
  const [secret, setSecret] = useState("");
  const [otpauth, setOtpauth] = useState("");
  const [configured, setConfigured] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const didSetup = useRef(false);

  useEffect(() => {
    // StrictMode에서 useEffect가 2회 실행될 수 있어 중복 호출 방지
    if (didSetup.current) return;
    didSetup.current = true;
    (async () => {
      try {
        // 우선 현재 시크릿으로 QR만 받아보기 (이미 구성된 경우)
        try {
          const got = await apiFetch("/api/auth/2fa/otpauth");
          if (got?.otpauth) {
            setOtpauth(got.otpauth);
            setConfigured(true);
            return; // QR 확보 완료
          }
        } catch (e) {
          // 404(no secret)면 아래에서 setup 진행
        }
        // 미구성인 경우 시크릿 생성 + QR 반환
        const res = await apiFetch("/api/auth/2fa/setup", { method: "POST" });
        if (res?.configured) {
          setConfigured(true);
        }
        setSecret(res?.secret || "");
        setOtpauth(res?.otpauth || "");
      } catch (e) {
        console.error(e);
      }
    })();
  }, []);

  const onVerify = async (e) => {
    e.preventDefault();

    const c = code.trim();
    if (c.length !== 6) {
      alert("인증코드 6자리를 입력해줘!");
      return;
    }

    try {
      const endpoint = secret ? "/api/auth/2fa/confirm" : "/api/auth/2fa";
      const res = await apiFetch(endpoint, {
        method: "POST",
        body: JSON.stringify({ code: c }),
      });

      // 기대 응답: { stage: "AUTHENTICATED" | "PENDING_2FA" | ... }
      const stageFromServer = res?.stage;
      if (stageFromServer === AuthStage.AUTHENTICATED) {
        setStage(AuthStage.AUTHENTICATED);
        navigate("/");
      } else if (stageFromServer === AuthStage.PENDING_2FA) {
        setStage(AuthStage.PENDING_2FA);
        alert("코드가 올바르지 않거나 아직 대기 상태입니다.");
      } else {
        alert("2FA 처리 상태를 확인할 수 없습니다. 다시 시도해 주세요.");
      }
    } catch (err) {
      console.error(err);
      alert("2FA 인증에 실패했습니다. 코드와 유효시간을 확인해 주세요.");
    }
  };

  return (
    <div style={styles.container}>
      <h2>2단계 인증</h2>
      <p>인증 앱(예: Google Authenticator)에서 생성된 6자리 코드를 입력하세요.</p>

      {otpauth && (
        <div style={styles.card}>
          <p>앱에 등록할 QR입니다. 스캔 후 코드를 입력해 주세요.</p>
          <img
            src={`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(otpauth)}`}
            alt="TOTP QR"
            width={220}
            height={220}
          />
          {secret && <p style={styles.secret}>시크릿: {secret}</p>}
        </div>
      )}

      <form onSubmit={onVerify} style={styles.card}>
        <input
          style={styles.input}
          placeholder="예: 123456"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          maxLength={6}
        />
        <button style={styles.button} type="submit">
          {secret ? "설정 완료" : "인증 확인"}
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
  secret: { fontFamily: "monospace" },
};
