package org.zstack.header.vm;

/**
 * VM sensitive-tag encryption operations exposed to other modules.
 *
 * <p>Sensitive tags (consolePassword / sshkey / userdata) are encrypted via the platform-global
 * {@code EncryptFacade} key, not a per-VM derived key. After {@link #mirrorEncryptionState},
 * callers should copy sensitive tags through {@code TagManager.transformTagForCopy} or
 * {@code PatternedSystemTag.getTokenByResourceUuid} + tag create so ciphertext is decrypted
 * from the source and re-encrypted for the destination when needed.
 */
public interface VmEncryptionOperator {
    /**
     * If the source VM has vmEncryption enabled, enable it on the destination VM as well.
     */
    void mirrorEncryptionState(String srcVmUuid, String dstVmUuid);
}
