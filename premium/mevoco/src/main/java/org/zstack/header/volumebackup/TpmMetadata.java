package org.zstack.header.volumebackup;

/**
 * TPM metadata attached to a volume backup. Stored as JSON in VolumeBackupMetadata.attachments
 */
public class TpmMetadata {
    // from VmHostBackupFileVO / VmHostFileContentVO
    private String uuid;
    private String contentBase64;
    private String contentFormat;
    private long createDate;

    // from EncryptedResourceKeyRefVO
    private String providerName;
    private Integer keyVersion;
    private String kekRef;
    private String wrappedDek;
    private String algorithm;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }

    public String getContentFormat() {
        return contentFormat;
    }

    public void setContentFormat(String contentFormat) {
        this.contentFormat = contentFormat;
    }

    public long getCreateDate() {
        return createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
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

    public String getKekRef() {
        return kekRef;
    }

    public void setKekRef(String kekRef) {
        this.kekRef = kekRef;
    }

    public String getWrappedDek() {
        return wrappedDek;
    }

    public void setWrappedDek(String wrappedDek) {
        this.wrappedDek = wrappedDek;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
