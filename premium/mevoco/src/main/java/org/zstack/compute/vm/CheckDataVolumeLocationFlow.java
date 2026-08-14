package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.GetPrimaryStorageResourceLocationMsg;
import org.zstack.header.storage.primary.GetPrimaryStorageResourceLocationReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.snapshot.VolumeSnapshotAO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotGroupMsg;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2021/5/14.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CheckDataVolumeLocationFlow extends NoRollbackFlow {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public boolean skip(Map data) {
        return ((Collection) data.get(PremiumVmInstanceConstant.DATA_VOLUME_SNAPSHOT)).isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run(FlowTrigger trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);

        List<String> dataVolumeUuids = ((Collection<VolumeSnapshotVO>) data.get(PremiumVmInstanceConstant.DATA_VOLUME_SNAPSHOT))
                .stream().map(VolumeSnapshotAO::getVolumeUuid)
                .collect(Collectors.toList());

        List<String> psUuids = ((Collection<VolumeSnapshotVO>) data.get(PremiumVmInstanceConstant.DATA_VOLUME_SNAPSHOT))
                .stream().map(VolumeSnapshotAO::getPrimaryStorageUuid)
                .collect(Collectors.toList());

        getDataVolumeAccessibleHostUuids(dataVolumeUuids, new ReturnValueCompletion<Set<String>>(trigger) {
            @Override
            public void success(Set<String> dataVolAccessibleHostUuids) {
                spec.setDataVolumeRequiredHostUuids(dataVolAccessibleHostUuids);
                spec.setDataVolumeRequiredPrimaryStorageUuids(new HashSet<>(psUuids));
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    private void getDataVolumeAccessibleHostUuids(List<String> dataVolumeUuids, ReturnValueCompletion<Set<String>> completion) {
        Set<String> dataVolAccessibleHostUuids = new HashSet<>();
        new While<>(dataVolumeUuids).each((dataVolUuid, compl) -> {
            String psUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, dataVolUuid).select(VolumeVO_.primaryStorageUuid).findValue();

            GetPrimaryStorageResourceLocationMsg gmsg = new GetPrimaryStorageResourceLocationMsg();
            gmsg.setResourceUuid(dataVolUuid);
            gmsg.setResourceType(VolumeVO.class.getSimpleName());
            gmsg.setPrimaryStorageUuid(psUuid);
            bus.makeLocalServiceId(gmsg, PrimaryStorageConstant.SERVICE_ID);
            bus.send(gmsg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        compl.allDone();
                        compl.addError(reply.getError());
                        return;
                    }

                    GetPrimaryStorageResourceLocationReply r = reply.castReply();
                    if (r.getHostUuids() == null || r.getHostUuids().isEmpty()) {
                        compl.done();
                        return;
                    }

                    if (dataVolAccessibleHostUuids.isEmpty()) {
                        dataVolAccessibleHostUuids.addAll(r.getHostUuids());
                    } else {
                        dataVolAccessibleHostUuids.retainAll(r.getHostUuids());
                    }
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }

                completion.success(dataVolAccessibleHostUuids);
            }
        });
    }
}
