package org.zstack.mttyDevice.KvmMttyDeviceBackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.CpuArchitecture;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.kvm.*;
import org.zstack.mttyDevice.*;
import org.zstack.pciDevice.virtual.vfio_mdev.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * @author yu.sun
 * @date 2022/11/22 18:12
 **/
public class MttyDeviceKvmBackend implements MttyDeviceBackend {
    public static final String GET_MTTY_DEVICES = "/mttydevice/get";
    public static final String GENERATE_SE_VFIO_MDEV_DEVICES = "/semdevdevice/generate";
    private static final String UNGENERATE_SE_VFIO_MDEV_DEVICES = "/semdevdevice/ungenerate";
    private static CLogger logger = Utils.getLogger(MttyDeviceKvmBackend.class);
    
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    protected RESTFacade restf;
    
    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }
    
    @Override
    public void generateSeMdevDevices(String hostUuid, MttyDeviceInventory mttyDevice, List<String> mdevUuids, Boolean reSplite, Completion completion) {
        int partNum = mdevUuids.size();
        if (partNum == 0) {
            completion.fail(operr("The number[value:%s] is not a valid part number.", partNum));
            return;
        }
        
        String mttyDeviceUuid = mttyDevice.getUuid();
        int accu = MttyDeviceGlobalConfig.SE_NUM.value(Integer.class);
        long seNum = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.hostUuid, hostUuid)
                .eq(MdevDeviceVO_.type, MdevDeviceType.SE_Controller)
                .count();
        if (!reSplite && accu < partNum + seNum) {
            completion.fail(operr("The quantity exceeded. The device[uuid: %s] required se devices number exceeds a quantiry[value: %s].", mttyDeviceUuid, accu));
            return;
        }
        GenerateSeVfioMdevDevicesCommand cmd = new GenerateSeVfioMdevDevicesCommand();
        cmd.setMdevUuids(mdevUuids);
        cmd.setMttyDeviceUuid(mttyDeviceUuid);
        cmd.setReSplite(reSplite);
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(GENERATE_SE_VFIO_MDEV_DEVICES);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                
                KVMHostAsyncHttpCallReply rly = reply.castReply();
                GenerateSeVfioMdevDevicesRsp rsp = rly.toResponse(GenerateSeVfioMdevDevicesRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("failed to generate se devices, because:%s", rsp.getError()));
                    return;
                }
                
                // if re-split after host reboot, then returns no mdev uuids
                List<String> mdevUuids = rsp.getMdevUuids();
                if (mdevUuids == null || mdevUuids.isEmpty() || reSplite) {
                    completion.success();
                    return;
                }
                
                // create mdev devices in database
                for (String mdevUuid : mdevUuids) {
                    MdevDeviceVO mdev = new MdevDeviceVO();
                    // mdev uuid return from kvmagent is like `7ce5b19c-e8cd-47c3-9bab-6661ec3539d0`
                    mdev.setUuid(mdevUuid.replaceAll("-", ""));
                    mdev.setName("SE" + '_' + mdev.getUuid().substring(0, 8));
                    mdev.setDescription(String.format("mdev device generated from mtty device[uuid:%s]", mttyDevice.getUuid()));
                    mdev.setHostUuid(hostUuid);
                    mdev.setMttyUuid(mttyDeviceUuid);
                    mdev.setType(MdevDeviceType.valueOf(mttyDevice.getType().toString()));
                    mdev.setState(MdevDeviceState.Enabled);
                    mdev.setStatus(MdevDeviceStatus.Active);
                    mdev.setChooser(MdevDeviceChooser.None);
                    mdev.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    dbf.persist(mdev);
                    logger.debug("created mdev device: " + mdev.getName());
                }
                
                // update mtty device virt status
                SQL.New(MttyDeviceVO.class)
                        .eq(MttyDeviceVO_.uuid, mttyDevice.getUuid())
                        .set(MttyDeviceVO_.virtStatus, MttyDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED)
                        .update();
                completion.success();
            }
        });
    }
    
    @Override
    public void ungenerateSeMdevDevices(String hostUuid, MttyDeviceInventory mttyDevice, List<String> mdevUuids, Completion completion) {
        MttyDeviceKvmBackend.UngenerateSeVfioMdevDevicesCommand cmd = new MttyDeviceKvmBackend.UngenerateSeVfioMdevDevicesCommand();
        cmd.setMttyDeviceUuid(mttyDevice.getUuid());
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(UNGENERATE_SE_VFIO_MDEV_DEVICES);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }
                
                KVMHostAsyncHttpCallReply rly = reply.castReply();
                UngenerateSeVfioMdevDevicesRsp rsp = rly.toResponse(UngenerateSeVfioMdevDevicesRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("failed to ungenerate se devices, because:%s", rsp.getError()));
                    return;
                }
                
                // delete mdev devices in database
                SQL.New(MdevDeviceVO.class)
                        .eq(MdevDeviceVO_.mttyUuid, mttyDevice.getUuid())
                        .hardDelete();
                
                // update mtty device virt status
                SQL.New(MttyDeviceVO.class)
                        .eq(MttyDeviceVO_.uuid, mttyDevice.getUuid())
                        .set(MttyDeviceVO_.virtStatus, MttyDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE)
                        .update();
                
                completion.success();
            }
        });
    }
    
    @Override
    public void syncMttyDeviceFromHost(String hostUuid, Completion completion) {
        GetMttyDevicesCmd cmd = new GetMttyDevicesCmd();
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(GET_MTTY_DEVICES);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    fail();
                    return;
                }
                KVMHostAsyncHttpCallReply r = reply.castReply();
                GetMttyDevicesRsp rsp = r.toResponse(MttyDeviceKvmBackend.GetMttyDevicesRsp.class);
                if (!rsp.isSuccess()) {
                    fail();
                    return;
                }
                
                MttyDeviceTO to = rsp.mttyDeviceInfo;
                if (to == null) {
                    logger.debug("no mtty device info received, which is unlikely");
                    completion.success();
                    return;
                }
                // sync info of mtty devices in host
                syncMttyInDb(hostUuid, to);
                completion.success();
            }
            
            private void fail() {
                completion.fail(operr(String.format("get mtty device info from host[uuid:%s] failed", hostUuid)));
            }
        });
    }
    
    
    public static class GenerateSeVfioMdevDevicesCommand extends KVMAgentCommands.AgentCommand {
        private List<String> mdevUuids;
        
        private String mttyDeviceUuid;
        
        private Boolean reSplite;
        
        public List<String> getMdevUuids() {
            return mdevUuids;
        }
        
        public String getMttyDeviceUuid() {
            return mttyDeviceUuid;
        }
        
        public void setMttyDeviceUuid(String mttyDeviceUuid) {
            this.mttyDeviceUuid = mttyDeviceUuid;
        }
        
        public void setMdevUuids(List<String> mdevUuids) {
            this.mdevUuids = mdevUuids;
        }
        
        public Boolean getReSplite() {
            return reSplite;
        }
        
        public void setReSplite(Boolean reSplite) {
            this.reSplite = reSplite;
        }
    }
    
    public static class GenerateSeVfioMdevDevicesRsp extends KVMAgentCommands.AgentResponse {
        private List<String> mdevUuids;
        
        public List<String> getMdevUuids() {
            return mdevUuids;
        }
        
        public void setMdevUuids(List<String> mdevUuids) {
            this.mdevUuids = mdevUuids;
        }
    }
    
    public static class UngenerateSeVfioMdevDevicesCommand extends KVMAgentCommands.AgentCommand {
        private String mttyDeviceUuid;
        
        private String getMttyDeviceUuid() {
            return mttyDeviceUuid;
        }
        
        public void setMttyDeviceUuid(String mttyDeviceUuid) {
            this.mttyDeviceUuid = mttyDeviceUuid;
        }
    }
    
    public static class UngenerateSeVfioMdevDevicesRsp extends KVMAgentCommands.AgentResponse {
    
    }
    
    public static class GetMttyDevicesCmd extends KVMAgentCommands.AgentCommand {
    }
    
    public static class GetMttyDevicesRsp extends KVMAgentCommands.AgentResponse {
        private MttyDeviceTO mttyDeviceInfo;
        
        public MttyDeviceTO getMttyDeviceInfo() {
            return mttyDeviceInfo;
        }
        
        public void setMttyDeviceInfo(MttyDeviceTO mttyDeviceInfo) {
            this.mttyDeviceInfo = mttyDeviceInfo;
        }
    }
    
    private synchronized void syncMttyInDb(String hostUuid, MttyDeviceTO to) {
        MttyDeviceVO mtty;
        String deviceUuid;
        
        List<String> mttyUuids = Q.New(MttyDeviceVO.class)
                .select(MttyDeviceVO_.uuid)
                .eq(MttyDeviceVO_.type, MttyDeviceType.SE_Controller)
                .eq(MttyDeviceVO_.hostUuid, hostUuid)
                .listValues();
        int mttyNum = mttyUuids.size();
        if (mttyNum == 1) {
            // update old mtty in host
            mtty = dbf.findByUuid(mttyUuids.get(0), MttyDeviceVO.class);
        } else {
            // create new mtty in host
            deviceUuid = Platform.getUuid();
            mtty = new MttyDeviceVO();
            mtty.setUuid(deviceUuid);
            mtty.setHostUuid(hostUuid);
        }
        
        // e.g. S7150V_2048M_72fdddb8 or GTX-1080Ti_5fd418ee
        mtty.setName(to.getName() + '_' + mtty.getUuid().substring(0, 8));
        // e.g. Advanced Micro Devices, Inc. [AMD/ATI], Tonga XTV GL [FirePro S7150V], VGA compatible controller
        mtty.setDescription(to.getDescription());
        mtty.setState(MttyDeviceState.Enabled);
        mtty.setVirtStatus(MttyDeviceVirtStatus.valueOf(to.getVirtStatus()));
        mtty.setType(MttyDeviceType.valueOf(to.getType()));
        mtty.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        logger.debug(String.format("create new mtty device[uuid:%s, name:%s]", mtty.getUuid(), mtty.getName()));
        if (mttyNum == 1) {
            dbf.update(mtty);
        } else {
            dbf.persist(mtty);
        }
    }
}