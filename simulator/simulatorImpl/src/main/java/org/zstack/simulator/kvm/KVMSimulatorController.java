package org.zstack.simulator.kvm;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostNUMANode;
import org.zstack.header.rest.RESTFacade;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMConstant.KvmVmState;
import org.zstack.kvm.KVMSecurityGroupBackend;
import org.zstack.network.securitygroup.SecurityGroupRuleTO;
import org.zstack.simulator.AsyncRESTReplyer;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KVMSimulatorController {
    private static final CLogger logger = Utils.getLogger(KVMSimulatorController.class);
    
    /*
     *  getBean() cannot return the same Controller as servlet use for unittest case,
     *  Controller should be a singleton, however, getBean() returns another copy of it
     *  as if it's prototype
     *  we have to use a config as Controller can inject the same instance that unittest
     *  gets
     */
    @Autowired
    private KVMSimulatorConfig config;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private VolumeSnapshotKvmSimulator snapshotKvmSimulator;

    private AsyncRESTReplyer replyer = new AsyncRESTReplyer();

    @RequestMapping(value=KVMConstant.KVM_HARDEN_CONSOLE_PATH, method=RequestMethod.POST)
    public @ResponseBody String hardenVmConsole(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        HardenVmConsoleCmd cmd = JSONObjectUtil.toObject(entity.getBody(), HardenVmConsoleCmd.class);
        config.hardenVmConsoleCmds.add(cmd);
        replyer.reply(entity, new AgentResponse());
        return null;
    }

    @RequestMapping(value=KVMConstant.KVM_DELETE_CONSOLE_FIREWALL_PATH, method=RequestMethod.POST)
    public @ResponseBody String deleteVmConsole(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        DeleteVmConsoleFirewallCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteVmConsoleFirewallCmd.class);
        config.deleteVmConsoleFirewallCmds.add(cmd);
        replyer.reply(entity, new AgentResponse());
        return null;
    }

    @RequestMapping(value=KVMConstant.KVM_VM_CHECK_STATE, method=RequestMethod.POST)
    public @ResponseBody String checkVmState(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        CheckVmStateCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckVmStateCmd.class);
        CheckVmStateRsp rsp = new CheckVmStateRsp();
        Map<String, String> m = new HashMap<String, String>();
        for (String vmUuid : cmd.vmUuids) {
            Map<String, String> h = config.checkVmStatesConfig.get(cmd.hostUuid);
            if (h != null) {
                m.put(vmUuid, h.get(vmUuid));
            }
        }
        rsp.states = m;
        config.checkVmStateCmds.add(cmd);
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=KVMConstant.KVM_ATTACH_NIC_PATH, method=RequestMethod.POST)
    public @ResponseBody String attachNic(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doAttachNic(entity);
        return null;
    }

    @AsyncThread
    private void doAttachNic(HttpEntity<String> entity) {
        AttachNicCommand cmd = JSONObjectUtil.toObject(entity.getBody(), AttachNicCommand.class);
        AttachNicResponse rsp = new AttachNicResponse();
        if (!config.attachNicSuccess) {
            rsp.setSuccess(false);
            rsp.setError("fail on purpose");
        } else {
            config.attachNicCommands.add(cmd);
            config.attachedNics.put(cmd.getNic().getNicInternalName(), cmd.getNic());
        }

        replyer.reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_ATTACH_ISO_PATH, method=RequestMethod.POST)
    public @ResponseBody String attachIso(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        AttachIsoCmd cmd = JSONObjectUtil.toObject(entity.getBody(), AttachIsoCmd.class);
        config.attachIsoCmds.add(cmd);
        reply(entity, new AttachIsoRsp());
        return null;
    }

    @RequestMapping(value=KVMConstant.KVM_DETACH_ISO_PATH, method=RequestMethod.POST)
    public @ResponseBody String detachIso(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        DetachIsoCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DetachIsoCmd.class);
        config.detachIsoCmds.add(cmd);
        reply(entity, new DetachIsoRsp());
        return null;
    }

    @RequestMapping(value=KVMConstant.KVM_DETACH_NIC_PATH, method=RequestMethod.POST)
    public @ResponseBody String detachNic(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doDetachNic(entity);
        return null;
    }

    private void doDetachNic(HttpEntity<String> entity) {
        DetachNicCommand cmd = JSONObjectUtil.toObject(entity.getBody(), DetachNicCommand.class);

        DetachNicRsp rsp = new DetachNicRsp();
        if (!config.detachNicSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            config.detachNicCommands.add(cmd);
        }

        replyer.reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_MERGE_SNAPSHOT_PATH, method=RequestMethod.POST)
    public @ResponseBody String mergeSnapshot(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        MergeSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), MergeSnapshotCmd.class);
        MergeSnapshotRsp rsp = new MergeSnapshotRsp();
        if (!config.mergeSnapshotSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            snapshotKvmSimulator.merge(cmd.getSrcPath(), cmd.getDestPath(), cmd.isFullRebase());
            config.mergeSnapshotCmds.add(cmd);
            logger.debug(entity.getBody());
        }

        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH, method=RequestMethod.POST)
    public @ResponseBody String takeSnapshot(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        TakeSnapshotCmd cmd = JSONObjectUtil.toObject(entity.getBody(), TakeSnapshotCmd.class);
        TakeSnapshotResponse rsp = new TakeSnapshotResponse();
        if (config.snapshotSuccess) {
            config.snapshotCmds.add(cmd);
            rsp = snapshotKvmSimulator.takeSnapshot(cmd);

            Long size = config.takeSnapshotCmdSize.get(cmd.getVolumeUuid());
            rsp.setSize(size == null ? 1 : size);
        } else  {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value=KVMConstant.KVM_PING_PATH, method=RequestMethod.POST)
    public @ResponseBody String ping(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        ping(entity);
        return null;
    }

    @AsyncThread
    private void ping(HttpEntity<String> entity) {
        PingCmd cmd = JSONObjectUtil.toObject(entity.getBody(), PingCmd.class);
        PingResponse rsp = new PingResponse();
        if (!config.pingSuccess) {
            rsp.setSuccess(false);
            rsp.setError("on purpose");
        }

        Boolean s = config.pingSuccessMap.get(cmd.hostUuid);
        if (s != null && !s) {
            rsp.setSuccess(false);
            rsp.setError("on purpose");
        }

        rsp.setHostUuid(config.connectHostUuids.get(cmd.hostUuid));
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_CONNECT_PATH, method=RequestMethod.POST)
    public @ResponseBody String connect(@RequestBody String body) {
        ConnectCmd cmd = JSONObjectUtil.toObject(body, ConnectCmd.class);
        
        config.connectHostUuids.put(cmd.getHostUuid(), cmd.getHostUuid());
        
        if (config.connectException) {
            throw new CloudRuntimeException("connect exception on purpose");
        }
        
        ConnectResponse rsp = new ConnectResponse();
        
        if (config.connectSuccess) {
            config.connectCmds.add(cmd);
            rsp.setSuccess(true);
            rsp.setLibvirtVersion("1.0.0");
            rsp.setQemuVersion("1.3.0");
            logger.debug("KVM connected");
        } else {
            rsp.setSuccess(false);
            rsp.setError("Fail connect on purpose");
        }
        return JSONObjectUtil.toJsonString(rsp);
    }
    
    @RequestMapping(value=KVMConstant.KVM_ECHO_PATH, method=RequestMethod.POST)
    public @ResponseBody String echo() {
        return "";
    }

    
    @RequestMapping(value=KVMConstant.KVM_DETACH_VOLUME, method=RequestMethod.POST)
    private @ResponseBody String detachDataVolume(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doDetachDataVolume(entity);
        return null;
    }
    
    @RequestMapping(value=KVMConstant.KVM_VM_SYNC_PATH, method=RequestMethod.POST)
    private @ResponseBody String vmSync(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doVmSync(entity);
        return null;
    }
    
    @AsyncThread
    private void doVmSync(HttpEntity<String> entity) {
        synchronized (config) {
            VmSyncResponse rsp = new VmSyncResponse();
            if (!config.vmSyncSuccess) {
                rsp.setSuccess(false);
                rsp.setError("on purpose");
                reply(entity, rsp);
                return;
            }

            HashMap<String, String> vms = new HashMap<String, String>();
            for (Map.Entry<String, KvmVmState> e : config.vms.entrySet()) {
                vms.put(e.getKey(), e.getValue().toString());
            }
            rsp.setStates(vms);
            rsp.setVmInShutdowns(new ArrayList<String>());
            reply(entity, rsp);
        }
    }
    
    @RequestMapping(value=KVMSecurityGroupBackend.SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH, method=RequestMethod.POST)
    private @ResponseBody String refreshSecurityGroupRulesOnHost(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doRefreshSecurityGroupRulesOnHost(entity);
        return null;
    }
    

    private void doRefreshSecurityGroupRulesOnHost(HttpEntity<String> entity) {
        RefreshAllRulesOnHostCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RefreshAllRulesOnHostCmd.class);
        RefreshAllRulesOnHostResponse rsp = new RefreshAllRulesOnHostResponse();
        if (!config.securityGroupSuccess) {
            rsp.setError("fail to apply security group rules on purpose");
            rsp.setSuccess(false);
        } else {
            config.securityGroups.clear();
            config.vmNicSecuritys.addAll(cmd.getVmNicTOs());
            config.securityGroupRefreshAllRulesOnHostCmds.add(cmd);
        }
        reply(entity, rsp);
    }

    @RequestMapping(value=KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH, method=RequestMethod.POST)
    private @ResponseBody String applySecurityGroupRules(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doApplySecurityGroupRules(entity);
        return null;
    }
    
    @AsyncThread
    private void doApplySecurityGroupRules(HttpEntity<String> entity) {
        ApplySecurityGroupRuleCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ApplySecurityGroupRuleCmd.class);
        ApplySecurityGroupRuleResponse rsp = new ApplySecurityGroupRuleResponse();
        if (!config.securityGroupSuccess) {
            rsp.setError("fail to apply security group rules on purpose");
            rsp.setSuccess(false);
        } else {
            config.vmNicSecuritys.addAll(cmd.getVmNicTOs());
            // for (SecurityGroupRuleTO rto : cmd.getRuleTOs()) {
            //     config.securityGroups.put(rto.getVmNicInternalName(), rto);
            //     logger.debug(String.format("succesfully applied security group rules for vm nic[%s], %s", rto.getVmNicInternalName(), rto));
            // }
        }
        reply(entity, rsp);
    }

    @AsyncThread
    private void doDetachDataVolume(HttpEntity<String> entity) {
        DetachDataVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DetachDataVolumeCmd.class);
        DetachDataVolumeResponse rsp = new DetachDataVolumeResponse();
        if (!config.detachVolumeSuccess) {
            rsp.setError("failed to detach data volume on purpose");
            rsp.setSuccess(false);
        } else {
            config.detachDataVolumeCmds.add(cmd);
            logger.debug(String.format("successfully detached data volume: %s", entity.getBody()));
        }
        reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_ATTACH_VOLUME, method=RequestMethod.POST)
    private @ResponseBody String attachDataVolume(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doAttachDataVolume(entity);
        return null;
    }
    
    private void doAttachDataVolume(HttpEntity<String> entity) {
        AttachDataVolumeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), AttachDataVolumeCmd.class);
        if (!config.attachVolumeSuccess) {
            throw new CloudRuntimeException("fail to attach volume on purpose");
        }
        config.attachDataVolumeCmds.add(cmd);
        logger.debug(String.format("successfully attached data volume: %s", entity.getBody()));
        AttachDataVolumeResponse rsp = new AttachDataVolumeResponse();
        reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_HOST_CAPACITY_PATH, method=RequestMethod.POST)
    public @ResponseBody String hostCapacity(@RequestBody String body) {
        if (config.hostFactException) {
            throw new CloudRuntimeException("Host capacity exception on purpose");
        }
        
        HostCapacityResponse rsp = new HostCapacityResponse();
        if (config.hostFactSuccess) {
            rsp.setSuccess(true);
            rsp.setCpuNum(config.cpuNum);
            rsp.setCpuSpeed(config.cpuSpeed);
            rsp.setTotalMemory(config.totalMemory);
            rsp.setUsedCpu(config.usedCpu);
            rsp.setUsedMemory(config.usedMemory);
        } else {
            rsp.setSuccess(false);
            rsp.setError("Fail host capacity on purpose");
        }
        return JSONObjectUtil.toJsonString(rsp);
    }
    
    @RequestMapping(value=KVMConstant.KVM_CHECK_PHYSICAL_NETWORK_INTERFACE_PATH, method=RequestMethod.POST)
    public @ResponseBody String checkPhysicalInterface(@RequestBody String body) {
        if (config.checkPhysicalInterfaceException) {
            throw new CloudRuntimeException("checkPhysicalInterface exception on purpose");
        }
        
        CheckPhysicalNetworkInterfaceCmd cmd = JSONObjectUtil.toObject(body, CheckPhysicalNetworkInterfaceCmd.class);
        CheckPhysicalNetworkInterfaceResponse rsp = new CheckPhysicalNetworkInterfaceResponse();
        if (config.checkPhysicalInterfaceSuccess) {
            rsp.setSuccess(true);
            logger.debug(String.format("Checked physical interfaces: %s", cmd.getInterfaceNames()));
        } else {
            rsp.setFailedInterfaceNames(cmd.getInterfaceNames());
            rsp.setSuccess(false);
            rsp.setError("Fail checkPhysicalInterface on purpose");
        }
        return JSONObjectUtil.toJsonString(rsp);
    }
    
    private void reply(HttpEntity<String> entity, AgentResponse rsp) {
        if (replyer == null) {
            replyer = new AsyncRESTReplyer();
        }
        replyer.reply(entity, rsp);
    }
    
    @AsyncThread
    private void createBridge(HttpEntity<String> entity) {
        CreateBridgeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateBridgeCmd.class);
        CreateBridgeResponse rsp = new CreateBridgeResponse();
        if (config.createL2NoVlanNetworkSuccess) {
            logger.debug(String.format("create bridge[%s] successfully on kvm simulator", cmd.getBridgeName()));
            config.createBridgeCmds.add(cmd);
        } else {
            rsp.setError("Fail createBridge on purpose");
            rsp.setSuccess(false);
        }
        reply(entity, rsp);
    }
    
    @RequestMapping(value=KVMConstant.KVM_REALIZE_L2NOVLAN_NETWORK_PATH, method=RequestMethod.POST)
    private @ResponseBody String createBridge(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        createBridge(entity);
        return null;
    }

    @RequestMapping(value=KVMConstant.KVM_MIGRATE_VM_PATH, method=RequestMethod.POST)
    private @ResponseBody String migrateVm(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        migrateVm(entity);
        return null;
    }

    @AsyncThread
    private void migrateVm(HttpEntity<String> entity) {
        MigrateVmCmd cmd = JSONObjectUtil.toObject(entity.getBody(), MigrateVmCmd.class);
        MigrateVmResponse rsp = new MigrateVmResponse();
        if (!config.migrateVmSuccess) {
            rsp.setSuccess(false);
            rsp.setError("on purpose");
        } else {
            synchronized (config.migrateVmCmds) {
                config.migrateVmCmds.add(cmd);
            }
            logger.debug(String.format("successfully migrated vm: %s", entity.getBody()));
        }
        reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_GET_CPU_XML_PATH, method=RequestMethod.POST)
    private @ResponseBody String getCpuXml(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        getCpuXml(entity);
        return null;
    }

    @AsyncThread
    private void getCpuXml(HttpEntity<String> entity) {
        VmGetCpuXmlResponse rsp = new VmGetCpuXmlResponse();
        rsp.setCpuXml("<cpu mode='custom' match='exact'>\n" +
                "  <model fallback='forbid'>Broadwell-IBRS</model>\n" +
                "  <vendor>Intel</vendor>\n" +
                "  <feature policy='require' name='vme'/>\n" +
                "  <feature policy='require' name='ss'/>\n" +
                "  <feature policy='require' name='ht'/>\n" +
                "</cpu>");
        reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_COMPARE_CPU_FUNCTION_PATH, method=RequestMethod.POST)
    private @ResponseBody String compareCpuFunction(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        compareCpuFunction(entity);
        return null;
    }

    @AsyncThread
    private void compareCpuFunction(HttpEntity<String> entity) {
        VmCompareCpuFunctionResponse rsp = new VmCompareCpuFunctionResponse();
        reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_CHECK_L2NOVLAN_NETWORK_PATH, method=RequestMethod.POST)
    private @ResponseBody String checkNoVlanBridge(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        checkNoVlanBridge(entity);
        return null;
    }

    @AsyncThread
    private void checkNoVlanBridge(HttpEntity<String> entity) {
        CheckBridgeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckBridgeCmd.class);
        CheckBridgeResponse rsp = new CheckBridgeResponse();
        if (!config.checkNoVlanBridgeSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            logger.debug(String.format("successfully checked bridge: %s", entity.getBody()));
        }
        reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_CHECK_L2VLAN_NETWORK_PATH, method=RequestMethod.POST)
    private @ResponseBody String checkVlanBridge(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        checkVlanBridge(entity);
        return null;
    }

    @AsyncThread
    private void checkVlanBridge(HttpEntity<String> entity) {
        CheckVlanBridgeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CheckVlanBridgeCmd.class);
        CheckVlanBridgeResponse rsp = new CheckVlanBridgeResponse();
        if (!config.checkVlanBridgeSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            logger.debug(String.format("successfully checked bridge: %s", entity.getBody()));
        }
        reply(entity, rsp);
    }

    @AsyncThread
    private void createVlanBridge(HttpEntity<String> entity) {
        CreateVlanBridgeCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateVlanBridgeCmd.class);
        CreateVlanBridgeResponse rsp = new CreateVlanBridgeResponse();
        if (config.createL2VlanNetworkSuccess) {
            config.vlanBridges.add(cmd);
            logger.debug(String.format("create bridge[name:%s, vlan:%s] successfully on kvm simulator", cmd.getBridgeName(), cmd.getVlan()));
        } else {
            rsp.setError("Fail createVlanBridge on purpose");
            rsp.setSuccess(false);
        }
        reply(entity, rsp);
    }
    
    @RequestMapping(value=KVMConstant.KVM_REALIZE_L2VLAN_NETWORK_PATH, method=RequestMethod.POST)
    private @ResponseBody String createVlanBridge(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        createVlanBridge(entity);
        return null;
    }
    
    @RequestMapping(value=KVMConstant.KVM_START_VM_PATH, method=RequestMethod.POST)
    private @ResponseBody String createVm(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        createVm(entity);
        return null;
    }

    @AsyncThread
	private void createVm(HttpEntity<String> entity) {
    	StartVmCmd cmd = JSONObjectUtil.toObject(entity.getBody(), StartVmCmd.class);
    	StartVmResponse rsp = new StartVmResponse();

    	if (config.startVmSuccess) {
            if (config.startVmFailureChance != 0) {
                if (Math.random() <= config.startVmFailureChance) {
                    rsp.setError("on purpose");
                    rsp.setSuccess(false);
                }
            } else {
                logger.debug(String.format("successfully start vm on kvm host, %s", entity.getBody()));
                synchronized (config) {
                    config.vms.put(cmd.getVmInstanceUuid(), KvmVmState.Running);
                    logger.debug(String.format("current running vm[%s]", config.vms.size()));
                }
                config.startVmCmd = cmd;
            }
    	} else {
    		String err = "fail start vm on purpose";
    		rsp.setError(err);
    		rsp.setSuccess(false);
    	}
    	replyer.reply(entity, rsp);
	}
    
    @RequestMapping(value=KVMConstant.KVM_STOP_VM_PATH, method=RequestMethod.POST)
    private @ResponseBody String stopVm(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        stopVm(entity);
        return null;
    }

    private void stopVm(HttpEntity<String> entity) {
        StopVmCmd cmd = JSONObjectUtil.toObject(entity.getBody(), StopVmCmd.class);
        StopVmResponse rsp = new StopVmResponse();
        if (config.stopVmSuccess) {
    		logger.debug(String.format("successfully stop vm on kvm host, %s", entity.getBody()));
            synchronized (config) {
                config.vms.put(cmd.getUuid(), KvmVmState.Shutdown);
            }
            config.stopVmCmds.add(cmd);
        } else {
    		String err = "fail stop vm on purpose";
    		rsp.setError(err);
    		rsp.setSuccess(false);
        }
    	replyer.reply(entity, rsp);
    }

    @RequestMapping(value = KVMConstant.KVM_PAUSE_VM_PATH, method = RequestMethod.POST)
    private @ResponseBody String suspendVm(HttpServletRequest req) throws InterruptedException{
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        suspendVm(entity);
        return null;
    }

    private void suspendVm(HttpEntity<String> entity) {
        PauseVmCmd cmd = JSONObjectUtil.toObject(entity.getBody(), PauseVmCmd.class);
        PauseVmResponse rsp = new PauseVmResponse();
        if (config.pauseVmSuccess) {
            logger.debug(String.format("successfully suspend  vm on kvm host, %s", entity.getBody()));
            synchronized (config) {
                config.vms.put(cmd.getUuid(), KvmVmState.Suspended);
            }
            config.pauseVmCmds.add(cmd);
        } else {
            String err = "fail suspend vm on purpose";
            rsp.setError(err);
            rsp.setSuccess(false);
        }
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = KVMConstant.KVM_RESUME_VM_PATH, method = RequestMethod.POST)
    private @ResponseBody String resumeVm(HttpServletRequest req) throws InterruptedException{
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        resumeVm(entity);
        return null;
    }

    private void resumeVm(HttpEntity<String> entity) {
        ResumeVmCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ResumeVmCmd.class);
        ResumeVmResponse rsp = new ResumeVmResponse();
        if (config.resumeVmSuccess) {
            logger.debug(String.format("successfully resume  vm on kvm host, %s", entity.getBody()));
            synchronized (config) {
                config.vms.put(cmd.getUuid(), KvmVmState.Running);
            }
            config.resumeVmCmds.add(cmd);
        } else {
            String err = "fail resume vm on purpose";
            rsp.setError(err);
            rsp.setSuccess(false);
        }
        replyer.reply(entity, rsp);
    }
    
    @RequestMapping(value=KVMConstant.KVM_REBOOT_VM_PATH, method=RequestMethod.POST)
    private @ResponseBody String rebootVm(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        rebootVm(entity);
        return null;
    }

    private void rebootVm(HttpEntity<String> entity) {
        RebootVmCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RebootVmCmd.class);
        RebootVmResponse rsp = new RebootVmResponse();
        if (config.rebootVmSuccess) {
    		logger.debug(String.format("successfully reboot vm on kvm host, %s", entity.getBody()));
            synchronized (config) {
                config.vms.put(cmd.getUuid(), KvmVmState.Running);
            }
            config.rebootVmCmds.add(cmd);
        } else {
    		String err = "fail reboot vm on purpose";
    		rsp.setError(err);
    		rsp.setSuccess(false);
        }
    	replyer.reply(entity, rsp);
    }
    
    @RequestMapping(value=KVMConstant.KVM_DESTROY_VM_PATH, method=RequestMethod.POST)
    private @ResponseBody String destroyVm(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        destroyVm(entity);
        return null;
    }

    @AsyncThread
    private void destroyVm(HttpEntity<String> entity) {
        DestroyVmCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DestroyVmCmd.class);
        DestroyVmResponse rsp = new DestroyVmResponse();
        if (config.destroyVmSuccess) {
            config.destroyedVmUuid = cmd.getUuid();
    		logger.debug(String.format("successfully destroy vm on kvm host, %s", entity.getBody()));
            synchronized (config) {
                config.vms.remove(cmd.getUuid());
            }
        } else {
    		String err = "fail destroy vm on purpose";
    		rsp.setError(err);
    		rsp.setSuccess(false);
        }
    	replyer.reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_GET_VNC_PORT_PATH, method=RequestMethod.POST)
    private @ResponseBody String getVncPort(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        getVncPort(entity);
        return null;
    }

    private void getVncPort(HttpEntity<String> entity) {
        KVMAgentCommands.GetVncPortCmd cmd = JSONObjectUtil.toObject(entity.getBody(), KVMAgentCommands.GetVncPortCmd.class);
        KVMAgentCommands.GetVncPortResponse rsp = new KVMAgentCommands.GetVncPortResponse();
        rsp.setPort(config.consolePort);
        logger.debug(String.format("successfully get console port[port:%s] for vm[uuid:%s]", rsp.getPort(), cmd.getVmUuid()));
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_LOGOUT_ISCSI_PATH, method=RequestMethod.POST)
    private @ResponseBody String logoutIscsiTarget(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        logoutIscsiTarget(entity);
        return null;
    }

    private void logoutIscsiTarget(HttpEntity<String> entity) {
        LogoutIscsiTargetCmd cmd = JSONObjectUtil.toObject(entity.getBody(), KVMAgentCommands.LogoutIscsiTargetCmd.class);
        LogoutIscsiTargetRsp rsp = new LogoutIscsiTargetRsp();
        synchronized (config.logoutIscsiTargetCmds) {
            config.logoutIscsiTargetCmds.add(cmd);
        }
        logger.debug(String.format("logout iscsi target: %s", cmd.getTarget()));
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value=KVMConstant.KVM_LOGIN_ISCSI_PATH, method=RequestMethod.POST)
    private @ResponseBody String loginIscsiTarget(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        loginIscsiTarget(entity);
        return null;
    }

    private void loginIscsiTarget(HttpEntity<String> entity) {
        LoginIscsiTargetCmd cmd = JSONObjectUtil.toObject(entity.getBody(), KVMAgentCommands.LoginIscsiTargetCmd.class);
        LoginIscsiTargetRsp rsp = new LoginIscsiTargetRsp();
        synchronized (config.loginIscsiTargetCmds) {
            config.loginIscsiTargetCmds.add(cmd);
        }
        logger.debug(String.format("login iscsi  target: %s", cmd.getTarget()));
        replyer.reply(entity, rsp);
    }


    @RequestMapping(value = KVMConstant.KVM_HOST_NUMA_PATH, method = RequestMethod.POST)
    private @ResponseBody String getHostNuma(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        getHostNuma(entity);
        return null;
    }

    private void getHostNuma(HttpEntity<String> entity) {
        GetHostNUMATopologyCmd cmd = JSONObjectUtil.toObject(entity.getBody(), KVMAgentCommands.GetHostNUMATopologyCmd.class);
        GetHostNUMATopologyResponse rsp = new GetHostNUMATopologyResponse();
        replyer.reply(entity, rsp);
    }


    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllException(Exception ex) {
        logger.warn(ex.getMessage(), ex);
        ModelAndView model = new ModelAndView("error/generic_error");
        model.addObject("errMsg", ex.getMessage());
        return model;
    }
}
