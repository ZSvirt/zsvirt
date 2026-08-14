package org.zstack.ipsec.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.header.rest.RESTFacade;
import org.zstack.ipsec.vyos.VyosIPsecBackend.*;
import org.zstack.simulator.AsyncRESTReplyer;
import org.zstack.utils.gson.JSONObjectUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by xing5 on 2016/11/8.
 */
public class VyosIPsecSimulator {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private VyosIPsecSimulatorConfig config;

    private final AsyncRESTReplyer replyer = new AsyncRESTReplyer();

    @RequestMapping(value = VyosIPsecBackend.CREATE_IPSEC_CONNECTION, method = RequestMethod.POST)
    private @ResponseBody
    String createIPsec(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        CreateIPsecConnectionRsp rsp = new CreateIPsecConnectionRsp();
        if (config.createIPsecConnectionSuccess) {
            CreateIPsecConnectionCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateIPsecConnectionCmd.class);
            config.createIPsecConnectionCmdList.add(cmd);
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = VyosIPsecBackend.SYNC_IPSEC_CONNECTION, method = RequestMethod.POST)
    private @ResponseBody
    String syncIPsec(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        SyncIPsecConnectionCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SyncIPsecConnectionCmd.class);
        config.syncIPsecConnectionCmds.add(cmd);
        SyncIPsecConnectionRsp rsp = new SyncIPsecConnectionRsp();
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = VyosIPsecBackend.DELETE_IPSEC_CONNECTION, method = RequestMethod.POST)
    private @ResponseBody
    String deleteIPsec(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        DeleteIPsecConnectionCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteIPsecConnectionCmd.class);
        config.deleteIPsecConnectionCmds.add(cmd);
        DeleteIPsecConnectionRsp rsp = new DeleteIPsecConnectionRsp();
        replyer.reply(entity, rsp);
        return null;
    }
}
