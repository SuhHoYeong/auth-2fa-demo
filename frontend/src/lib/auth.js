const KEY = "auth_stage";

export const AuthStage = {
  NONE: "NONE",
  PENDING_2FA: "PENDING_2FA",
  AUTHENTICATED: "AUTHENTICATED",
};

export function getStage() {
  return localStorage.getItem(KEY) || AuthStage.NONE;
}

export function setStage(stage) {
  localStorage.setItem(KEY, stage);
}

export function logout() {
  localStorage.removeItem(KEY);
}
