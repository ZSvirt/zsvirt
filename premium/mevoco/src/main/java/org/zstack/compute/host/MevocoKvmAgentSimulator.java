package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmAccountPreference;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.simulator.AsyncRESTReplyer;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by mingjian.deng on 16/12/1.
 */
public class MevocoKvmAgentSimulator {
    private final CLogger logger = Utils.getLogger(MevocoKvmAgentSimulator.class);

    private final AsyncRESTReplyer replyer = new AsyncRESTReplyer();

    @Autowired
    private RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;

    @RequestMapping(value = MevocoKVMConstant.KVM_VM_CHANGE_PASSWORD_PATH, method = RequestMethod.POST)
    public @ResponseBody String changeVmPassword(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        MevocoKVMAgentCommands.ChangeVmPasswordCmd cmd =
                JSONObjectUtil.toObject(entity.getBody(), MevocoKVMAgentCommands.ChangeVmPasswordCmd.class);
        String vmUuid = cmd.getAccountPerference().getVmUuid();
        String account = cmd.getAccountPerference().getUserAccount();
        String password= cmd.getAccountPerference().getAccountPassword();
        logger.debug(password);
        logger.debug(account);
        logger.debug(vmUuid);
        MevocoKVMAgentCommands.ChangeVmPasswordResponse rsp = new MevocoKVMAgentCommands.ChangeVmPasswordResponse();
        rsp.setSuccess(true);
        rsp.setVmAccountPerference(new VmAccountPreference(vmUuid, account, password));
        replyer.reply(entity, rsp);
        return JSONObjectUtil.toJsonString(rsp);
    }

    @RequestMapping(value = MevocoKVMConstant.SET_NIC_QOS, method = RequestMethod.POST)
    public @ResponseBody String setNicQos(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        MevocoKVMAgentCommands.NicQosCmd cmd = JSONObjectUtil.toObject(entity.getBody(), MevocoKVMAgentCommands.NicQosCmd.class);
        String vmUuid = cmd.getVmUuid();
        String internalName = cmd.getInternalName();
        long in = cmd.getInboundBandwidth();
        long out = cmd.getOutboundBandwidth();
        logger.debug(String.format("[in setNicQos]Simulator vmUuid: %s", vmUuid));
        logger.debug(String.format("[in setNicQos]Simulator internalName: %s", internalName));
        logger.debug(String.format("[in setNicQos]Simulator in: %d", in));
        logger.debug(String.format("[in setNicQos]Simulator out: %d", out));
        KVMAgentCommands.AgentResponse rsp = new KVMAgentCommands.AgentResponse();

        if (internalName == null || vmUuid == null ) {
            rsp.setSuccess(false);
        }

        replyer.reply(entity, rsp);
        return JSONObjectUtil.toJsonString(rsp);
    }

    @RequestMapping(value = MevocoKVMConstant.GET_NIC_QOS, method = RequestMethod.POST)
    public @ResponseBody String getNicQos(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        MevocoKVMAgentCommands.NicQosCmd cmd = JSONObjectUtil.toObject(entity.getBody(), MevocoKVMAgentCommands.NicQosCmd.class);

        String vmUuid = cmd.getVmUuid();
        String internalName = cmd.getInternalName();

        logger.debug(String.format("[in getNicQos]Simulator vmUuid: %s", vmUuid));
        logger.debug(String.format("[in getNicQos]Simulator internalName: %s", internalName));
        MevocoKVMAgentCommands.NicAgentResponse rsp = new MevocoKVMAgentCommands.NicAgentResponse();

        if (internalName == null || vmUuid == null) {
            rsp.setSuccess(false);
            rsp.setError("internalName or vmUuid is null");
            replyer.reply(entity, rsp);
            return JSONObjectUtil.toJsonString(rsp);
        }

        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vmUuid);
        q.add(VmNicVO_.internalName, SimpleQuery.Op.EQ, internalName);
        VmNicVO vvo = q.find();
        if (vvo == null) {
            rsp.setSuccess(false);
            rsp.setError("no such nic");
            return JSONObjectUtil.toJsonString(rsp);
        }
        String nicUuid = vvo.getUuid();
        String inbound = MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.getTokenByResourceUuid(nicUuid, InstanceOfferingVO.class, MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH_TOKEN);
        logger.debug(String.format("[in getNicQos]inbound 1: %s", inbound));
        if(inbound == null) {
            // find the systemTag in vmUuid, which attched by VmInstanceVO
            if (vvo.getVmInstanceUuid() != null) {
                inbound = MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.getTokenByResourceUuid(vmUuid, VmInstanceVO.class, MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH_TOKEN);
                logger.debug(String.format("[in getNicQos]inbound 2: %s", inbound));
            }
        }
        String outbound = MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.getTokenByResourceUuid(nicUuid, InstanceOfferingVO.class, MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN);
        logger.debug(String.format("[in getNicQos]outbound 1: %s", outbound));
        if(outbound == null) {
            // find the systemTag in vmUuid, which attched by VmInstanceVO
            if (vvo.getVmInstanceUuid() != null) {
                outbound = MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.getTokenByResourceUuid(vmUuid, VmInstanceVO.class, MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN);
                logger.debug(String.format("[in getNicQos]outbound 2: %s", outbound));
            }
        }
        // qemu set value was (real value / 1024)
        if (inbound != null) {
            rsp.inbound = String.valueOf(Long.valueOf(inbound));
        }
        if (outbound != null) {
            rsp.outbound = String.valueOf(Long.valueOf(outbound));
        }
        replyer.reply(entity, rsp);
        return JSONObjectUtil.toJsonString(rsp);
    }
}
