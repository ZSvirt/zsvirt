package org.zstack.kvm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.ArchiveVmBundle;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.devices.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

public class VirtualPciDeviceKvmExtensionPoint implements KVMStartVmExtensionPoint, KVMSyncVmDeviceInfoExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VirtualPciDeviceKvmExtensionPoint.class);

    @Autowired
    private VmInstanceResourceMetadataManager vidManager;

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (cmd.getRootVolume() != null) {
            setDeviceAddress(cmd.getRootVolume(), cmd);
        }

        if (cmd.getDataVolumes() != null) {
            cmd.getDataVolumes().forEach(to -> setDeviceAddress(to, cmd));
        }

        if (cmd.getNics() != null) {
            cmd.getNics().forEach(to -> setDeviceAddress(to, cmd));
        }

        if (cmd.getCdRoms() != null) {
            cmd.getCdRoms().forEach(to -> setDeviceAddress(to, cmd));
        }

        cmd.setVmXml(getVmXml(spec.getVmInventory().getUuid()));
    }

    private void setDeviceAddress(BaseVirtualDeviceTO to, KVMAgentCommands.StartVmCmd cmd) {
        to.setDeviceAddress(vidManager.getVmDeviceAddress(to.getResourceUuid(), cmd.getVmInstanceUuid()));
    }

    private String getVmXml(String vmUuid) {
        VmInstanceResourceMetadataVO vo = Q.New(VmInstanceResourceMetadataVO.class)
                .eq(VmInstanceResourceMetadataVO_.vmInstanceUuid, vmUuid)
                .eq(VmInstanceResourceMetadataVO_.resourceUuid, vmUuid)
                .find();
        if (vo == null) {
            logger.debug(String.format("VmInstanceResourceMetadataVO is not found for vm[%s]", vmUuid));
            return null;
        }

        if (vo.getMetadata() == null) {
            logger.debug(String.format("metadata is not found for vm[%s] VmInstanceResourceMetadataVO", vmUuid));
            return null;
        }

        ArchiveVmBundle archiveVmBundle;
        try {
            archiveVmBundle = JSONObjectUtil.toObject(vo.getMetadata(), ArchiveVmBundle.class);
            if (archiveVmBundle.getXml() == null) {
                logger.debug(String.format("vm xml is not found for in metadata[%s]", vo.getMetadata()));
                return null;
            }
        } catch (Exception e) {
            logger.warn(String.format("failed to deserialize vm[%s] metadata", vmUuid), e);
            return null;
        }
        return archiveVmBundle.getXml();
    }

    @Override
    public void afterReceiveVmDeviceInfoResponse(VmInstanceInventory vm, KVMAgentCommands.VmDevicesInfoResponse rsp, VmInstanceSpec spec) {
        String vmUuid = spec != null ? spec.getVmInventory().getUuid() : vm.getUuid();

        vidManager.saveVmXmlMetadata(rsp.getVmXml(), vmUuid);

        // only update pci address, metadata is not mandatory in normal usage
        // check its usage when create snapshot or backup
        if (rsp.getVirtualDeviceInfoList() != null) {
            rsp.getVirtualDeviceInfoList().forEach(info -> {
                if (info.isValid()) {
                    vidManager.createOrUpdateVmResourceMetadata(info, vmUuid);
                }
            });
        }

        if (rsp.getNicInfos() == null) {
            return;
        }

        rsp.getNicInfos().forEach(info -> {
            VmNicInventory nic = (spec != null ? spec.getDestNics() : vm.getVmNics())
                    .stream()
                    .filter(vmNicInventory -> vmNicInventory.getMac().equals(info.getMacAddress()))
                    .findFirst()
                    .orElse(null);
            if (nic == null) {
                return;
            }

            vidManager.createOrUpdateVmResourceMetadata(new VirtualDeviceInfo(nic.getUuid(), info.getDeviceAddress()), vmUuid);
        });

        if (!StringUtils.isEmpty(rsp.getMemBalloonInfo().getDeviceAddress().toString())) {
            vidManager.createOrUpdateVmResourceMetadata(new VirtualDeviceInfo(vidManager.MEM_BALLOON_UUID,
                    DeviceAddress.fromString(rsp.getMemBalloonInfo().getDeviceAddress().toString())), vmUuid);
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }
}
