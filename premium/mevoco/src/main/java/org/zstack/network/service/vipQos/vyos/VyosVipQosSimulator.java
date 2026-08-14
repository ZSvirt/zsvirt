package org.zstack.network.service.vipQos.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.header.rest.RESTFacade;
import org.zstack.simulator.AsyncRESTReplyer;
import org.zstack.utils.gson.JSONObjectUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by shixin on 2017/12/15.
 */
public class VyosVipQosSimulator {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private VyosVipQosSimulatorConfig config;

    private AsyncRESTReplyer replyer = new AsyncRESTReplyer();

    @RequestMapping(value = VyosVipQosBackend.VR_SET_VIP_QOS, method = RequestMethod.POST)
    private @ResponseBody
    String setVipQos(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        VyosVipQosBackend.SetVipQosRsp rsp = new VyosVipQosBackend.SetVipQosRsp();
        if (config.VipQosSuccess) {
            VyosVipQosBackend.SetVipQosCmd cmd = JSONObjectUtil.toObject(entity.getBody(), VyosVipQosBackend.SetVipQosCmd.class);
            config.SetVipQosCmdList.add(cmd);
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = VyosVipQosBackend.VR_DELETE_VIPALL_QOS, method = RequestMethod.POST)
    public @ResponseBody
    String deleteVipAllQos(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        VyosVipQosBackend.DeleteVipAllQosRsp rsp = new VyosVipQosBackend.DeleteVipAllQosRsp();
        if (config.VipQosSuccess) {
            VyosVipQosBackend.DeleteVipAllQosCmd cmd = JSONObjectUtil.toObject(entity.getBody(), VyosVipQosBackend.DeleteVipAllQosCmd.class);
            config.DeleteVipAllQosCmdList.add(cmd);
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }

        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = VyosVipQosBackend.VR_DELETE_VIP_QOS, method = RequestMethod.POST)
    public  @ResponseBody
    String deleteVipQos(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        VyosVipQosBackend.DeleteVipQosRsp rsp = new VyosVipQosBackend.DeleteVipQosRsp();
        if (config.VipQosSuccess) {
            VyosVipQosBackend.DeleteVipQosCmd cmd = JSONObjectUtil.toObject(entity.getBody(), VyosVipQosBackend.DeleteVipQosCmd.class);
            config.DeleteVipQosCmdList.add(cmd);
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }

        replyer.reply(entity, rsp);
        return null;
    }
}
