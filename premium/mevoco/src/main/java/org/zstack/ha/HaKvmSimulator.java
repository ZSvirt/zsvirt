package org.zstack.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.ha.HaKvmHostSiblingChecker.ScanCmd;
import org.zstack.ha.HaKvmHostSiblingChecker.ScanRsp;
import org.zstack.ha.SelfFencerKvmBackend.AgentRsp;
import org.zstack.ha.SelfFencerKvmBackend.CancelSelfFencerCmd;
import org.zstack.ha.SelfFencerKvmBackend.SetupSelfFencerCmd;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by xing5 on 2016/3/29.
 */
public class HaKvmSimulator {
    @Autowired
    private HaKvmSimulatorConfig config;
    @Autowired
    private RESTFacade restf;

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

    @RequestMapping(value= HaKvmHostSiblingChecker.SCAN_HOST_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String scanHost(HttpEntity<String> entity) {
        ScanCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ScanCmd.class);
        config.scanCmds.add(cmd);
        Boolean success = config.scanSuccess.get(cmd.hostUuid);
        ScanRsp rsp = new ScanRsp();
        if (success != null && !success) {
            rsp.success = false;
        } else {
            rsp.result = config.scanResult;
        }

        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= SelfFencerKvmBackend.SETUP_SELF_FENCER_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String setupSelfFencer(HttpEntity<String> entity) {
        SetupSelfFencerCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SetupSelfFencerCmd.class);
        config.setupSelfFencerCmds.add(cmd);
        AgentRsp rsp = new AgentRsp();
        reply(entity, rsp);
        return null;
    }

    @RequestMapping(value= SelfFencerKvmBackend.CANCEL_SELF_FENCER_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String cancelSelfFencer(HttpEntity<String> entity) {
        CancelSelfFencerCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CancelSelfFencerCmd.class);
        config.cancelSelfFencerCmds.add(cmd);
        AgentRsp rsp = new AgentRsp();
        reply(entity, rsp);
        return null;
    }
}
