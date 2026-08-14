package org.zstack.pciDevice;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.identity.Quota;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.identity.QuotaUtil;
import org.zstack.identity.ResourceHelper;
import org.zstack.pciDevice.specification.mdev.*;
import org.zstack.pciDevice.specification.pci.*;
import org.zstack.pciDevice.virtual.vfio_mdev.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019-06-05.
 */
public class PciDeviceQuotaUtils {
    private static final CLogger logger = Utils.getLogger(PciDeviceQuotaUtils.class);

    // get requested number of different types of pci devices
    // we assume the type of device to be GPU_Video_Controller/GPU_3D_Controller/Moxa_Device etc
    // GPU_Audio_Controller/GPU_USB_Controller/GPU_Serial_Controller is not allowed
    public static Map<String, Long> getRequestPciDeviceNumber(List<String> systemTags) {
        Map<String, Long> requestDevices = new HashMap<>();
        if (systemTags == null || systemTags.isEmpty()) {
            return requestDevices;
        }

        for (String tag : systemTags) {
            if (PciDeviceSystemTags.PCI_DEVICE.isMatch(tag)) {
                String pciDeviceUuid = PciDeviceSystemTags.PCI_DEVICE.getTokenByTag(tag, PciDeviceSystemTags.PCI_DEVICE_TOKEN);
                PciDeviceType type = Q.New(PciDeviceVO.class)
                        .eq(PciDeviceVO_.uuid, pciDeviceUuid)
                        .select(PciDeviceVO_.type)
                        .findValue();
                String typeString = type.toString();
                if (requestDevices.containsKey(typeString)) {
                    requestDevices.put(typeString, requestDevices.get(typeString) + 1L);
                } else {
                    requestDevices.put(typeString, 1L);
                }
            } else if (PciDeviceSystemTags.PCI_DEVICE_SPEC.isMatch(tag)) {
                String pciSpecUuid = PciDeviceSystemTags.PCI_DEVICE_SPEC.getTokenByTag(tag, PciDeviceSystemTags.PCI_DEVICE_SPEC_UUID_TOKEN);
                String pciDeviceNumber = PciDeviceSystemTags.PCI_DEVICE_SPEC.getTokenByTag(tag, PciDeviceSystemTags.PCI_DEVICE_NUMBER_TOKEN);
                PciDeviceType type = Q.New(PciDeviceSpecVO.class)
                        .eq(PciDeviceSpecVO_.uuid, pciSpecUuid)
                        .select(PciDeviceSpecVO_.type)
                        .findValue();
                String typeString = type.toString();
                Long number = Long.valueOf(pciDeviceNumber);
                if (requestDevices.containsKey(typeString)) {
                    requestDevices.put(typeString, requestDevices.get(typeString) + number);
                } else {
                    requestDevices.put(typeString, number);
                }
            } else if (MdevDeviceSystemTags.MDEV_DEVICE.isMatch(tag)) {
                String mdevDeviceUuid = MdevDeviceSystemTags.MDEV_DEVICE.getTokenByTag(tag, MdevDeviceSystemTags.MDEV_DEVICE_TOKEN);
                MdevDeviceType type = Q.New(MdevDeviceVO.class)
                        .eq(MdevDeviceVO_.uuid, mdevDeviceUuid)
                        .select(MdevDeviceVO_.type)
                        .findValue();
                String typeString = type.toString();
                if (requestDevices.containsKey(typeString)) {
                    requestDevices.put(typeString, requestDevices.get(typeString) + 1L);
                } else {
                    requestDevices.put(typeString, 1L);
                }
            } else if (MdevDeviceSystemTags.MDEV_DEVICE_SPEC.isMatch(tag)) {
                String mdevSpecUuid = MdevDeviceSystemTags.MDEV_DEVICE_SPEC.getTokenByTag(tag, MdevDeviceSystemTags.MDEV_DEVICE_SPEC_UUID_TOKEN);
                String mdevDeviceNumber = MdevDeviceSystemTags.MDEV_DEVICE_SPEC.getTokenByTag(tag, MdevDeviceSystemTags.MDEV_DEVICE_NUMBER_TOKEN);
                MdevDeviceType type = Q.New(MdevDeviceSpecVO.class)
                        .eq(MdevDeviceSpecVO_.uuid, mdevSpecUuid)
                        .select(MdevDeviceSpecVO_.type)
                        .findValue();
                String typeString = type.toString();
                Long number = Long.valueOf(mdevDeviceNumber);
                if (requestDevices.containsKey(typeString)) {
                    requestDevices.put(typeString, requestDevices.get(typeString) + number);
                } else {
                    requestDevices.put(typeString, number);
                }
            }
        }
        logger.debug("getRequestPciDeviceNumber returns: " + requestDevices);
        return requestDevices;
    }

