package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.kvm.KVMAgentCommands.AgentResponse;
import org.zstack.network.service.flat.FlatDhcpBackend.*;
import org.zstack.network.service.flat.FlatEipBackend.ApplyEipCmd;
import org.zstack.network.service.flat.FlatEipBackend.BatchApplyEipCmd;
import org.zstack.network.service.flat.FlatEipBackend.BatchDeleteEipCmd;
import org.zstack.network.service.flat.FlatEipBackend.DeleteEipCmd;
import org.zstack.network.service.flat.FlatNetworkServiceConstant.AgentRsp;
import org.zstack.network.service.flat.FlatUserdataBackend.*;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 9/19/2015.
 */
public class FlatNetworkServiceSimulator {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private FlatNetworkServiceSimulatorConfig config;

    public void reply(HttpEntity<String> entity, Object rsp) {
        String taskUuid = entity.getHeaders().getFirst(RESTConstant.TASK_UUID);
        String callbackUrl = entity.getHeaders().getFirst(RESTConstant.CALLBACK_URL);
        String rspBody = JSONObjectUtil.toJsonString(rsp);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(rspBody.length());
        headers.set(RESTConstant.TASK_UUID, taskUuid);
        HttpEntity<String> rreq = new HttpEntity<String>(rspBody, headers);
        restf.getRESTTemplate().exchange(callbackUrl, HttpMethod.POST, rreq, String.class);
    }

    @RequestMapping(value = FlatDhcpBackend.APPLY_DHCP_PATH, method = RequestMethod.POST)
    public @ResponseBody String setDhcp(HttpEntity<String> entity) {
        ApplyDhcpCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ApplyDhcpCmd.class);
        config.applyDhcpCmdList.add(cmd);
        ApplyDhcpRsp rsp = new ApplyDhcpRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatDhcpBackend.PREPARE_DHCP_PATH, method = RequestMethod.POST)
    public @ResponseBody String prepareDhcp(HttpEntity<String> entity) {
        PrepareDhcpCmd cmd = JSONObjectUtil.toObject(entity.getBody(), PrepareDhcpCmd.class);
        config.prepareDhcpCmdList.add(cmd);
        reply(entity, new PrepareDhcpRsp());
        return null;
    }

    @RequestMapping(value = FlatDhcpBackend.RESET_DEFAULT_GATEWAY_PATH, method = RequestMethod.POST)
    public @ResponseBody String resetDefaultGateway(HttpEntity<String> entity) {
        ResetDefaultGatewayCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ResetDefaultGatewayCmd.class);
        config.resetDefaultGatewayCmds.add(cmd);
        reply(entity, new ResetDefaultGatewayRsp());
        return null;
    }

    @RequestMapping(value = FlatDhcpBackend.RELEASE_DHCP_PATH, method = RequestMethod.POST)
    public @ResponseBody String releaseDhcp(HttpEntity<String> entity) {
        ReleaseDhcpCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ReleaseDhcpCmd.class);
        config.releaseDhcpCmds.add(cmd);
        ReleaseDhcpRsp rsp = new ReleaseDhcpRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH, method = RequestMethod.POST)
    public @ResponseBody String deleteNamespace(HttpEntity<String> entity) {
        DeleteNamespaceCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteNamespaceCmd.class);
        config.deleteNamespaceCmds.add(cmd);
        DeleteNamespaceRsp rsp = new DeleteNamespaceRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatDhcpBackend.DHCP_FLUSH_NAMESPACE_PATH, method = RequestMethod.POST)
    public @ResponseBody String flushNamespace(HttpEntity<String> entity) {
        FlushDhcpNamespaceCmd cmd = JSONObjectUtil.toObject(entity.getBody(), FlushDhcpNamespaceCmd.class);
        config.flushNamespaceCmds.add(cmd);
        FlushDhcpNamespaceRsp rsp = new FlushDhcpNamespaceRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatDhcpBackend.ARPING_NAMESPACE_PATH, method = RequestMethod.POST)
    public @ResponseBody String arpingNamespace(HttpEntity<String> entity) {
        ArpingCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ArpingCmd.class);
        config.arpingNamespaceCmds.add(cmd);
        Map<String, List<String>> result = new HashMap<>();
        ArpingRsp rsp = new ArpingRsp();
        rsp.result = result;
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatUserdataBackend.APPLY_USER_DATA, method = RequestMethod.POST)
    public @ResponseBody String applyUserdata(HttpEntity<String> entity) {
        ApplyUserdataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ApplyUserdataCmd.class);
        config.applyUserdataCmds.add(cmd);
        ApplyUserdataRsp rsp = new ApplyUserdataRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatUserdataBackend.BATCH_APPLY_USER_DATA, method = RequestMethod.POST)
    public @ResponseBody String batchApplyUserdata(HttpEntity<String> entity) {
        BatchApplyUserdataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), BatchApplyUserdataCmd.class);
        config.batchApplyUserdataCmds.add(cmd);
        AgentResponse rsp = new AgentResponse();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatUserdataBackend.RELEASE_USER_DATA, method = RequestMethod.POST)
    public @ResponseBody String releaseUserdata(HttpEntity<String> entity) {
        ReleaseUserdataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ReleaseUserdataCmd.class);
        config.releaseUserdataCmds.add(cmd);
        ReleaseUserdataRsp rsp = new ReleaseUserdataRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatUserdataBackend.CLEANUP_USER_DATA, method = RequestMethod.POST)
    public @ResponseBody String cleanupUserdata(HttpEntity<String> entity) {
        CleanupUserdataCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CleanupUserdataCmd.class);
        config.cleanupUserdataCmds.add(cmd);
        CleanupUserdataRsp rsp = new CleanupUserdataRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatDhcpBackend.DHCP_CONNECT_PATH, method = RequestMethod.POST)
    public @ResponseBody String connect(HttpEntity<String> entity) {
        ConnectCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ConnectCmd.class);
        config.connectCmds.add(cmd);
        ConnectRsp rsp = new ConnectRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatEipBackend.APPLY_EIP_PATH, method = RequestMethod.POST)
    public @ResponseBody String applyEip(HttpEntity<String> entity) {
        ApplyEipCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ApplyEipCmd.class);
        config.applyEipCmds.add(cmd);
        AgentRsp rsp = new AgentRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatEipBackend.DELETE_EIP_PATH, method = RequestMethod.POST)
    public @ResponseBody String deleteEip(HttpEntity<String> entity) {
        DeleteEipCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteEipCmd.class);
        config.deleteEipCmds.add(cmd);
        AgentRsp rsp = new AgentRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatEipBackend.BATCH_APPLY_EIP_PATH, method = RequestMethod.POST)
    public @ResponseBody String batchApplyEip(HttpEntity<String> entity) {
        BatchApplyEipCmd cmd = JSONObjectUtil.toObject(entity.getBody(), BatchApplyEipCmd.class);
        config.batchApplyEipCmds.add(cmd);
        AgentRsp rsp = new AgentRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatEipBackend.BATCH_DELETE_EIP_PATH, method = RequestMethod.POST)
    public @ResponseBody String batchDeleteEip(HttpEntity<String> entity) {
        BatchDeleteEipCmd cmd = JSONObjectUtil.toObject(entity.getBody(), BatchDeleteEipCmd.class);
        config.batchDeleteEipCmds.add(cmd);
        AgentRsp rsp = new AgentRsp();
        reply(entity, rsp);
        return null;
    }
}
