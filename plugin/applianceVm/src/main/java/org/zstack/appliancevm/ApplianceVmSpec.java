package org.zstack.appliancevm;

import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.utils.JsonWrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplianceVmSpec implements Serializable {
    private ApplianceVmNicSpec managementNic;
    private List<ApplianceVmNicSpec> additionalNics;
    private InstanceOfferingInventory instanceOffering;
    private List<DiskOfferingInventory> dataDisk;
    private ImageInventory template;
    private String name;
    private String uuid;
    private String description;
    private L3NetworkInventory defaultL3Network;
    private L3NetworkInventory defaultRouteL3Network;
    private ApplianceVmType applianceVmType;
    private String accountUuid;
    private boolean syncCreate;
    private List<ApplianceVmFirewallRuleInventory> firewallRules = new ArrayList<ApplianceVmFirewallRuleInventory>();
    private List<String> inherentSystemTags;
    private List<String> nonInherentSystemTags;
    private String sshUsername = "root";
    private int sshPort = 22;
    private int agentPort = 7759;
    private Map<String,Map<Integer, String>> staticVip = new HashMap<>();
    private Map<String,Map<Integer, String>> staticIp = new HashMap<>();
    private ApplianceVmHaSpec haSpec;
    private String requiredZoneUuid;
    private String requiredClusterUuid;
    private String requiredHostUuid;
    private String primaryStorageUuidForRootVolume;
    private List<String> rootVolumeSystemTags;
    @NoJsonSchema
    private Map<String, JsonWrapper> applianceData = new HashMap<>();

    public int getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(int agentPort) {
        this.agentPort = agentPort;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public List<String> getNonInherentSystemTags() {
        return nonInherentSystemTags;
    }

    public void setNonInherentSystemTags(List<String> nonInherentSystemTags) {
        this.nonInherentSystemTags = nonInherentSystemTags;
    }

    public List<String> getInherentSystemTags() {
        return inherentSystemTags;
    }

    public void setInherentSystemTags(List<String> inherentSystemTags) {
        this.inherentSystemTags = inherentSystemTags;
    }

    public List<ApplianceVmFirewallRuleInventory> getFirewallRules() {
        return firewallRules;
    }

    public void setFirewallRules(List<ApplianceVmFirewallRuleInventory> firewallRules) {
        this.firewallRules = firewallRules;
    }

    public ApplianceVmNicSpec getManagementNic() {
        return managementNic;
    }

    public void setManagementNic(ApplianceVmNicSpec managementNic) {
        this.managementNic = managementNic;
    }

    public List<ApplianceVmNicSpec> getAdditionalNics() {
        if (additionalNics == null) {
            additionalNics = new ArrayList<ApplianceVmNicSpec>();
        }
        return additionalNics;
    }

    public void setAdditionalNics(List<ApplianceVmNicSpec> additionalNics) {
        this.additionalNics = additionalNics;
    }

    public InstanceOfferingInventory getInstanceOffering() {
        return instanceOffering;
    }

    public void setInstanceOffering(InstanceOfferingInventory instanceOffering) {
        this.instanceOffering = instanceOffering;
    }

    public List<DiskOfferingInventory> getDataDisk() {
        return dataDisk;
    }

    public void setDataDisk(List<DiskOfferingInventory> dataDisk) {
        this.dataDisk = dataDisk;
    }

    public ImageInventory getTemplate() {
        return template;
    }

    public void setTemplate(ImageInventory template) {
        this.template = template;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public L3NetworkInventory getDefaultRouteL3Network() {
        return defaultRouteL3Network;
    }

    public void setDefaultRouteL3Network(L3NetworkInventory defaultRouteL3Network) {
        this.defaultRouteL3Network = defaultRouteL3Network;
    }

    public ApplianceVmType getApplianceVmType() {
        return applianceVmType;
    }

    public void setApplianceVmType(ApplianceVmType applianceVmType) {
        this.applianceVmType = applianceVmType;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public boolean isSyncCreate() {
        return syncCreate;
    }

    public void setSyncCreate(boolean syncCreate) {
        this.syncCreate = syncCreate;
    }

    public ApplianceVmHaSpec getHaSpec() {
        return haSpec;
    }

    public void setHaSpec(ApplianceVmHaSpec haSpec) {
        this.haSpec = haSpec;
    }

    public Map<String,Map<Integer, String>> getStaticVip() {
        return staticVip;
    }

    public void setStaticVip(Map <String,Map<Integer, String>> staticVip) {
        this.staticVip = staticVip;
    }

    public Map<String,Map<Integer, String>> getStaticIp() {
        return staticIp;
    }

    public void setStaticIp(Map <String,Map<Integer, String>> staticIp) {
        this.staticIp = staticIp;
    }

    public L3NetworkInventory getDefaultL3Network() {
        return defaultL3Network;
    }

    public void setDefaultL3Network(L3NetworkInventory defaultL3Network) {
        this.defaultL3Network = defaultL3Network;
    }

    public String getRequiredZoneUuid() { return requiredZoneUuid; }

    public void setRequiredZoneUuid(String requiredZoneUuid) { this.requiredZoneUuid = requiredZoneUuid; }

    public String getRequiredClusterUuid() { return requiredClusterUuid; }

    public void setRequiredClusterUuid(String requiredClusterUuid) { this.requiredClusterUuid = requiredClusterUuid; }

    public String getRequiredHostUuid() { return requiredHostUuid; }

    public void setRequiredHostUuid(String requiredHostUuid) { this.requiredHostUuid = requiredHostUuid; }

    public String getPrimaryStorageUuidForRootVolume() { return primaryStorageUuidForRootVolume; }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) { this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume; }

    public List<String> getRootVolumeSystemTags() { return rootVolumeSystemTags; }

    public void setRootVolumeSystemTags(List<String> rootVolumeSystemTags) { this.rootVolumeSystemTags = rootVolumeSystemTags; }

    public <T> T getExtensionData(String key, Class<?> clazz) {
        JsonWrapper<T> wrapper = applianceData.get(key);
        if (wrapper == null) {
            return null;
        }

        return wrapper.get();
    }

    public void putExtensionData(String key, Object data) {
        this.applianceData.put(key, JsonWrapper.wrap(data));
    }
}
