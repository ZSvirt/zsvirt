package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.identity.AccountManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionUtils.filter;
import static org.zstack.utils.CollectionUtils.isEmpty;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateVolumeFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private EventFacade evtf;

    @Override
    public String name() {
        return "create-volumes";
    }

    protected List<CreateVolumeMsg> prepareMsg(Map<String, Object> ctx) {
        VmInstanceSpec spec = (VmInstanceSpec) ctx.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(spec.getVmInventory().getUuid());
        if (accountUuid == null) {
            throw new CloudRuntimeException(String.format("accountUuid for vm[uuid:%s] is null", spec.getVmInventory().getUuid()));
        }

        List<DiskAO> nonTemplateDeprecatedDisks = spec.getNonTemplateDeprecatedDisksSpecs();
        if (!CollectionUtils.isEmpty(nonTemplateDeprecatedDisks)) {
            List<VolumeSpec> dataVolumeSpecs = filter(spec.getVolumeSpecs(), s -> s.getType().equals(VolumeType.Data.toString()));
            int minLen = Math.min(nonTemplateDeprecatedDisks.size(), dataVolumeSpecs.size());

            for (int i = 0; i < minLen; i++) {
                DiskAO disk = nonTemplateDeprecatedDisks.get(i);
                if (!CollectionUtils.isEmpty(disk.getSystemTags())) {
                    dataVolumeSpecs.get(i).setTags(disk.getSystemTags());
                }
            }
        }

        List<VolumeSpec> volumeSpecs = spec.getVolumeSpecs();
        List<CreateVolumeMsg> msgs = new ArrayList<>(volumeSpecs.size());
        int dataVolumeIndex = 0;

        for (VolumeSpec vspec : volumeSpecs) {
            CreateVolumeMsg msg = new CreateVolumeMsg();
            Set<String> tags = new HashSet<>();
            if (vspec == null) {
                continue;
            }

            if (vspec.getTags() != null) {
                tags.addAll(vspec.getTags());
            }

            DebugUtils.Assert(vspec.getType() != null, "VolumeType can not be null!");

            if (vspec.isRoot()) {
                DiskAO disk = spec.getRootDisk();
                msg.setResourceUuid((String) ctx.get("uuid"));

                String name = disk == null ? null : disk.getName();
                msg.setName(name == null ? "ROOT-for-" + spec.getVmInventory().getName() : name);

                msg.setDescription(String.format("Root volume for VM[uuid:%s]", spec.getVmInventory().getUuid()));
                if (spec.getImageSpec().relayOnImage()) {
                    msg.setRootImageUuid(spec.getImageSpec().getInventory().getUuid());
                } else if (disk != null && disk.getTemplateUuid() != null) {
                    msg.setRootImageUuid(disk.getTemplateUuid());
                }

                msg.setFormat(spec.getVolumeFormatFromImage());

                if (spec.getRootVolumeSystemTags() != null) {
                    tags.addAll(spec.getRootVolumeSystemTags());
                }

                if (disk != null && !isEmpty(disk.getSystemTags())) {
                    tags.addAll(disk.getSystemTags());
                }
                Boolean volEnc = disk != null && Boolean.TRUE.equals(disk.getEncrypted());
                msg.setEncrypted(volEnc);
            } else if (vspec.isData()) {
                DiskAO disk = isEmpty(spec.getDataDisks()) ? null :
                        spec.getDataDisks().size() > dataVolumeIndex ? spec.getDataDisks().get(dataVolumeIndex) : null;
                DiskAO deprecatedDisk = isEmpty(nonTemplateDeprecatedDisks) ? null :
                        nonTemplateDeprecatedDisks.size() > dataVolumeIndex ? nonTemplateDeprecatedDisks.get(dataVolumeIndex) : null;

                String name = disk == null ? null : disk.getName();
                msg.setName(name == null ? "DATA-for-" + spec.getVmInventory().getName() : name);

                msg.setDescription(String.format("DataVolume-%s", spec.getVmInventory().getUuid()));
                msg.setFormat(VolumeFormat.getVolumeFormatByMasterHypervisorType(spec.getDestHost().getHypervisorType()).toString());

                if (spec.getDataVolumeSystemTags() != null) {
                    tags.addAll(spec.getDataVolumeSystemTags());
                }
                if (deprecatedDisk != null && !isEmpty(deprecatedDisk.getSystemTags())) {
                    tags.addAll(deprecatedDisk.getSystemTags());
                }
                if (disk != null && !isEmpty(disk.getSystemTags())) {
                    tags.addAll(disk.getSystemTags());
                }
                msg.setEncrypted(disk != null && Boolean.TRUE.equals(disk.getEncrypted()));

                dataVolumeIndex++;
            } else {
                continue;
            }

            msg.setSystemTags(new ArrayList<>(tags));
            msg.setDiskOfferingUuid(vspec.getDiskOfferingUuid());
            msg.setSize(vspec.getSize());
            msg.setPrimaryStorageUuid(vspec.getPrimaryStorageInventory().getUuid());
            msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
            msg.setVolumeType(vspec.getType());
            msg.setAccountUuid(accountUuid);

            bus.makeLocalServiceId(msg, VolumeConstant.SERVICE_ID);
            msgs.add(msg);
        }

        return msgs;
    }

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<CreateVolumeMsg> msgs = prepareMsg(data);
        bus.send(msgs, 1, new CloudBusListCallBack(trigger) {
            @Override
            public void run(List<MessageReply> replies) {
                ErrorCode err = null;
                for (MessageReply r : replies) {
                    VolumeSpec vspec = spec.getVolumeSpecs().get(replies.indexOf(r));

                    if (r.isSuccess()) {
                        CreateVolumeReply cr = r.castReply();
                        VolumeInventory inv = cr.getInventory();
                        if (inv.getType().equals(VolumeType.Root.toString())) {
                            spec.setDestRootVolume(inv);
                            UpdateQuery.New(VmInstanceVO.class)
                                    .isNull(VmInstanceVO_.rootVolumeUuid)
                                    .set(VmInstanceVO_.rootVolumeUuid, inv.getUuid())
                                    .condAnd(VmInstanceVO_.uuid, Op.EQ, spec.getVmInventory().getUuid())
                                    .update();
                        } else {
                            spec.getDestDataVolumes().add(inv);
                        }

                        vspec.setAssociatedVolumeUuid(inv.getUuid());
                        vspec.setIsVolumeCreated(true);
                    } else {
                        err = r.getError();
                        vspec.setIsVolumeCreated(false);
                    }
                }

                if (err != null) {
                    trigger.fail(err);
                } else {
                    trigger.next();
                }
            }
        });
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<VolumeInventory> destVolumes = new ArrayList<>(spec.getDestDataVolumes().size() + 1);
        if (spec.getDestRootVolume() != null) {
            destVolumes.add(spec.getDestRootVolume());
        }
        destVolumes.addAll(spec.getDestDataVolumes());

        final List<DeleteVolumeMsg> msgs = transformAndRemoveNull(destVolumes, arg -> {
            DeleteVolumeMsg msg = new DeleteVolumeMsg();
            msg.setDeletionPolicy(VolumeDeletionPolicy.Direct.toString());
            msg.setUuid(arg.getUuid());
            // don't do detach; because the VM is in state of Starting, it cannot do a detach operation.
            msg.setDetachBeforeDeleting(false);
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, arg.getUuid());
            return msg;
        });

        if (msgs.isEmpty()) {
            chain.rollback();
            return;
        }

        new While<>(msgs).each((msg, compl) -> {
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        compl.done();
                        return;
                    }

                    DeleteVolumeGC gc = new DeleteVolumeGC();
                    gc.NAME = String.format("gc-volume-%s", msg.getVolumeUuid());
                    gc.deletionPolicy = VolumeDeletionPolicy.Direct.toString();
                    gc.volumeUuid = msg.getVolumeUuid();
                    gc.submit(TimeUnit.HOURS.toSeconds(8), TimeUnit.SECONDS);

                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(chain) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                // if ChangeImage, resume rootVolumeUuid
                if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.ChangeImage) {
                    VmInstanceVO vm = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
                    vm.setRootVolumeUuid(spec.getVmInventory().getRootVolumeUuid());
                    dbf.update(vm);
                }

                chain.rollback();
            }
        });
    }
}
