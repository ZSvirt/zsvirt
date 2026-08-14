package org.zstack.pciDevice;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountConstant;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceHelper;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecState;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecVO;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by weiwang on 11/07/2017.
 */

public class PciDeviceTO implements Serializable {
    private static CLogger logger = Utils.getLogger(PciDeviceTO.class);
    private static final HostNetworkInterfaceHelper hostNetworkInterfaceHelper = new HostNetworkInterfaceHelper();

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

    public static void updatePciDeviceVO(PciDeviceVO vo, PciDeviceTO to) {
        vo.setName(!StringUtils.isEmpty(vo.getName()) ? vo.getName() : to.getName() + "_" + vo.getUuid().substring(0, 8));
        vo.setDescription(StringUtils.isEmpty(to.getDescription()) ? vo.getDescription() : to.getDescription());
        vo.setType(PciDeviceType.valueOf(to.getType()));
        vo.setPciDeviceAddress(to.getPciDeviceAddress());
        vo.setIommuGroup(vo.getHostUuid() + '_' + to.getIommuGroup());
        vo.setPassThroughState(getPciDevicePassThroughState(to, vo.getPassThroughState()));

        // If the pci device is SRIOV_VIRTUALIZABLE or VFIO_MDEV_VIRTUALIZABLE, then stick with it, do not change!
        PciDeviceVirtStatus oldStatus = vo.getVirtStatus();
        PciDeviceVirtStatus newStatus = PciDeviceVirtStatus.valueOf(to.virtStatus);
        List<PciDeviceVirtStatus> virtualizableStatus = Arrays.asList(PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE, PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE);
        if (!virtualizableStatus.contains(oldStatus)) {
            vo.setVirtStatus(newStatus);
        }

        // if pci type is Ethernet, will sync virt status from kvmagent
        if (vo.getType() == PciDeviceType.Ethernet_Controller) {
            vo.setVirtStatus(newStatus);
        }

        // reduce the influence of pci device address format changing from 00:00.0 to 0000:00:00.0
        if (StringUtils.isEmpty(vo.getParentUuid()) && StringUtils.isNotBlank(to.getParentAddress())) {
            vo.setParentUuid(Q.New(PciDeviceVO.class).eq(PciDeviceVO_.pciDeviceAddress, to.getParentAddress()).select(PciDeviceVO_.uuid).findValue());
        }
    }

    public static PciDeviceSpecVO createPciDeviceSpec(PciDeviceTO to) {
        PciDeviceSpecVO spec = new PciDeviceSpecVO();
        spec.setUuid(Platform.getUuid());
        spec.setName(to.name);
        spec.setDescription(to.description);
        spec.setVendorId(to.vendorId);
        spec.setVendor(to.getVendor());
        spec.setDeviceId(to.deviceId);
        spec.setDevice(to.getDevice());
        spec.setSubvendorId(to.subvendorId);
        spec.setSubdeviceId(to.subdeviceId);
        spec.setRamSize(to.ramSize);
        spec.setType(PciDeviceType.valueOf(to.type));
        spec.setVirtual(PciDeviceVirtStatus.SRIOV_VIRTUAL.isEqual(to.virtStatus));
        spec.setMaxPartNum(to.getMaxPartNum());
        spec.setState(PciDeviceSpecState.Enabled);
        spec.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        return spec;
    }

