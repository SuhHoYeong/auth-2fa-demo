import { useNavigate } from "react-router-dom";
import { logout } from "../lib/auth";

export default function MainPage() {
  const navigate = useNavigate();

  const onLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div style={styles.container}>
      <h2>메인 화면 ✅</h2>
      <p>로그인 + 2FA 통과하면 여기로 온다!</p>

      <button style={styles.button} onClick={onLogout}>
        로그아웃
      </button>
    </div>
  );
}

const styles = {
  container: { maxWidth: 420, margin: "60px auto", padding: 16 },
  button: { padding: 12, borderRadius: 10, border: "none", cursor: "pointer" },
};
