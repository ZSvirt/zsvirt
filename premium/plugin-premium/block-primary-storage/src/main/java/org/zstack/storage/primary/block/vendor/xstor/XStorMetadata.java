package org.zstack.storage.primary.block.vendor.xstor;

import org.zstack.header.log.NoLogging;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/15 11:02
 */
public class XStorMetadata {
    private String ip;

    private Integer port;

    private String username;

    @NoLogging
    private String password;

    private List<AccessZoneRsp.AccessZone> accessZones = new ArrayList<>();

    private List<StoragePoolRsp.StoragePool> storagePools = new ArrayList<>();

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public List<StoragePoolRsp.StoragePool> getStoragePools() {
        return storagePools;
    }

    public void setStoragePools(List<StoragePoolRsp.StoragePool> storagePools) {
        this.storagePools = storagePools;
    }

    public List<AccessZoneRsp.AccessZone> getAccessZones() {
        return accessZones;
    }

    public void setAccessZones(List<AccessZoneRsp.AccessZone> accessZones) {
        this.accessZones = accessZones;
    }
}
