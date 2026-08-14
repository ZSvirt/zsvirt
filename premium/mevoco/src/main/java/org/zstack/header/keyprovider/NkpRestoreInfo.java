package org.zstack.header.keyprovider;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.SDK;
import org.zstack.utils.gson.JSONObjectUtil;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

@PythonClassInventory
@SDK(sdkClassName = "NkpRestoreInfo")
public class NkpRestoreInfo implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String kdf;
    private String saltPolicy;
    private Integer currentVersion;
    private Long backupTime;

    public static NkpRestoreInfo fromJsonBytes(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("nkp restore content cannot be empty");
        }
        String json = new String(data, StandardCharsets.UTF_8);
        try {
            return JSONObjectUtil.toObject(json, NkpRestoreInfo.class);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("invalid nkp restore content: invalid json structure", e);
        }
    }

    public static NkpRestoreInfo valueOf(NkpVO vo) {
        NkpRestoreInfo info = new NkpRestoreInfo();
        info.setUuid(vo.getUuid());
        info.setName(vo.getName());
        info.setDescription(vo.getDescription());
        info.setKdf(vo.getKdf());
        info.setSaltPolicy(vo.getSaltPolicy());
        info.setCurrentVersion(vo.getCurrentVersion());
        info.setBackupTime(System.currentTimeMillis());
        return info;
    }

    public static NkpRestoreInfo __example__() {
        NkpRestoreInfo info = new NkpRestoreInfo();
        info.setUuid(DocUtils.createFixedUuid(NkpVO.class));
        info.setName("nkp-1");
        info.setDescription("example");
        info.setKdf(NkpKdf.HKDF_SHA256.toString());
        info.setSaltPolicy(NkpSaltPolicy.PROVIDER_NAME.toString());
        info.setCurrentVersion(1);
        info.setBackupTime(1700000000000L);
        return info;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKdf() {
        return kdf;
    }

    public void setKdf(String kdf) {
        this.kdf = kdf;
    }

    public String getSaltPolicy() {
        return saltPolicy;
    }

    public void setSaltPolicy(String saltPolicy) {
        this.saltPolicy = saltPolicy;
    }

    public Integer getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(Integer currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Long getBackupTime() {
        return backupTime;
    }

    public void setBackupTime(Long backupTime) {
        this.backupTime = backupTime;
    }
}
