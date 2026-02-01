package com.suhhoyeong.backend.auth;

import com.suhhoyeong.backend.auth.dto.LoginRequest;
import com.suhhoyeong.backend.auth.dto.StageResponse;
import com.suhhoyeong.backend.auth.dto.TwoFactorRequest;
import com.suhhoyeong.backend.auth.service.AuthService;
import com.suhhoyeong.backend.auth.model.AuthStage;
import com.suhhoyeong.backend.auth.model.User;
import com.suhhoyeong.backend.auth.exception.InvalidCredentialsException;
import com.suhhoyeong.backend.auth.exception.WrongProviderException;
import com.suhhoyeong.backend.auth.service.TotpService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final TotpService totpService;

    public AuthController(AuthService authService, TotpService totpService) {
        this.authService = authService;
        this.totpService = totpService;
    }

    private static final String KEY = "AUTH_STAGE";
    private static final String TOTP_SECRET = "TOTP_SECRET";
    private static final String USER_EMAIL = "USER_EMAIL";

    private AuthStage getStage(HttpSession session) {
        Object v = session.getAttribute(KEY);
        return (v instanceof AuthStage) ? (AuthStage) v : AuthStage.NONE;
    }

    private void setStage(HttpSession session, AuthStage stage) {
        session.setAttribute(KEY, stage);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req, HttpSession session) {

        if (req.email() == null || req.password() == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "email/password required"));
        }

        if (!req.email().contains("@") || req.password().length() < 4) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "invalid credentials"));
        }
        // 예외는 GlobalExceptionHandler에서 일괄 처리되므로 여기서는 서비스 호출만 수행
        User user = authService.loginLocal(req.email(), req.password());
        // 세션에 사용자 이메일 보관 (등록/확인/비활성화 엔드포인트에서 사용)
        session.setAttribute(USER_EMAIL, user.getEmail());

        setStage(session, AuthStage.PENDING_2FA);
        // 무료버전(TOTP): 세션에 사용자 비밀키를 담아 검증 단계에서 사용
        if (user.getTwofaSecret() != null && !user.getTwofaSecret().isBlank()) {
            session.setAttribute(TOTP_SECRET, user.getTwofaSecret());
        }
        return ResponseEntity.ok(new StageResponse(getStage(session)));
    }

    // 2FA 등록: 시크릿 생성 후 DB 저장 + otpauth URI 반환
    @PostMapping("/2fa/setup")
    public ResponseEntity<?> setup2fa(HttpSession session) {
        Object emailObj = session.getAttribute(USER_EMAIL);
        if (emailObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "not logged in"));
        }
        String email = String.valueOf(emailObj);
        // 이미 구성된 경우 덮어쓰지 않고 구성 상태만 알려줌
        User user = authService.getByEmail(email);
        if (user != null && user.getTwofaSecret() != null && !user.getTwofaSecret().isBlank()) {
            // 검증 단계에서 사용할 수 있도록 세션에도 보관
            session.setAttribute(TOTP_SECRET, user.getTwofaSecret());

            String issuer = "ProjectApp";
            String label = email;
            String otpauthExisting = String.format(
                    "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                    issuer, label, user.getTwofaSecret(), issuer);
            return ResponseEntity.ok(Map.of(
                    "configured", true,
                    "otpauth", otpauthExisting));
        }

        String secret = totpService.generateSecret();
        authService.setTwofaSecret(email, secret);
        session.setAttribute(TOTP_SECRET, secret);

        String issuer = "ProjectApp";
        String label = email;
        String otpauth = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                issuer, label, secret, issuer);

        return ResponseEntity.ok(Map.of(
                "secret", secret,
                "otpauth", otpauth,
                "configured", false));
    }

    // 현재 사용자 시크릿으로 otpauth URI 반환 (이미 구성된 경우 QR만 필요할 때 사용)
    @GetMapping("/2fa/otpauth")
    public ResponseEntity<?> getOtpauth(HttpSession session) {
        Object emailObj = session.getAttribute(USER_EMAIL);
        if (emailObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "not logged in"));
        }
        String email = String.valueOf(emailObj);
        // 세션 우선, 없으면 DB에서
        String secret = null;
        Object sObj = session.getAttribute(TOTP_SECRET);
        if (sObj != null) {
            secret = String.valueOf(sObj);
        } else {
            var user = authService.getByEmail(email);
            if (user != null)
                secret = user.getTwofaSecret();
        }
        if (secret == null || secret.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "no 2FA secret"));
        }
        String issuer = "ProjectApp";
        String label = email;
        String otpauth = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                issuer, label, secret, issuer);
        return ResponseEntity.ok(Map.of("otpauth", otpauth));
    }

    // 2FA 등록 확인: 앱 코드 검증 후 활성화
    @PostMapping("/2fa/confirm")
    public ResponseEntity<?> confirm2fa(@RequestBody TwoFactorRequest req, HttpSession session) {
        Object emailObj = session.getAttribute(USER_EMAIL);
        if (emailObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "not logged in"));
        }
        String email = String.valueOf(emailObj);

        String codeStr = req.code();
        if (codeStr == null || codeStr.trim().length() != 6) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid 2fa code"));
        }
        int code;
        try {
            code = Integer.parseInt(codeStr.trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid 2fa code"));
        }

        // 비밀키는 세션에 있으면 우선 사용, 없으면 DB에서 가져옴
        String secret;
        Object sObj = session.getAttribute(TOTP_SECRET);
        if (sObj != null) {
            secret = String.valueOf(sObj);
        } else {
            var user = authService.getByEmail(email);
            if (user == null || user.getTwofaSecret() == null || user.getTwofaSecret().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "no 2FA secret"));
            }
            secret = user.getTwofaSecret();
        }

        boolean ok = totpService.verify(secret, code);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid 2fa code"));
        }
        authService.setTwofaEnabled(email, true);
        setStage(session, AuthStage.AUTHENTICATED);
        return ResponseEntity.ok(new StageResponse(getStage(session)));
    }

    // 2FA 비활성화
    @DeleteMapping("/2fa")
    public ResponseEntity<?> disable2fa(HttpSession session) {
        Object emailObj = session.getAttribute(USER_EMAIL);
        if (emailObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "not logged in"));
        }
        String email = String.valueOf(emailObj);
        authService.setTwofaEnabled(email, false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa")
    public ResponseEntity<?> verify2fa(@RequestBody TwoFactorRequest req, HttpSession session) {

        AuthStage stage = getStage(session);

        if (stage == AuthStage.NONE) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "not logged in"));
        }

        if (stage != AuthStage.PENDING_2FA) {
            // 이미 AUTHENTICATED면 그대로 리턴
            return ResponseEntity.ok(new StageResponse(stage));
        }

        String codeStr = req.code();
        if (codeStr == null || codeStr.trim().length() != 6) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid 2fa code"));
        }
        Object secretObj = session.getAttribute(TOTP_SECRET);
        if (secretObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "no 2fa in progress"));
        }
        String secret = String.valueOf(secretObj);
        int code;
        try {
            code = Integer.parseInt(codeStr.trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid 2fa code"));
        }
        boolean ok = totpService.verify(secret, code);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "invalid 2fa code"));
        }
        // 성공 시 비밀키를 세션에서 제거할 필요는 없지만, 단계 전환
        setStage(session, AuthStage.AUTHENTICATED);
        return ResponseEntity.ok(new StageResponse(getStage(session)));
    }

    @GetMapping("/me")
    public ResponseEntity<StageResponse> me(HttpSession session) {
        return ResponseEntity.ok(new StageResponse(getStage(session)));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(HttpSession session) {
        AuthStage stage = getStage(session);
        if (stage == AuthStage.NONE) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "not logged in"));
        }
        Object emailObj = session.getAttribute(USER_EMAIL);
        if (emailObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "not logged in"));
        }
        String email = String.valueOf(emailObj);
        User user = authService.getByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "user not found"));
        }
        return ResponseEntity.ok(Map.of("email", user.getEmail()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build(); // 204
    }
}
