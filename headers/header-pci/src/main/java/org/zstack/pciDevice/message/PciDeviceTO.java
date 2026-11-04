package org.zstack.pciDevice.message;

import org.apache.commons.lang.StringUtils;
import org.zstack.pciDevice.entity.PciDeviceVO;

import java.io.Serializable;
import java.util.*;

/**
 * Created by weiwang on 11/07/2017.
 */

public class PciDeviceTO implements Serializable {
    private String uuid;

    private String name;

    private String description;

    private String hostUuid;

    private String vendorId;

    private String vendor;

    private String deviceId;

    private String device;

    private String subvendorId;

    private String subdeviceId;

    private String pciDeviceAddress;

    private String parentAddress;

    private String parentUuid;

    private String iommuGroup;

    private String type;

    private String virtStatus;
    
    private String chooser;

    private int maxPartNum = 0;

    private String ramSize;

    private List<Map<String, String>> mdevSpecifications;

    private Map<String, String> addonInfo = new HashMap<>();

    public PciDeviceTO() {
    }

    public PciDeviceTO(PciDeviceVO vo) {
        this.setUuid(vo.getUuid());
        this.setHostUuid(vo.getHostUuid());
        this.setVendorId(vo.getVendorId());
        this.setVendor(vo.getVendor());
        this.setDeviceId(vo.getDeviceId());
        this.setDevice(vo.getDevice());
        this.setSubvendorId(vo.getSubvendorId());
        this.setSubdeviceId(vo.getSubdeviceId());
        this.setPciDeviceAddress(vo.getPciDeviceAddress());
        if (vo.getType() != null) {
            this.setType(vo.getType().toString());
        }
    }

    public static PciDeviceTO valueOf(PciDeviceVO vo) {
        return new PciDeviceTO(vo);
    }

    public static List<PciDeviceTO> valueOf(List<PciDeviceVO> vos) {
        List<PciDeviceTO> tos = new ArrayList<>();

        for (PciDeviceVO vo : vos) {
            tos.add(new PciDeviceTO(vo));
        }
        return tos;
    }

    public static boolean pciDeviceAddressEquals(String addr1, String addr2) {
        if (addr1.split(":").length < 3) {
            addr1 = "0000:" + addr1;
        }
        if (addr2.split(":").length < 3) {
            addr2 = "0000:" + addr2;
        }
        return addr1.equals(addr2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PciDeviceTO)) return false;

        PciDeviceTO that = (PciDeviceTO) o;
        if (!hostUuid.equals(that.hostUuid)) return false;
        if (!vendorId.equals(that.vendorId)) return false;
        if (!deviceId.equals(that.deviceId)) return false;
        if (StringUtils.isNotBlank(subvendorId) ? !subvendorId.equals(that.getSubvendorId()) : StringUtils.isNotBlank(that.getSubvendorId())) return false;
        if (StringUtils.isNotBlank(subdeviceId) ? !subdeviceId.equals(that.getSubdeviceId()) : StringUtils.isNotBlank(that.getSubdeviceId())) return false;
        // before 3.5.2, pciDeviceAddress is like 01:00.0, it changed into 0000:01:00.0 since 3.5.2
        return pciDeviceAddressEquals(pciDeviceAddress, that.pciDeviceAddress);
    }

    @Override
    public int hashCode() {
        int result = hostUuid.hashCode();
        result = 31 * result + vendorId.hashCode();
        result = 31 * result + deviceId.hashCode();
        result = 31 * result + (subvendorId != null ? subvendorId.hashCode() : 0);
        result = 31 * result + (subdeviceId != null ? subdeviceId.hashCode() : 0);
        result = 31 * result + ((pciDeviceAddress.split(":").length < 3) ? ("0000:" + pciDeviceAddress).hashCode() : pciDeviceAddress.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "PciDeviceTO{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", hostUuid='" + hostUuid + '\'' +
                ", vendorId='" + vendorId + '\'' +
                ", vendorId='" + vendor + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", deviceId='" + device + '\'' +
                ", subvendorId='" + subvendorId + '\'' +
                ", subdeviceId='" + subdeviceId + '\'' +
                ", pciDeviceAddress='" + pciDeviceAddress + '\'' +
                ", parentAddress='" + parentAddress + '\'' +
                ", iommuGroup='" + iommuGroup + '\'' +
                ", type=" + type +
                ", virtStatus=" + virtStatus +
                ", chooser=" + chooser +
                ", maxPartNum='" + maxPartNum + '\'' +
                ", ramSize='" + ramSize + '\'' +
                ", mdevSpecifications=" + mdevSpecifications +
                ", mdevSpecifications=" + addonInfo +
                '}';
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

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSubvendorId() {
        return subvendorId;
    }

    public void setSubvendorId(String subvendorId) {
        this.subvendorId = subvendorId;
    }

    public String getSubdeviceId() {
        return subdeviceId;
    }

    public void setSubdeviceId(String subdeviceId) {
        this.subdeviceId = subdeviceId;
    }

    public String getPciDeviceAddress() {
        return pciDeviceAddress;
    }

    public void setPciDeviceAddress(String pciDeviceAddress) {
        this.pciDeviceAddress = pciDeviceAddress;
    }

    public String getParentAddress() {
        return parentAddress;
    }

    public void setParentAddress(String parentAddress) {
        this.parentAddress = parentAddress;
    }

    public String getIommuGroup() {
        return iommuGroup;
    }

    public void setIommuGroup(String iommuGroup) {
        this.iommuGroup = iommuGroup;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVirtStatus() {
        return virtStatus;
    }

    public void setVirtStatus(String virtStatus) {
        this.virtStatus = virtStatus;
    }

    public String getChooser() {
        return chooser;
    }

    public void setChooser(String chooser) {
        this.chooser = chooser;
    }

    public int getMaxPartNum() {
        return maxPartNum;
    }

    public void setMaxPartNum(int maxPartNum) {
        this.maxPartNum = maxPartNum;
    }

    public String getRamSize() {
        return ramSize;
    }

    public void setRamSize(String ramSize) {
        this.ramSize = ramSize;
    }

    public List<Map<String, String>> getMdevSpecifications() {
        return mdevSpecifications;
    }

    public void setMdevSpecifications(List<Map<String, String>> mdevSpecifications) {
        this.mdevSpecifications = mdevSpecifications;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Map<String, String> getAddonInfo() {
        return addonInfo;
    }

    public void setAddonInfo(Map<String, String> addonInfo) {
        this.addonInfo = addonInfo;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }
}
