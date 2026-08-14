package org.zstack.header.keyprovider;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.Completion;

/**
 * Unified resource key management service.
 * Business layers (TPM, volume encryption, etc.) call this to create/retrieve DEKs
 * without knowing gRPC/key-tools protocol details.
 */
public interface EncryptedResourceKeyManager {

    /**
     * Get or create a resource encryption key.
     * <p>
     * Semantically reuses the existing key record for the same {@code (resourceType, resourceUuid)}
     * when one is already available; otherwise creates a new one and returns the plaintext DEK.
     * <p>
     * This contract does not guarantee concurrent linearizability by itself. Callers must not assume
     * the interface alone provides serialization, uniqueness enforcement, or transaction-level protection
     * across concurrent create requests.
     *
     * @param ctx        context describing the resource and key provider
     * @param completion returns {@link ResourceKeyResult} containing the plaintext DEK (base64)
     */
    void getOrCreateKey(GetOrCreateResourceKeyContext ctx,
                        ReturnValueCompletion<ResourceKeyResult> completion);

    /**
     * Load the existing resource encryption key material only.
     * <p>
     * Requires an {@code EncryptedResourceKeyRef} row and a usable secret reference already stored
     * for the resource. Does <strong>not</strong> insert a ref row and does <strong>not</strong> call
     * key-tool/KMS <em>create</em> APIs.
     * <p>
     * The implementation may still call key-tool/KMS <em>get/unwrap</em> for the <strong>existing</strong>
     * secret ref in order to return the plaintext DEK (for example defining the secret on the destination
     * host during hot migration). That RPC is read-side materialization, not secret creation.
     * <p>
     * On success, the implementation may update {@code EncryptedResourceKeyRef} provider columns to match
     * the resolved key provider when they have drifted (same behavior as the existing-key branch of
     * {@link #getOrCreateKey}).
     *
     * @param ctx same fields as {@link #getOrCreateKey}; identifies resource and provider
     * @return {@link ResourceKeyResult} with {@code createdNewKey == false} on success
     * @throws org.zstack.header.errorcode.OperationFailureException when the key cannot be loaded
     */
    ResourceKeyResult getKey(GetOrCreateResourceKeyContext ctx);

    /**
     * Roll back a newly created resource key during upper-layer workflow rollback.
     * <p>
     * When {@link ResourceKeyResult#isCreatedNewKey()} is true, the implementation deletes the
     * key-tool secret if one was materialized, then removes the {@code EncryptedResourceKeyRef} row
     * for the resource (same storage effect as detaching the key provider from the resource).
     * When {@code createdNewKey} is false (existing secret was reused), this is a no-op.
     */
    void rollbackCreatedKey(ResourceKeyResult result, Completion completion);

    class GetOrCreateResourceKeyContext {
        private String resourceUuid;
        private String resourceType;
        private String keyProviderUuid;
        private String keyProviderName;
        private String purpose;

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getKeyProviderUuid() {
            return keyProviderUuid;
        }

        public void setKeyProviderUuid(String keyProviderUuid) {
            this.keyProviderUuid = keyProviderUuid;
        }

        public String getKeyProviderName() {
            return keyProviderName;
        }

        public void setKeyProviderName(String keyProviderName) {
            this.keyProviderName = keyProviderName;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }
    }

    class ResourceKeyResult {
        private String resourceUuid;
        private String resourceType;
        private String keyProviderUuid;
        private String keyProviderName;
        private Integer keyVersion;
        private String dekBase64;
        private String secretRef;
        private boolean createdNewKey;

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getKeyProviderUuid() {
            return keyProviderUuid;
        }

        public void setKeyProviderUuid(String keyProviderUuid) {
            this.keyProviderUuid = keyProviderUuid;
        }

        public String getKeyProviderName() {
            return keyProviderName;
        }

        public void setKeyProviderName(String keyProviderName) {
            this.keyProviderName = keyProviderName;
        }

        public Integer getKeyVersion() {
            return keyVersion;
        }

        public void setKeyVersion(Integer keyVersion) {
            this.keyVersion = keyVersion;
        }

        public String getDekBase64() {
            return dekBase64;
        }

        public void setDekBase64(String dekBase64) {
            this.dekBase64 = dekBase64;
        }

        public String getSecretRef() {
            return secretRef;
        }

        public void setSecretRef(String secretRef) {
            this.secretRef = secretRef;
        }

        public boolean isCreatedNewKey() {
            return createdNewKey;
        }

        public void setCreatedNewKey(boolean createdNewKey) {
            this.createdNewKey = createdNewKey;
        }
    }
}
