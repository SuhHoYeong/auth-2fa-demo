-- Seed or update test user for login + SMS 2FA
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT,
  provider TEXT NOT NULL DEFAULT 'LOCAL',
  provider_sub TEXT,
  phone TEXT,
  twofa_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  twofa_secret TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Upsert test user
INSERT INTO users (email, password_hash, provider, provider_sub, phone, twofa_enabled, twofa_secret)
VALUES (
  'test@example.com',
  crypt('appuser1234', gen_salt('bf')),
  'LOCAL',
  NULL,
  '+818049260976',
  TRUE,
  'JBSWY3DPEHPK3PXP' -- sample base32 TOTP secret (can be changed)
)
ON CONFLICT (email) DO UPDATE
  SET password_hash = EXCLUDED.password_hash,
      provider = EXCLUDED.provider,
      provider_sub = EXCLUDED.provider_sub,
      phone = EXCLUDED.phone,
      twofa_enabled = EXCLUDED.twofa_enabled,
      twofa_secret = EXCLUDED.twofa_secret,
      updated_at = now();

-- Verify
SELECT id, email, provider, phone, twofa_enabled FROM users WHERE email = 'test@example.com';
