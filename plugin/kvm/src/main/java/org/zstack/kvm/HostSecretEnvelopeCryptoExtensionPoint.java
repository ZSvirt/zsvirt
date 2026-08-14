package org.zstack.kvm;

/**
 * Extension point for sealing plaintext (e.g. DEK) with recipient's X25519 public key for host secret (e.g. vTPM).
 * Implementation is provided by premium/crypto when available; KVM uses PluginRegistry.getExtensionList() to get it.
 */
public interface HostSecretEnvelopeCryptoExtensionPoint {
    /**
     * Seal plaintext with recipient's X25519 public key (raw 32 bytes).
     * @return envelope = enc (32 bytes) || ciphertext (AEAD output), compatible with key-agent open.
     */
    byte[] seal(byte[] recipientPublicKey, byte[] plaintext) throws Exception;
}
