package org.zstack.header.core.external.service;

import org.zstack.header.log.NoLogging;
import org.zstack.header.search.Inventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 1:31 AM
 */
@Inventory(mappingVOClass = ExternalServiceConfigurationVO.class)
public class ExternalServiceConfigurationInventory {
    public static final String MASKED_PASSWORD = "******";
    private String uuid;
    private String serviceType;
    @NoLogging
    private String configuration;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<ApplyExternalConfigurationResult> applyResults;

    public static ExternalServiceConfigurationInventory valueOf(ExternalServiceConfigurationVO vo) {
        return valueOf(vo, true);
    }

    public static ExternalServiceConfigurationInventory valueOf(ExternalServiceConfigurationVO vo, boolean maskSensitive) {
        ExternalServiceConfigurationInventory inv = new ExternalServiceConfigurationInventory();
        inv.setUuid(vo.getUuid());
        inv.setDescription(vo.getDescription());
        inv.setServiceType(vo.getServiceType());
        inv.setConfiguration(maskSensitive ? maskRemoteWritePassword(vo.getConfiguration()) : vo.getConfiguration());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<ExternalServiceConfigurationInventory> valueOf(Collection<ExternalServiceConfigurationVO> vos) {
        return valueOf(vos, true);
    }

    public static List<ExternalServiceConfigurationInventory> valueOf(Collection<ExternalServiceConfigurationVO> vos, boolean maskSensitive) {
        List<ExternalServiceConfigurationInventory> invs = new ArrayList<ExternalServiceConfigurationInventory>();
        for (ExternalServiceConfigurationVO vo : vos) {
            invs.add(valueOf(vo, maskSensitive));
        }
        return invs;
    }

    private static String maskRemoteWritePassword(String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            return configuration;
        }

        try {
            LinkedHashMap config = JSONObjectUtil.toObject(configuration, LinkedHashMap.class);
            Object remoteWrites = config.get("remote_write");
            if (remoteWrites instanceof Collection) {
                for (Object remoteWrite : (Collection) remoteWrites) {
                    if (!(remoteWrite instanceof Map)) {
                        continue;
                    }
                    Object basicAuth = ((Map) remoteWrite).get("basic_auth");
                    if (basicAuth instanceof Map && ((Map) basicAuth).containsKey("password")) {
                        ((Map) basicAuth).put("password", MASKED_PASSWORD);
                    }
                }
            }
            return JSONObjectUtil.toJsonString(config);
        } catch (Exception ignored) {
            return configuration.replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*(\")", "$1" + MASKED_PASSWORD + "$2");
        }
    }

    public static String restoreMaskedRemoteWritePassword(String configuration, String oldConfiguration) {
        if (configuration == null || configuration.isEmpty() || !configuration.contains(MASKED_PASSWORD)) {
            return configuration;
        }

        try {
            LinkedHashMap config = JSONObjectUtil.toObject(configuration, LinkedHashMap.class);
            Object remoteWrites = config.get("remote_write");
            if (!(remoteWrites instanceof List)) {
                return configuration;
            }

            List oldRemoteWrites = null;
            if (oldConfiguration != null && !oldConfiguration.isEmpty()) {
                LinkedHashMap oldConfig = JSONObjectUtil.toObject(oldConfiguration, LinkedHashMap.class);
                Object oldRemoteWriteObject = oldConfig.get("remote_write");
                if (oldRemoteWriteObject instanceof List) {
                    oldRemoteWrites = (List) oldRemoteWriteObject;
                }
            }

            boolean restored = false;
            List remoteWriteList = (List) remoteWrites;
            for (int i = 0; i < remoteWriteList.size(); i++) {
                Object remoteWrite = remoteWriteList.get(i);
                if (!(remoteWrite instanceof Map)) {
                    continue;
                }

                Object basicAuth = ((Map) remoteWrite).get("basic_auth");
                if (!(basicAuth instanceof Map) || !MASKED_PASSWORD.equals(((Map) basicAuth).get("password"))) {
                    continue;
                }

                Object oldPassword = getRemoteWritePassword(oldRemoteWrites, i);
                if (oldPassword == null) {
                    throw new IllegalArgumentException(String.format(
                            "remote_write[%s].basic_auth.password is masked but no saved password can be reused", i));
                }

                ((Map) basicAuth).put("password", oldPassword);
                restored = true;
            }

            return restored ? JSONObjectUtil.toJsonString(config) : configuration;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("masked remote_write password can only be reused with valid json configuration", e);
        }
    }

    private static Object getRemoteWritePassword(List remoteWrites, int index) {
        if (remoteWrites == null || remoteWrites.size() <= index) {
            return null;
        }

        Object remoteWrite = remoteWrites.get(index);
        if (!(remoteWrite instanceof Map)) {
            return null;
        }

        Object basicAuth = ((Map) remoteWrite).get("basic_auth");
        if (!(basicAuth instanceof Map)) {
            return null;
        }

        return ((Map) basicAuth).get("password");
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<ApplyExternalConfigurationResult> getApplyResults() {
        return applyResults;
    }

    public void setApplyResults(List<ApplyExternalConfigurationResult> applyResults) {
        this.applyResults = applyResults;
    }
}
