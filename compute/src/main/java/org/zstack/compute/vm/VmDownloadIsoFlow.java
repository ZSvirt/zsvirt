package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DownloadIsoToPrimaryStorageMsg;
import org.zstack.header.storage.primary.DownloadIsoToPrimaryStorageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.ImageSpec;
import org.zstack.utils.CollectionUtils;

import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by frank on 10/17/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmDownloadIsoFlow extends NoRollbackFlow {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final ImageInventory iso = (ImageInventory) data.get(VmInstanceConstant.Params.AttachingIsoInventory.toString());
        final VmInstanceSpec.IsoSpec isoSpec = spec.getDestIsoList().stream()
                .filter(s -> s.getImageUuid().equals(iso.getUuid()))
                .findAny()
                .orElse(null);

        final String psUuid = spec.getVmInventory().getRootVolume().getPrimaryStorageUuid();

        final HostInventory host = spec.getDestHost();
        ImageBackupStorageSelector selector = new ImageBackupStorageSelector();
        selector.setImageUuid(iso.getUuid());
        selector.setZoneUuid(host.getZoneUuid());
        final String bsUuid = selector.select();

        if (bsUuid == null) {
            throw new OperationFailureException(operr("cannot find the iso[uuid:%s] in any connected backup storage attached to the zone[uuid:%s]. check below:\n" +
                                    "1. if the backup storage is attached to the zone where the VM[name: %s, uuid:%s] is running\n" +
                                    "2. if the backup storage is in connected status, if not, try reconnecting it",
                            iso.getUuid(), host.getZoneUuid(), spec.getVmInventory().getName(), spec.getVmInventory().getUuid())
            );
        }

        ImageSpec imageSpec = new ImageSpec();
        imageSpec.setSelectedBackupStorage(CollectionUtils.findOneOrNull(iso.getBackupStorageRefs(),
                arg -> arg.getBackupStorageUuid().equals(bsUuid)
                        && ImageStatus.Ready.toString().equals(arg.getStatus())));
        imageSpec.setInventory(iso);

        DownloadIsoToPrimaryStorageMsg msg = new DownloadIsoToPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(psUuid);
        msg.setIsoSpec(imageSpec);
        msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        msg.setDestHostUuid(spec.getDestHost().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }

                if (isoSpec != null) {
                    DownloadIsoToPrimaryStorageReply r = reply.castReply();
                    isoSpec.setInstallPath(r.getInstallPath());
                    isoSpec.setPrimaryStorageUuid(psUuid);
                    isoSpec.setProtocol(r.getProtocol());
                }
                trigger.next();
            }
        });
    }
}
