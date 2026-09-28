package org.zstack.pciDevice;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountConstant;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceHelper;
import org.zstack.pciDevice.entity.PciDeviceOfferingVO;
import org.zstack.pciDevice.entity.PciDevicePciDeviceOfferingRefVO;
import org.zstack.pciDevice.entity.PciDeviceVO;
import org.zstack.pciDevice.entity.PciDeviceVO_;
import org.zstack.pciDevice.entity.specification.pci.PciDeviceSpecVO;
import org.zstack.pciDevice.header.PciDeviceChooser;
import org.zstack.pciDevice.header.PciDevicePassThroughState;
import org.zstack.pciDevice.header.PciDeviceState;
import org.zstack.pciDevice.header.PciDeviceStatus;
import org.zstack.pciDevice.header.PciDeviceType;
import org.zstack.pciDevice.header.specification.pci.PciDeviceSpecState;
import org.zstack.pciDevice.header.virtual.PciDeviceVirtStatus;
import org.zstack.pciDevice.message.PciDeviceTO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

public class PciDeviceTOHelper {
    private static CLogger logger = Utils.getLogger(PciDeviceTOHelper.class);
    private static final HostNetworkInterfaceHelper hostNetworkInterfaceHelper = new HostNetworkInterfaceHelper();

    public static void updatePciDeviceVO(PciDeviceVO vo, PciDeviceTO to) {
        vo.setName(!StringUtils.isEmpty(vo.getName()) ? vo.getName() : to.getName() + "_" + vo.getUuid().substring(0, 8));
        vo.setDescription(StringUtils.isEmpty(to.getDescription()) ? vo.getDescription() : to.getDescription());
        vo.setType(PciDeviceType.valueOf(to.getType()));
        vo.setPciDeviceAddress(to.getPciDeviceAddress());
        vo.setIommuGroup(vo.getHostUuid() + '_' + to.getIommuGroup());
        vo.setPassThroughState(getPciDevicePassThroughState(to, vo.getPassThroughState()));

        // If the pci device is SRIOV_VIRTUALIZABLE or VFIO_MDEV_VIRTUALIZABLE, then stick with it, do not change!
        PciDeviceVirtStatus oldStatus = vo.getVirtStatus();
        PciDeviceVirtStatus newStatus = PciDeviceVirtStatus.valueOf(to.getVirtStatus());
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
        spec.setName(to.getName());
        spec.setDescription(to.getDescription());
        spec.setVendorId(to.getVendorId());
        spec.setVendor(to.getVendor());
        spec.setDeviceId(to.getDeviceId());
        spec.setDevice(to.getDevice());
        spec.setSubvendorId(to.getSubvendorId());
        spec.setSubdeviceId(to.getSubdeviceId());
        spec.setRamSize(to.getRamSize());
        spec.setType(PciDeviceType.valueOf(to.getType()));
        spec.setVirtual(PciDeviceVirtStatus.SRIOV_VIRTUAL.isEqual(to.getVirtStatus()));
        spec.setMaxPartNum(to.getMaxPartNum());
        spec.setState(PciDeviceSpecState.Enabled);
        spec.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        return spec;
    }

    public static void updatePciDeviceSpec(PciDeviceSpecVO spec, PciDeviceTO to) {
        // do not update spec name and desc because user might have changed them
        spec.setVendorId(to.getVendorId());
        spec.setVendor(to.getVendor());
        spec.setDeviceId(to.getDeviceId());
        spec.setDevice(to.getDevice());
        spec.setSubvendorId(to.getSubvendorId());
        spec.setSubdeviceId(to.getSubdeviceId());
        spec.setRamSize(to.getRamSize());
        spec.setType(PciDeviceType.valueOf(to.getType()));
        spec.setVirtual(PciDeviceVirtStatus.SRIOV_VIRTUAL.isEqual(to.getVirtStatus()));
        spec.setMaxPartNum(to.getMaxPartNum());
        spec.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
    }

    public static PciDeviceVO getPciDeviceVO(PciDeviceTO to, final List<PciDeviceSpecVO> pciSpecs,
                                             final List<PciDeviceOfferingVO> offerings,
                                             List<PciDevicePciDeviceOfferingRefVO> refVOS) {
        PciDeviceVO pciVo = new PciDeviceVO();
        pciVo.setUuid(Platform.getUuid());
        pciVo.setHostUuid(to.getHostUuid());
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
        pciVo.setIommuGroup(to.getHostUuid() + '_' + to.getIommuGroup());
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