    // return used number of different types of pci device
    public static Map<String, Long> getUsedPci(String accountUuid) {
        // init used
        Map<String, Long> used = new HashMap<>();
        used.put(PciDeviceConstants.GPU_Video_Controller, 0L);
        used.put(PciDeviceConstants.Generic, 0L);

        // get all non-new-created vms of account
        List<String> vmUuids = ResourceHelper.findOwnResourceUuidList(VmInstanceVO.class, accountUuid);
        if (vmUuids.isEmpty()) {
            return used;
        }

        // get all used pci devices number
        List<VmInstancePciDeviceSpecRefVO> pciSpecRefs = Q.New(VmInstancePciDeviceSpecRefVO.class)
                .in(VmInstancePciDeviceSpecRefVO_.vmInstanceUuid, vmUuids)
                .list();

        List<Integer> pciSpecDevNums = pciSpecRefs.stream()
                .map(VmInstancePciDeviceSpecRefVO::getPciDeviceNumber).collect(Collectors.toList());

        long pciSpecDevNum = pciSpecDevNums.stream().mapToInt(Integer::intValue).sum();

        List<String> attachedIommuGroups = Q.New(PciDeviceVO.class)
                .in(PciDeviceVO_.vmInstanceUuid, vmUuids)
                .groupBy(PciDeviceVO_.iommuGroup)
                .select(PciDeviceVO_.iommuGroup)
                .listValues();

        List<String> attachedSpecPcis = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.chooser, PciDeviceChooser.Spec)
                .in(PciDeviceVO_.vmInstanceUuid, vmUuids)
                .select(PciDeviceVO_.uuid)
                .listValues();

        if (!attachedSpecPcis.isEmpty()) {
            List<String> attachedSpecIommuGroups = Q.New(PciDeviceVO.class)
                    .in(PciDeviceVO_.uuid, attachedSpecPcis)
                    .groupBy(PciDeviceVO_.iommuGroup)
                    .select(PciDeviceVO_.iommuGroup)
                    .listValues();

            attachedIommuGroups.removeAll(attachedSpecIommuGroups);
        }

        Long allUsedPciNum = pciSpecDevNum + attachedIommuGroups.size();

        // get all used mdev devices number
        List<VmInstanceMdevDeviceSpecRefVO> mdevSpecRefs = Q.New(VmInstanceMdevDeviceSpecRefVO.class)
                .in(VmInstanceMdevDeviceSpecRefVO_.vmInstanceUuid, vmUuids)
                .list();

        List<Integer> mdevSpecDevNums = mdevSpecRefs.stream()
                .map(VmInstanceMdevDeviceSpecRefVO::getMdevDeviceNumber).collect(Collectors.toList());

        long mdevSpecDevNum = mdevSpecDevNums.stream().mapToInt(Integer::intValue).sum();

        List<String> attachedMdevs = Q.New(MdevDeviceVO.class)
                .in(MdevDeviceVO_.vmInstanceUuid, vmUuids)
                .select(MdevDeviceVO_.uuid)
                .listValues();

