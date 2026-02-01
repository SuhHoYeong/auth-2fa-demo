// Simple TOTP generator for testing with Base32 secret
// Usage: node scripts/gen_totp.js JBSWY3DPEHPK3PXP
const crypto = require('crypto');

function base32Decode(str) {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
  const s = str.replace(/=+$/, '').toUpperCase();
  let bits = 0, value = 0; const out = [];
  for (const c of s) {
    const idx = alphabet.indexOf(c);
    if (idx === -1) continue;
    value = (value << 5) | idx;
    bits += 5;
    if (bits >= 8) {
      out.push((value >>> (bits - 8)) & 0xff);
      bits -= 8;
    }
  }
  return Buffer.from(out);
}

function hotp(key, counter, digits = 6) {
  const buf = Buffer.alloc(8);
  for (let i = 7; i >= 0; i--) { buf[i] = counter & 0xff; counter = Math.floor(counter / 256); }
  const hmac = crypto.createHmac('sha1', key).update(buf).digest();
  const offset = hmac[hmac.length - 1] & 0x0f;
  const binary = ((hmac[offset] & 0x7f) << 24) | ((hmac[offset + 1] & 0xff) << 16) | ((hmac[offset + 2] & 0xff) << 8) | (hmac[offset + 3] & 0xff);
  const otp = binary % (10 ** digits);
  return otp.toString().padStart(digits, '0');
}

function totp(base32Secret, period = 30) {
  const key = base32Decode(base32Secret);
  const counter = Math.floor(Date.now() / 1000 / period);
  return hotp(key, counter);
}

const secret = process.argv[2] || 'JBSWY3DPEHPK3PXP';
console.log(totp(secret));
