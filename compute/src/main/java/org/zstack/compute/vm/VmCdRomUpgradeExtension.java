package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.identity.Account;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lining on 2019/1/6.
 *
 * add cdRom To historical vm
 */
public class VmCdRomUpgradeExtension implements ManagementNodeReadyExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private ResourceDestinationMaker destinationMaker;

    @Override
    public void managementNodeReady() {
        addCdRomToHistoricalVm();
    }

    private void addCdRomToHistoricalVm() {
        if (!VmCdRomGlobalProperty.addCdRomToHistoricalVm) {
            return;
        }

        List<String> vmUuids = getTargetVmUuids();
        if (vmUuids.isEmpty()){
            return;
        }

        List<Tuple> vmUuidPlatforms = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.platform)
                .in(VmInstanceVO_.uuid, vmUuids)
                .listTuple();

        for (Tuple vmTuple : vmUuidPlatforms) {
            String vmUuid = vmTuple.get(0, String.class);
            String vmPlatform = vmTuple.get(1, String.class);

            if (vmPlatform != null && ImagePlatform.Windows.toString().equalsIgnoreCase(vmPlatform)) {
                addCdRomForWindowsVm(vmUuid);
                continue;
            }

            addCdRomForLinuxAndOthersVm(vmUuid);
        }
    }

    private void addCdRomForWindowsVm(String vmUuid) {
        List<VmCdRomVO> cdRomVOS = new ArrayList<>();
        String acntUuid = Account.getAccountUuidOfResource(vmUuid);
        Map<Integer, String> vmDeviceIdIsoMap = getVmIsoMap(vmUuid);

        for (int deviceId = 0; deviceId < VmInstanceConstant.MAXIMUM_CDROM_NUMBER; deviceId++) {
            VmCdRomVO cdRomVO = new VmCdRomVO();
            cdRomVO.setUuid(Platform.getUuid());
            cdRomVO.setDeviceId(deviceId);
            cdRomVO.setIsoUuid(vmDeviceIdIsoMap.get(deviceId));
            cdRomVO.setOccupant(cdRomVO.getIsoUuid() == null ? null : VmInstanceConstant.VM_CDROM_OCCUPANT_ISO);
            cdRomVO.setVmInstanceUuid(vmUuid);
            cdRomVO.setName(String.format("vm-%s-cdRom", vmUuid));
            cdRomVO.setAccountUuid(acntUuid);
            cdRomVOS.add(cdRomVO);
        }

        dbf.persistCollection(cdRomVOS);
    }

    private void addCdRomForLinuxAndOthersVm(String vmUuid) {
        String acntUuid = Account.getAccountUuidOfResource(vmUuid);

        Map<Integer, String> vmDeviceIdIsoMap = getVmIsoMap(vmUuid);
        if (vmDeviceIdIsoMap.isEmpty()) {
            VmCdRomVO cdRomVO = new VmCdRomVO();
            cdRomVO.setUuid(Platform.getUuid());
            cdRomVO.setDeviceId(0);
            cdRomVO.setVmInstanceUuid(vmUuid);
            cdRomVO.setName(String.format("vm-%s-cdRom", vmUuid));
            cdRomVO.setAccountUuid(acntUuid);
            dbf.persist(cdRomVO);
            return;
        }

        List<VmCdRomVO> cdRomVOS = new ArrayList<>();

        vmDeviceIdIsoMap.forEach((deviceId, isoUuid) -> {
            VmCdRomVO cdRomVO = new VmCdRomVO();
            cdRomVO.setUuid(Platform.getUuid());
            cdRomVO.setDeviceId(deviceId);
            cdRomVO.setIsoUuid(isoUuid);
            cdRomVO.setOccupant(isoUuid == null ? null : VmInstanceConstant.VM_CDROM_OCCUPANT_ISO);
            cdRomVO.setVmInstanceUuid(vmUuid);
            cdRomVO.setName(String.format("vm-%s-cdRom", vmUuid));
            cdRomVO.setAccountUuid(acntUuid);
            cdRomVOS.add(cdRomVO);
        });

        dbf.persistCollection(cdRomVOS);
    }

    private Map<Integer, String> getVmIsoMap(String vmUuid) {
        Map<Integer, String> vmDeviceIdIsoMap = new HashMap<>();

        if (!VmSystemTags.ISO.hasTag(vmUuid)) {
            return vmDeviceIdIsoMap;
        }

        List<Map<String, String>> tokenList = VmSystemTags.ISO.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            String isoUuid = tokens.get(VmSystemTags.ISO_TOKEN);
            String isoDeviceId = tokens.get(VmSystemTags.ISO_DEVICEID_TOKEN);

            boolean imageExists = Q.New(ImageVO.class).eq(ImageVO_.uuid, isoUuid).isExists();
            isoUuid = imageExists ? isoUuid : null;
            vmDeviceIdIsoMap.put(Integer.parseInt(isoDeviceId), isoUuid);
        }

        return vmDeviceIdIsoMap;
    }

    private List<String> getTargetVmUuids() {
        List<String> result = new ArrayList<>();

        List<String> allVmUuids = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid)
                .eq(VmInstanceVO_.type, VmInstanceConstant.USER_VM_TYPE)
                .eq(VmInstanceVO_.hypervisorType, VmInstanceConstant.KVM_HYPERVISOR_TYPE)
                .listValues();

        for (String vmUuid : allVmUuids) {
            if (destinationMaker.isManagedByUs(vmUuid)) {
                result.add(vmUuid);
            }
        }

        if (result.isEmpty()) {
            return result;
        }

        List<String> hasCdRomVmUuids = Q.New(VmCdRomVO.class)
                .select(VmCdRomVO_.vmInstanceUuid)
                .in(VmCdRomVO_.vmInstanceUuid, result)
                .listValues();

        result.removeAll(hasCdRomVmUuids);
        return result;
    }
}
