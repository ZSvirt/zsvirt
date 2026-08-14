package org.zstack.externalbackup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.GetHostAccessibleVolumeMsg;
import org.zstack.header.storage.primary.GetHostAccessibleVolumeReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/11/29.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GetResourcesToBackupFlow extends NoRollbackFlow {
    @Autowired
    private CloudBus bus;

    @Override
    public boolean skip(Map data) {
        ExternalBackupSpec spec = (ExternalBackupSpec) data.get(ExternalBackupConstants.EXTERNAL_BACKUP_SPEC);
        return spec.getAllVmUuids() != null;
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        ExternalBackupSpec spec = (ExternalBackupSpec) data.get(ExternalBackupConstants.EXTERNAL_BACKUP_SPEC);

        ErrorCodeList errs = new ErrorCodeList();
        Set<String> recordedVolumeUuids = new HashSet<>();
        Set<String> recordedVmUuids = getAndSetRunningVmToBackup(spec);
        new While<>(buildMsg(spec.getAllHostUuids())).each((msg, compl) -> bus.send(msg, new CloudBusCallBack(compl) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    // TODO: support fcsan ps.
                    if (reply.getError().isError(SysErrors.UNKNOWN_MESSAGE_ERROR)) {
                        compl.done();
                        return;
                    }

                    errs.getCauses().add(reply.getError());
                    compl.allDone();
                    return;
                }

                handleReply(reply.castReply());
                compl.done();
            }

            private void handleReply(GetHostAccessibleVolumeReply reply) {
                if (reply.getVolumeUuids().isEmpty()) {
                    return;
                }

                List<Tuple> ts = Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid, VolumeVO_.uuid)
                        .in(VolumeVO_.uuid, reply.getVolumeUuids())
                        .listTuple();

                for (Tuple t : ts) {
                    String attachedVmUuid = t.get(0, String.class);
                    String volUuid = t.get(1, String.class);
                    if (attachedVmUuid == null && !recordedVolumeUuids.contains(volUuid)) {
                        recordedVolumeUuids.add(volUuid);
                        spec.getAllVolumeUuids().computeIfAbsent(msg.getHostUuid(), k -> new HashSet<>()).add(volUuid);
                    } else if (attachedVmUuid != null && !recordedVmUuids.contains(attachedVmUuid)) {
                        // TODO: check if all volumes of vm can be accessed.
                        recordedVmUuids.add(attachedVmUuid);
                        spec.getAllVmUuids().computeIfAbsent(msg.getHostUuid(), k -> new HashSet<>()).add(attachedVmUuid);
                    }
                }
            }
        })).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errs.getCauses().isEmpty()) {
                    trigger.fail(errs.getCauses().get(0));
                    return;
                }

                spec.getAllVmUuids().forEach((key, value) -> spec.getRestVmUuids().put(key, new HashSet<>(value)));
                spec.getAllVolumeUuids().forEach((key, value) -> spec.getRestVolumeUuids().put(key, new HashSet<>(value)));
                trigger.next();
            }
        });
    }

    @Transactional(readOnly = true)
    protected List<GetHostAccessibleVolumeMsg> buildMsg(List<String> hostUuids) {
        List<GetHostAccessibleVolumeMsg> msgs = new ArrayList<>();
        for (String hostUuid : hostUuids) {
            List<String> psUuids = SQL.New("select ref.primaryStorageUuid from PrimaryStorageClusterRefVO ref, HostVO host" +
                    " where host.uuid = :hostUuid" +
                    " and host.clusterUuid = ref.clusterUuid", String.class)
                    .param("hostUuid", hostUuid)
                    .list();
            for (String psUuid : psUuids) {
                GetHostAccessibleVolumeMsg msg = new GetHostAccessibleVolumeMsg();
                msg.setHostUuid(hostUuid);
                msg.setPrimaryStorageUuid(psUuid);
                bus.makeLocalServiceId(msg, PrimaryStorageConstant.SERVICE_ID);
                msgs.add(msg);
            }
        }
        return msgs;
    }

    private Set<String> getAndSetRunningVmToBackup(ExternalBackupSpec spec) {
        List<Tuple> ts = Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid, VmInstanceVO_.hostUuid)
                .in(VmInstanceVO_.hostUuid, spec.getAllHostUuids())
                // FT VM group do not has root volume uuid.
                .notNull(VmInstanceVO_.rootVolumeUuid)
                .listTuple();

        spec.setAllVmUuids(ts.stream().collect(Collectors.groupingBy(t -> ((Tuple)t).get(1, String.class),
                Collectors.mapping(t -> ((Tuple)t).get(0, String.class), Collectors.toSet())
        )));

        return ts.stream().map(it -> it.get(0, String.class)).collect(Collectors.toSet());
    }
}
