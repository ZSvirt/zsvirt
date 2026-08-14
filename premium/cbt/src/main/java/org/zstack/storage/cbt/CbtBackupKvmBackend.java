package org.zstack.storage.cbt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.agent.AgentResponse;
import org.zstack.header.cbt.CbtTaskVO;
import org.zstack.header.cbt.VolumeCbtBackupInfo;
import org.zstack.header.cbt.VolumeCbtBackupRecordVO;
import org.zstack.header.cbt.VolumeCbtBackupRecordVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.cbt.CbtBackupKvmCommands.TakeVolumeCbtBackupCmd;
import org.zstack.storage.cbt.CbtBackupKvmCommands.GetVolumesBitmapCmd;
import org.zstack.storage.cbt.CbtBackupKvmCommands.ExportNbdVolumesCmd;
import org.zstack.storage.cbt.CbtBackupKvmCommands.GetVolumesBitmapResponse;
import org.zstack.storage.cbt.CbtBackupKvmCommands.TakeVolumeCbtBackupResponse;
import org.zstack.storage.cbt.CbtBackupKvmCommands.DisableVolumeCbtBackupCmd;
import org.zstack.storage.cbt.CbtBackupKvmCommands.ExportNbdVolumesResponse;
import org.zstack.storage.cbt.CbtBackupKvmCommands.UnexportNbdVolumesCmd;
import org.zstack.storage.cbt.CbtBackupKvmCommands.ListExportedVolumesCmd;
import org.zstack.storage.cbt.CbtBackupKvmCommands.ListExportedVolumesResponse;
import org.zstack.kvm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CbtBackupKvmBackend implements CbtBackupHypervisorBackend {
    private static final CLogger logger = Utils.getLogger(CbtBackupKvmBackend.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    CbtBackupKvmBackend() {
    }

    @Override
    public void takeVolumeCbtBackup(String vmUuid, String hostUuid, List<VolumeCbtBackupInfo> volumeInfos, String bitmapName, ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion) {
        TakeVolumeCbtBackupCmd cmd = new TakeVolumeCbtBackupCmd();
        cmd.volumeInfos = volumeInfos;
        cmd.vmUuid = vmUuid;
        cmd.bitmapTimestamp = bitmapName;
        cmd.portRange = HostGlobalConfig.NBD_PORT_RANGE.value(String.class);
        doCbtBackup(hostUuid, cmd, new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(completion) {
            @Override
            public void success(List<VolumeCbtBackupInfo> infos) {
                completion.success(infos);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void getVolumesBitmap(List<VolumeCbtBackupInfo> volumeInfos, String hostUuid, String bitmapName, ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion) {
        GetVolumesBitmapCmd cmd = new GetVolumesBitmapCmd();
        cmd.volumeInfos = volumeInfos;
        cmd.bitmapTimestamp = bitmapName;
        doGetVolumeBitmaps(hostUuid, cmd, new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(completion) {
            @Override
            public void success(List<VolumeCbtBackupInfo> infos) {
                completion.success(infos);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void exportNbdVolumesForRecovery(String vmUuid, List<String> volumeUuids, ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion) {
        List<VolumeCbtBackupInfo> infos = new ArrayList<VolumeCbtBackupInfo>();
        String hostUuid = getVmHostUuid(vmUuid);
        KVMHostInventory inv = KVMHostInventory.valueOf(dbf.findByUuid(hostUuid, KVMHostVO.class));

        for (String uuid : volumeUuids) {
            VolumeVO vo = getVolumeByUuid(uuid);
            VolumeCbtBackupInfo info = new VolumeCbtBackupInfo();
            info.setVolume(VolumeTO.valueOf(VolumeInventory.valueOf(vo), inv));
            info.setNbdServer(inv.getManagementIp());
            infos.add(info);
        }
        ExportNbdVolumesCmd cmd = new ExportNbdVolumesCmd();
        cmd.volumeInfos = infos;
        cmd.portRange = HostGlobalConfig.NBD_PORT_RANGE.value(String.class);
        doExportNbdVolumesForRecovery(hostUuid, cmd, new ReturnValueCompletion<List<VolumeCbtBackupInfo>>(completion) {
            @Override
            public void success(List<VolumeCbtBackupInfo> infos) {
                completion.success(infos);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void unexportNbdVolumesForRecovery(String vmUuid, List<String> volumeUuids, Completion completion) {
        List<VolumeTO> volumes = new ArrayList<VolumeTO>();
        String hostUuid = getVmHostUuid(vmUuid);
        KVMHostInventory inv = KVMHostInventory.valueOf(dbf.findByUuid(hostUuid, KVMHostVO.class));

        for (String uuid : volumeUuids) {
            VolumeVO vo = getVolumeByUuid(uuid);
            volumes.add(VolumeTO.valueOf(VolumeInventory.valueOf(vo), inv));
        }
        UnexportNbdVolumesCmd cmd = new UnexportNbdVolumesCmd();
        cmd.volumes = volumes;
        doUnexportNbdVolumesForRecovery(hostUuid, cmd, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void listExportedVolumesForRecovery(String vmUuid, List<String> volumeUuids, ReturnValueCompletion<Map<String, Boolean>> completion) {
        List<VolumeTO> volumes = new ArrayList<VolumeTO>();
        String hostUuid = getVmHostUuid(vmUuid);
        KVMHostInventory inv = KVMHostInventory.valueOf(dbf.findByUuid(hostUuid, KVMHostVO.class));

        for (String uuid : volumeUuids) {
            VolumeVO vo = getVolumeByUuid(uuid);
            volumes.add(VolumeTO.valueOf(VolumeInventory.valueOf(vo), inv));
        }
        ListExportedVolumesCmd cmd = new ListExportedVolumesCmd();
        cmd.volumes = volumes;
        doListExportedVolumesForRecovery(hostUuid, cmd, new ReturnValueCompletion<Map<String, Boolean>>(completion)  {
            @Override
            public void success(Map<String, Boolean> infos) {
                completion.success(infos);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void disableVolumeCbtBackup(CbtTaskVO vo, String vmUuid, String hostUuid, Completion completion) {
        List<VolumeCbtBackupRecordVO> records = SQL.New("select vo from VolumeCbtBackupRecordVO vo" +
                " where taskUuid = :taskUuid and id in (select max(id) from VolumeCbtBackupRecordVO group by volumeUuid)")
                .param("taskUuid", vo.getUuid())
                .list();

        DisableVolumeCbtBackupCmd cmd = new DisableVolumeCbtBackupCmd();
        cmd.vmUuid = vmUuid;
        cmd.records = records;
        doDisableCbtBackup(hostUuid, cmd, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void doCbtBackup(String hostUuid, TakeVolumeCbtBackupCmd cmd, ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setCommand(cmd);
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(CbtBackupKvmCommands.REST_API_TAKE_VOLUME_CBT_BACKUP_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                TakeVolumeCbtBackupResponse rsp = r.toResponse(TakeVolumeCbtBackupResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                } else {
                    completion.success(rsp.volumeInfos);
                }
            }
        });
    }

    private void doExportNbdVolumesForRecovery(String hostUuid, ExportNbdVolumesCmd cmd, ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setCommand(cmd);
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(CbtBackupKvmCommands.REST_API_EXPORT_NDB_VOLUMES_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                ExportNbdVolumesResponse rsp = r.toResponse(ExportNbdVolumesResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                } else {
                    completion.success(rsp.volumeInfos);
                }
            }
        });
    }

    private void doUnexportNbdVolumesForRecovery(String hostUuid, UnexportNbdVolumesCmd cmd, Completion completion) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setCommand(cmd);
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(CbtBackupKvmCommands.REST_API_UNEXPORT_NDB_VOLUMES_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
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
                } else {
                    completion.success();
                }
            }
        });
    }

    private void doListExportedVolumesForRecovery(String hostUuid, ListExportedVolumesCmd cmd, ReturnValueCompletion<Map<String, Boolean>> completion) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setCommand(cmd);
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(CbtBackupKvmCommands.REST_API_LIST_EXPORTED_VOLUMES_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                ListExportedVolumesResponse rsp = r.toResponse(ListExportedVolumesResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                } else {
                    completion.success(rsp.volumeExportInfos);
                }
            }
        });
    }

    private void doGetVolumeBitmaps(String hostUuid, GetVolumesBitmapCmd cmd, ReturnValueCompletion<List<VolumeCbtBackupInfo>> completion) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setCommand(cmd);
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(CbtBackupKvmCommands.REST_API_GET_VOLUMES_BITMAPS_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                GetVolumesBitmapResponse rsp = r.toResponse(GetVolumesBitmapResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                } else {
                    completion.success(rsp.volumeInfos);
                }
            }
        });
    }

    private void doDisableCbtBackup(String hostUuid, DisableVolumeCbtBackupCmd cmd, Completion completion) {
        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();

        kmsg.setCommand(cmd);
        kmsg.setHostUuid(hostUuid);
        kmsg.setPath(CbtBackupKvmCommands.REST_API_STOP_VOLUME_CBT_BACKUP_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                AgentResponse rsp = r.toResponse(AgentResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                } else {
                    completion.success();
                }
            }
        });
    }

    private String getVmHostUuid(String vmUuid) {
        String hostUuid = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hostUuid)
                .findValue();
        if (StringUtils.isNotEmpty(hostUuid)) {
            return hostUuid;
        }

        return Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.lastHostUuid)
                .findValue();
    }

    private VolumeVO getVolumeByUuid(String uuid) {
        return Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, uuid)
                .find();
    }
}