    public static void updatePciDeviceSpec(PciDeviceSpecVO spec, PciDeviceTO to) {
        // do not update spec name and desc because user might have changed them
        spec.setVendorId(to.vendorId);
        spec.setVendor(to.getVendor());
        spec.setDeviceId(to.deviceId);
        spec.setDevice(to.getDevice());
        spec.setSubvendorId(to.subvendorId);
        spec.setSubdeviceId(to.subdeviceId);
        spec.setRamSize(to.ramSize);
        spec.setType(PciDeviceType.valueOf(to.type));
        spec.setVirtual(PciDeviceVirtStatus.SRIOV_VIRTUAL.isEqual(to.virtStatus));
        spec.setMaxPartNum(to.getMaxPartNum());
        spec.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
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

    public static PciDeviceVO getPciDeviceVO(PciDeviceTO to, final List<PciDeviceSpecVO> pciSpecs,
                                             final List<PciDeviceOfferingVO> offerings,
                                             List<PciDevicePciDeviceOfferingRefVO> refVOS) {
        PciDeviceVO pciVo = new PciDeviceVO();
        pciVo.setUuid(Platform.getUuid());
        pciVo.setHostUuid(to.hostUuid);
        // e.g. S7150V_2048M_72fdddb8 or GTX-1080Ti_5fd418ee
        pciVo.setName(to.getName() + '_' + pciVo.getUuid().substring(0, 8));
        // e.g. Advanced Micro Devices, Inc. [AMD/ATI], Tonga XTV GL [FirePro S7150V], VGA compatible controller
        pciVo.setDescription(to.getDescription());
        pciVo.setVendorId(to.getVendorId());
        pciVo.setVendor(to.getVendor());
        pciVo.setDeviceId(to.getDeviceId());
        pciVo.setDevice(to.getDevice());
        pciVo.setSubvendorId(to.getSubvendorId());
        pciVo.setSubdeviceId(to.getSubdeviceId());
        pciVo.setPciDeviceAddress(to.getPciDeviceAddress());
        pciVo.setIommuGroup(to.hostUuid + '_' + to.getIommuGroup());
        pciVo.setState(PciDeviceState.Enabled);
        pciVo.setStatus(PciDeviceStatus.System);
        pciVo.setChooser(PciDeviceChooser.None);
        pciVo.setType(PciDeviceType.valueOf(to.getType()));
        pciVo.setPassThroughState(getPciDevicePassThroughState(to, null));
        pciVo.setAddonInfo(to.getAddonInfo());

        // we cannot collect info of mdev devices that are generated bypass zstack
        if (to.getVirtStatus().equals(PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED.toString())) {
            logger.warn(String.format("pci device[uuid:%s] has been virtualized bypass zstack", pciVo.getUuid()));
            pciVo.setVirtStatus(PciDeviceVirtStatus.VIRTUALIZED_BYPASS_ZSTACK);
        } else {
            pciVo.setVirtStatus(PciDeviceVirtStatus.valueOf(to.getVirtStatus()));
        }

        pciSpecs.stream().filter(s -> s.matchPciDevice(to)).findFirst().ifPresent(s -> pciVo.setPciSpecUuid(s.getUuid()));
        pciVo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        to.setUuid(pciVo.getUuid());

        // link new pci devices with existing pci device offerings, for billing only
        List<PciDeviceOfferingVO> matchedOfferings = offerings.stream()
                .filter(offering -> offering.matchPciDevice(pciVo))
                .collect(Collectors.toList());
        if (!matchedOfferings.isEmpty()) {
            pciVo.setStatus(PciDeviceStatus.Active);
        }

        refVOS.addAll(matchedOfferings.stream()
                .map(offering -> {
                    PciDevicePciDeviceOfferingRefVO refVO = new PciDevicePciDeviceOfferingRefVO();
                    refVO.setPciDeviceOfferingUuid(offering.getUuid());
                    refVO.setPciDeviceUuid(pciVo.getUuid());
                    return refVO;
                }).collect(Collectors.toList()));

        logger.debug(String.format("create new pci device[uuid:%s, name:%s, address:%s]",
                pciVo.getUuid(), pciVo.getName(), pciVo.getPciDeviceAddress()));

        return pciVo;
    }

    private static PciDevicePassThroughState getPciDevicePassThroughState(PciDeviceTO to, PciDevicePassThroughState oldState) {
        if (PciDeviceType.Ethernet_Controller.toString().equals(to.getType())) {
            // VF
            if (StringUtils.isNotBlank(to.getParentAddress())) {
                return PciDevicePassThroughState.Enabled;
            }

            // PF
            if (PciDeviceVirtStatus.SRIOV_VIRTUALIZED.toString().equals(to.getVirtStatus())) {
                return PciDevicePassThroughState.Disabled;
            }

            String interfaceName = Q.New(HostNetworkInterfaceVO.class)
                    .eq(HostNetworkInterfaceVO_.hostUuid, to.getHostUuid())
                    .eq(HostNetworkInterfaceVO_.pciDeviceAddress, to.getPciDeviceAddress())
                    .select(HostNetworkInterfaceVO_.interfaceName)
                    .isNull(HostNetworkInterfaceVO_.bondingUuid)
                    .findValue();

            // bonding slave or error
            if (interfaceName == null) {
                return PciDevicePassThroughState.Disabled;
            }

            // occupied by l2 network
            if (hostNetworkInterfaceHelper.isPhysicalInterfaceOccupiedByL2Network(interfaceName, to.getHostUuid())) {
                return PciDevicePassThroughState.Disabled;
            }

            // no longer excluded but old state is still disabled
            if (PciDevicePassThroughState.Disabled.equals(oldState)) {
                return PciDevicePassThroughState.Available;
            }
        }

        return oldState != null ? oldState : getPciDevicePassThroughStateByType(PciDeviceType.valueOf(to.getType()));
    }

    private static PciDevicePassThroughState getPciDevicePassThroughStateByType(PciDeviceType type) {
        if (PciDeviceType.enabledPassThroughPciDeviceTypes.contains(type)) {
            return PciDevicePassThroughState.Enabled;
        } else if (PciDeviceType.disabledPassThroughPciDeviceTypes.contains(type)) {
            return PciDevicePassThroughState.Disabled;
        } else {
            return PciDevicePassThroughState.Available;
        }
    }
}
