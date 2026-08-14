package org.zstack.crypto.envelope;

import org.zstack.kvm.HostSecretEnvelopeCryptoExtensionPoint;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.X25519PublicKeyParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * HPKE seal (RFC 9180) compatible with Go key-agent: KEM_X25519_HKDF_SHA256, KDF_HKDF_SHA256, AEAD_AES256GCM.
 * Seal: encrypt wrapper DEK with host public key; output = enc (32) || ciphertext (for agent to open with private key).
 * Must use the same HPKE "info" as key-agent (cmd/key-agent: info := []byte("key-agent hpke info")) for key schedule.
 */
public final class HostSecretEnvelopeCryptoExtension implements HostSecretEnvelopeCryptoExtensionPoint {
    private static final String HPKE_V1 = "HPKE-v1";
    /** HPKE application info; must match key-agent main.go: info := []byte("key-agent hpke info") */
    private static final byte[] HPKE_INFO = "key-agent hpke info".getBytes(StandardCharsets.UTF_8);
    /** RFC 9180 / IANA HPKE: KEM_ID 0x0020 = DHKEM(X25519, HKDF-SHA256); Nenc = Npk = 32. */
    private static final byte[] KEM_ID = new byte[]{0x00, 0x20};
    private static final byte[] KDF_ID = new byte[]{0x00, 0x01};   // HKDF-SHA256
    private static final byte[] AEAD_ID = new byte[]{0x00, 0x02};   // AES-256-GCM
    private static final byte[] KEM_SUITE_ID = concat("KEM".getBytes(StandardCharsets.UTF_8), KEM_ID);
    private static final byte[] SUITE_ID = concat(concat("HPKE".getBytes(StandardCharsets.UTF_8), KEM_ID), concat(KDF_ID, AEAD_ID));
    /** Nh: KDF output size (SHA-256 = 32). RFC 9180 Section 4. */
    private static final int NH = 32;
    /** Nk: AEAD key size (AES-256 = 32). RFC 9180 Section 7.3. */
    private static final int NK = 32;
    /** Nn: AEAD nonce size (AES-GCM = 12). RFC 9180 Section 7.3. */
    private static final int NN = 12;

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static byte[] i2osp(int n, int w) {
        byte[] r = new byte[w];
        for (int i = w - 1; i >= 0; i--) {
            r[i] = (byte) (n & 0xff);
            n >>= 8;
        }
        return r;
    }

    /** RFC 9180 LabeledExtract(salt, label, ikm); use kemSuiteId for KEM layer, null for HPKE layer (uses SUITE_ID). */
    private static byte[] labeledExtract(byte[] salt, String label, byte[] ikm, Digest digest, byte[] suiteId) {
        byte[] sid = suiteId != null ? suiteId : SUITE_ID;
        byte[] labeledIkm = concat(concat(concat(HPKE_V1.getBytes(StandardCharsets.UTF_8), sid), label.getBytes(StandardCharsets.UTF_8)), ikm != null ? ikm : new byte[0]);
        return hkdfExtract(salt, labeledIkm, digest);
    }

    /** RFC 9180 LabeledExpand(prk, label, info, L); use kemSuiteId for KEM layer, null for HPKE layer. */
    private static byte[] labeledExpand(byte[] prk, String label, byte[] info, int L, Digest digest, byte[] suiteId) {
        byte[] sid = suiteId != null ? suiteId : SUITE_ID;
        byte[] labeledInfo = concat(concat(concat(concat(i2osp(L, 2), HPKE_V1.getBytes(StandardCharsets.UTF_8)), sid), label.getBytes(StandardCharsets.UTF_8)), info != null ? info : new byte[0]);
        return hkdfExpand(prk, labeledInfo, L, digest);
    }

    /**
     * RFC 5869 / RFC 9180 HKDF-Extract: returns PRK = HMAC-Hash(salt, IKM).
     */
    private static byte[] hkdfExtract(byte[] salt, byte[] ikm, Digest digest) {
        byte[] saltBytes = (salt != null && salt.length > 0) ? salt : new byte[NH];
        HMac hmac = new HMac(digest);
        hmac.init(new KeyParameter(saltBytes));
        hmac.update(ikm, 0, ikm.length);
        byte[] prk = new byte[NH];
        hmac.doFinal(prk, 0);
        return prk;
    }