        List<String> attachedSpecMdevs = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.chooser, MdevDeviceChooser.Spec)
                .in(MdevDeviceVO_.vmInstanceUuid, vmUuids)
                .select(MdevDeviceVO_.uuid)
                .listValues();

        if (!attachedMdevs.isEmpty()) {
            attachedMdevs.removeAll(attachedSpecMdevs);
        }

        Long allUsedMdevNum = mdevSpecDevNum + attachedMdevs.size();

        // get used gpu (pci) number
        List<String> pciSpecUuids = pciSpecRefs.stream()
                .map(VmInstancePciDeviceSpecRefVO::getPciSpecUuid).collect(Collectors.toList());

        if (pciSpecUuids.isEmpty()) pciSpecUuids.add(Platform.FAKE_UUID);
        List<String> gpuPciSpecUuids = Q.New(PciDeviceSpecVO.class)
                .in(PciDeviceSpecVO_.uuid, pciSpecUuids)
                .in(PciDeviceSpecVO_.type, Arrays.asList(PciDeviceType.GPU_Video_Controller, PciDeviceType.GPU_3D_Controller))
                .select(PciDeviceSpecVO_.uuid)
                .listValues();

        if (gpuPciSpecUuids.isEmpty()) gpuPciSpecUuids.add(Platform.FAKE_UUID);
        List<Integer> gpuPciSpecDevNums = Q.New(VmInstancePciDeviceSpecRefVO.class)
                .in(VmInstancePciDeviceSpecRefVO_.vmInstanceUuid, vmUuids)
                .in(VmInstancePciDeviceSpecRefVO_.pciSpecUuid, gpuPciSpecUuids)
                .select(VmInstancePciDeviceSpecRefVO_.pciDeviceNumber)
                .listValues();

        long gpuPciSpecDevNum = gpuPciSpecDevNums.stream().mapToInt(Integer::intValue).sum();

        List<String> attachedGpuPcis = Q.New(PciDeviceVO.class)
                .in(PciDeviceVO_.vmInstanceUuid, vmUuids)
                .in(PciDeviceSpecVO_.type, Arrays.asList(PciDeviceType.GPU_Video_Controller, PciDeviceType.GPU_3D_Controller))
                .select(PciDeviceVO_.uuid)
                .listValues();

        List<String> attachedSpecGpuPcis = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.chooser, PciDeviceChooser.Spec)
                .in(PciDeviceVO_.vmInstanceUuid, vmUuids)
                .in(PciDeviceVO_.pciSpecUuid, gpuPciSpecUuids)
                .select(PciDeviceVO_.uuid)
                .listValues();

        if (!attachedGpuPcis.isEmpty()) {
            attachedGpuPcis.removeAll(attachedSpecGpuPcis);
        }

        Long usedGpuPciNum = gpuPciSpecDevNum + attachedGpuPcis.size();

        // get used gpu (mdev) number
        List<String> mdevSpecUuids = mdevSpecRefs.stream()
                .map(VmInstanceMdevDeviceSpecRefVO::getMdevSpecUuid).collect(Collectors.toList());

        if (mdevSpecUuids.isEmpty()) mdevSpecUuids.add(Platform.FAKE_UUID);
        List<String> gpuMdevSpecUuids = Q.New(MdevDeviceSpecVO.class)
                .in(MdevDeviceSpecVO_.uuid, mdevSpecUuids)
                .eq(MdevDeviceSpecVO_.type, MdevDeviceType.GPU_Video_Controller)
                .select(MdevDeviceSpecVO_.uuid)
                .listValues();

        if (gpuMdevSpecUuids.isEmpty()) gpuMdevSpecUuids.add(Platform.FAKE_UUID);
        List<Integer> gpuMdevSpecDevNums = Q.New(VmInstanceMdevDeviceSpecRefVO.class)
                .in(VmInstanceMdevDeviceSpecRefVO_.vmInstanceUuid, vmUuids)
                .in(VmInstanceMdevDeviceSpecRefVO_.mdevSpecUuid, gpuMdevSpecUuids)
                .select(VmInstanceMdevDeviceSpecRefVO_.mdevDeviceNumber)
                .listValues();

        long gpuMdevSpecDevNum = gpuMdevSpecDevNums.stream().mapToInt(Integer::intValue).sum();

        List<String> attachedGpuMdevs = Q.New(MdevDeviceVO.class)
                .in(MdevDeviceVO_.vmInstanceUuid, vmUuids)
                .eq(MdevDeviceVO_.type, MdevDeviceType.GPU_Video_Controller)
                .select(MdevDeviceVO_.uuid)
                .listValues();

        List<String> attachedSpecGpuMdevs = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.chooser, MdevDeviceChooser.Spec)
                .in(MdevDeviceVO_.vmInstanceUuid, vmUuids)
                .in(MdevDeviceVO_.mdevSpecUuid, gpuMdevSpecUuids)
                .select(MdevDeviceVO_.uuid)
                .listValues();

        if (!attachedGpuMdevs.isEmpty()) {
            attachedGpuMdevs.removeAll(attachedSpecGpuMdevs);
        }

        Long usedGpuMdevNum = gpuMdevSpecDevNum + attachedGpuMdevs.size();

        /*
         *  GPU USED NUMBER
         */
        used.put(PciDeviceConstants.GPU_Video_Controller, usedGpuPciNum + usedGpuMdevNum);

        // get used generic number
        used.put(PciDeviceConstants.Generic, allUsedPciNum + allUsedMdevNum - usedGpuPciNum - usedGpuMdevNum);

        logger.debug(String.format("account[uuid:%s] used pci devices: %s", accountUuid, used));
        return used;
    }

    // check generic pci device quota
    public static void checkGeneric(String accountUuid, Long requestNum, Map<String, Quota.QuotaPair> pairs) {
        long pciQuota = pairs.get(PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER).getValue();
        long pciUsed = getUsedPci(accountUuid).get(PciDeviceConstants.Generic);

        if (pciUsed + requestNum > pciQuota) {
            throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                    accountUuid, PciDeviceConstants.GENERIC_PCI_DEVICE_NUMBER, pciQuota));
        }
    }

    // check gpu device quota
    public static void checkGpu(String accountUuid, Long requestNum, Map<String, Quota.QuotaPair> pairs) {
        long gpuQuota = pairs.get(PciDeviceConstants.GPU_NUMBER).getValue();
        long gpuUsed = getUsedPci(accountUuid).get(PciDeviceConstants.GPU_Video_Controller);

        if (gpuUsed + requestNum > gpuQuota) {
            throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                    accountUuid, PciDeviceConstants.GPU_NUMBER, gpuQuota));
        }
    }

    public static void check(String accountUuid, String typeString, Long requestNum, Map<String, Quota.QuotaPair> pairs) {
        switch (typeString) {
            case PciDeviceConstants.GPU_Video_Controller:
            case PciDeviceConstants.GPU_3D_Controller:
                checkGpu(accountUuid, requestNum, pairs);
                break;
            case PciDeviceConstants.GPU_Audio_Controller:
            case PciDeviceConstants.GPU_USB_Controller:
            case PciDeviceConstants.GPU_Serial_Controller:
                break;
            default:
                checkGeneric(accountUuid, requestNum, pairs);
                break;
        }
    }
}
