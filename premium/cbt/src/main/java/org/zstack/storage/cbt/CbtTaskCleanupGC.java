package org.zstack.storage.cbt;

import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.cbt.CbtTaskResourceRefVO;
import org.zstack.header.cbt.CbtTaskResourceRefVO_;
import org.zstack.header.cbt.CbtTaskStatus;
import org.zstack.header.cbt.CbtTaskVO;
import org.zstack.header.cbt.CbtTaskVO_;
import org.zstack.header.cbt.VolumeCbtBackupRecordVO;
import org.zstack.header.cbt.VolumeCbtBackupRecordVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.DeleteVolumeMsg;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeDeletionPolicyManager;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.zstack.core.Platform.operr;

public class CbtTaskCleanupGC extends TimeBasedGarbageCollector {
    @GC
    public String hostUuid;
    @GC
    public String vmUuid;
    @GC
    public String taskUuid;
    @GC
    public List<VolumeCbtBackupRecordVO> records;
    @GC
    public List<String> cbtVolumeUuids;
    @GC
    public boolean rollback;

    public void prepare(String taskUuid, String vmUuid) {
        this.taskUuid = taskUuid;
        this.vmUuid = vmUuid;
        this.hostUuid = findVmHostUuid(vmUuid);
        this.rollback = CbtTaskStatus.Failed == Q.New(CbtTaskVO.class)
                .eq(CbtTaskVO_.uuid, taskUuid).select(CbtTaskVO_.status).findValue();
        this.records = Q.New(VolumeCbtBackupRecordVO.class)
                .eq(VolumeCbtBackupRecordVO_.taskUuid, taskUuid).list();
        this.cbtVolumeUuids = Q.New(CbtTaskResourceRefVO.class)
                .eq(CbtTaskResourceRefVO_.taskUuid, taskUuid)
                .eq(CbtTaskResourceRefVO_.resourceType, VolumeVO.class.getSimpleName())
                .select(CbtTaskResourceRefVO_.resourceUuid)
                .listValues();
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!lock()) {
            return;
        }

        HostStatus status = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).select(HostVO_.status).findValue();
        if (status == null) {
            deleteCbtVolumes(completion);
            return;
        }
        if (status != HostStatus.Connected) {
            completion.fail(operr("the host[uuid:%s] is not connected", hostUuid));
            return;
        }
        List<VolumeCbtBackupRecordVO> cleanupRecords = records == null ? Collections.emptyList() : records;
        if (rollback) {
            rollbackAttempts(CbtCleanupRecordPolicy.rollbackAttempts(cleanupRecords).iterator(), completion);
            return;
        }

        cleanupRecords = CbtCleanupRecordPolicy.stopRecords(cleanupRecords);
        if (cleanupRecords.isEmpty()) {
            deleteCbtVolumes(completion);
            return;
        }

        sendCleanupRequest(cleanupRecords, CbtBackupKvmCommands.REST_API_STOP_VOLUME_CBT_BACKUP_PATH,
                new Completion(completion) {
                    @Override
                    public void success() {
                        deleteCbtVolumes(completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private void rollbackAttempts(Iterator<List<VolumeCbtBackupRecordVO>> attempts, GCCompletion completion) {
        if (!attempts.hasNext()) {
            deleteCbtVolumes(completion);
            return;
        }

        List<VolumeCbtBackupRecordVO> attempt = attempts.next();
        sendCleanupRequest(attempt, CbtBackupKvmCommands.REST_API_ROLLBACK_VOLUME_CBT_BACKUP_PATH,
                new Completion(completion) {
                    @Override
                    public void success() {
                        dbf.removeCollection(attempt, VolumeCbtBackupRecordVO.class);
                        records = new ArrayList<>(records);
                        records.removeAll(attempt);
                        updateContext();
                        rollbackAttempts(attempts, completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    private void sendCleanupRequest(List<VolumeCbtBackupRecordVO> cleanupRecords, String path,
                                    Completion completion) {
        CbtBackupKvmCommands.DisableVolumeCbtBackupCmd cmd = new CbtBackupKvmCommands.DisableVolumeCbtBackupCmd();
        cmd.vmUuid = vmUuid;
        cmd.records = cleanupRecords;
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(path);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                KVMHostAsyncHttpCallReply r = reply.castReply();
                KVMAgentCommands.AgentResponse rsp = r.toResponse(KVMAgentCommands.AgentResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }
                completion.success();
            }
        });
    }

    private void deleteCbtVolumes(GCCompletion completion) {
        List<String> volumeUuids = cbtVolumeUuids == null || cbtVolumeUuids.isEmpty()
                ? Collections.emptyList()
                : Q.New(VolumeVO.class).in(VolumeVO_.uuid, cbtVolumeUuids)
                .select(VolumeVO_.uuid).listValues();
        if (volumeUuids.isEmpty()) {
            deleteCbtData();
            completion.success();
            return;
        }

        new While<>(volumeUuids).step((uuid, c) -> {
            DeleteVolumeMsg msg = new DeleteVolumeMsg();
            msg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
            msg.setUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, uuid);
            bus.send(msg, new CloudBusCallBack(c) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        c.addError(reply.getError());
                    }
                    c.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }
                deleteCbtData();
                completion.success();
            }
        });
    }

    private void deleteCbtData() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(CbtTaskResourceRefVO.class).eq(CbtTaskResourceRefVO_.taskUuid, taskUuid).hardDelete();
                sql(VolumeCbtBackupRecordVO.class).eq(VolumeCbtBackupRecordVO_.taskUuid, taskUuid).hardDelete();
                sql(CbtTaskVO.class).eq(CbtTaskVO_.uuid, taskUuid).hardDelete();
            }
        }.execute();
    }

    private String findVmHostUuid(String vmUuid) {
        String uuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hostUuid).findValue();
        return uuid != null ? uuid : Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.lastHostUuid).findValue();
    }
}