    /**
     * RFC 5869 / RFC 9180 HKDF-Expand: OKM = HKDF-Expand(PRK, info, L).
     */
    private static byte[] hkdfExpand(byte[] prk, byte[] info, int L, Digest digest) {
        HKDFBytesGenerator gen = new HKDFBytesGenerator(digest);
        gen.init(HKDFParameters.skipExtractParameters(prk, info != null ? info : new byte[0]));
        byte[] out = new byte[L];
        gen.generateBytes(out, 0, out.length);
        return out;
    }

    /**
     * Seal plaintext with recipient's X25519 public key (raw 32 bytes).
     * Returns envelope = enc (32 bytes) || ciphertext (AEAD output).
     */
    @Override
    public byte[] seal(byte[] recipientPublicKey, byte[] plaintext) throws Exception {
        if (recipientPublicKey == null || recipientPublicKey.length != 32 || plaintext == null) {
            throw new IllegalArgumentException("recipientPublicKey must be 32 bytes, plaintext non-null");
        }
        SecureRandom random = new SecureRandom();
        Digest digest = new SHA256Digest();

        // 1. Generate ephemeral X25519 key pair (BC crypto)
        X25519KeyPairGenerator kpg = new X25519KeyPairGenerator();
        kpg.init(new X25519KeyGenerationParameters(random));
        AsymmetricCipherKeyPair ephemeralKp = kpg.generateKeyPair();
        X25519PublicKeyParameters ephemeralPub = (X25519PublicKeyParameters) ephemeralKp.getPublic();
        byte[] enc = ephemeralPub.getEncoded();

        // 2. DH shared secret (ephemeral priv, recipient pub)
        X25519PublicKeyParameters recipientPub = new X25519PublicKeyParameters(recipientPublicKey, 0);
        X25519Agreement agreement = new X25519Agreement();
        agreement.init(ephemeralKp.getPrivate());
        byte[] sharedSecret = new byte[32];
        agreement.calculateAgreement(recipientPub, sharedSecret, 0);

        // 3. KEM shared_secret (DHKEM ExtractAndExpand) with KEM suite_id
        byte[] kemContext = concat(enc, recipientPublicKey);
        byte[] eaePrk = labeledExtract(new byte[0], "eae_prk", sharedSecret, digest, KEM_SUITE_ID);
        byte[] kemSharedSecret = labeledExpand(eaePrk, "shared_secret", kemContext, NH, digest, KEM_SUITE_ID);

        // 4. Key schedule (base mode, empty psk; info must match key-agent NewReceiver(sk, info))
        byte[] pskIdHash = labeledExtract(new byte[0], "psk_id_hash", new byte[0], digest, null);
        byte[] infoHash = labeledExtract(new byte[0], "info_hash", HPKE_INFO, digest, null);
        byte[] keyScheduleContext = concat(new byte[]{0x00}, concat(pskIdHash, infoHash));
        byte[] secret = labeledExtract(kemSharedSecret, "secret", new byte[0], digest, null);
        byte[] key = labeledExpand(secret, "key", keyScheduleContext, NK, digest, null);
        byte[] baseNonce = labeledExpand(secret, "base_nonce", keyScheduleContext, NN, digest, null);

        // 5. AEAD Seal (AES-256-GCM, empty aad)
        GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
        cipher.init(true, new AEADParameters(new KeyParameter(key), 128, baseNonce));
        byte[] out = new byte[cipher.getOutputSize(plaintext.length)];
        int len = cipher.processBytes(plaintext, 0, plaintext.length, out, 0);
        len += cipher.doFinal(out, len);
        byte[] ct = Arrays.copyOf(out, len);

        return concat(enc, ct);
    }

    /** Public no-arg constructor for Spring bean (implements HostSecretEnvelopeCryptoExtensionPoint). */
    public HostSecretEnvelopeCryptoExtension() {
    }
}
