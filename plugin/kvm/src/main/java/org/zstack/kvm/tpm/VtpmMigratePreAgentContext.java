package org.zstack.kvm.tpm;

import org.zstack.header.keyprovider.EncryptedResourceKeyManager.ResourceKeyResult;

import java.util.Objects;

/**
 * Context for vTPM secret preparation on the source KVM host immediately before the agent
 * {@code migrate-vm} call.
 * <p>
 * Built during {@link org.zstack.header.vm.VmInstanceMigrateExtensionPoint#preMigrateVm} (e.g. in
 * {@link KvmTpmExtensions}) to hold VM/source/destination hosts and resolved TPM/key/secret fields.
 * This replaces the previous split between a thin migrate wrapper
 * and a separate object meant for FlowChain {@code Map} storage.
 */
public final class VtpmMigratePreAgentContext {
    private final String vmUuid;
    private final String srcHostUuid;
    private final String dstHostUuid;
    private boolean enableKeyProvider = true;

    private String tpmUuid;
    private String providerUuid;
    private String providerName;
    private Integer keyVersion;
    private ResourceKeyResult resourceKeyResult;
    private String sourceSecretUuid;

    public VtpmMigratePreAgentContext(String vmUuid, String srcHostUuid, String dstHostUuid) {
        this.vmUuid = Objects.requireNonNull(vmUuid, "vmUuid");
        this.srcHostUuid = Objects.requireNonNull(srcHostUuid, "srcHostUuid");
        this.dstHostUuid = Objects.requireNonNull(dstHostUuid, "dstHostUuid");
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public String getSrcHostUuid() {
        return srcHostUuid;
    }

    public String getDstHostUuid() {
        return dstHostUuid;
    }

    public boolean isEnableKeyProvider() {
        return enableKeyProvider;
    }

    public void setEnableKeyProvider(boolean enableKeyProvider) {
        this.enableKeyProvider = enableKeyProvider;
    }

    public String getTpmUuid() {
        return tpmUuid;
    }

    public void setTpmUuid(String tpmUuid) {
        this.tpmUuid = tpmUuid;
    }

    public String getProviderUuid() {
        return providerUuid;
    }

    public void setProviderUuid(String providerUuid) {
        this.providerUuid = providerUuid;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(Integer keyVersion) {
        this.keyVersion = keyVersion;
    }

    public ResourceKeyResult getResourceKeyResult() {
        return resourceKeyResult;
    }

    public void setResourceKeyResult(ResourceKeyResult resourceKeyResult) {
        this.resourceKeyResult = resourceKeyResult;
    }

    public String getSourceSecretUuid() {
        return sourceSecretUuid;
    }

    public void setSourceSecretUuid(String sourceSecretUuid) {
        this.sourceSecretUuid = sourceSecretUuid;
    }
}
