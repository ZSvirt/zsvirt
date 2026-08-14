package org.zstack.network.service.vipQos.flat;

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
 * Created by shixin on 2018/01/02.
 */
public class FlatVipQosSimulator {
    @Autowired
    private RESTFacade restf;
    @Autowired
    private FlatVipQosSimulatorConfig config;

    private AsyncRESTReplyer replyer = new AsyncRESTReplyer();

    @RequestMapping(value = FlatVipQosBackend.FLAT_SET_VIP_QOS, method = RequestMethod.POST)
    public  @ResponseBody
    String setVipQos(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        FlatVipQosBackend.SetVipQosRsp rsp = new FlatVipQosBackend.SetVipQosRsp();
        if (config.VipQosSuccess) {
            FlatVipQosBackend.SetVipQosCmd cmd = JSONObjectUtil.toObject(entity.getBody(), FlatVipQosBackend.SetVipQosCmd.class);
            config.SetVipQosCmdList.add(cmd);
        } else {
            rsp.error = "on purpose";
            rsp.success = false;
        }
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatVipQosBackend.FLAT_DELETE_VIPALL_QOS, method = RequestMethod.POST)
    public @ResponseBody
    String syncVipQos(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        FlatVipQosBackend.DeleteVipAllQosRsp rsp = new FlatVipQosBackend.DeleteVipAllQosRsp();
        if (config.VipQosSuccess) {
            FlatVipQosBackend.DeleteVipAllQosCmd cmd = JSONObjectUtil.toObject(entity.getBody(), FlatVipQosBackend.DeleteVipAllQosCmd.class);
            config.DeleteVipAllQosCmdList.add(cmd);
        } else {
            rsp.error = "on purpose";
            rsp.success = false;
        }

        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = FlatVipQosBackend.FLAT_DELETE_VIP_QOS, method = RequestMethod.POST)
    public @ResponseBody
    String deleteVipQos(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        FlatVipQosBackend.DeleteVipQosRsp rsp = new FlatVipQosBackend.DeleteVipQosRsp();
        if (config.VipQosSuccess) {
            FlatVipQosBackend.DeleteVipQosCmd cmd = JSONObjectUtil.toObject(entity.getBody(), FlatVipQosBackend.DeleteVipQosCmd.class);
            config.DeleteVipQosCmdList.add(cmd);
        } else {
            rsp.error = "on purpose";
            rsp.success = false;
        }

        replyer.reply(entity, rsp);
        return null;
    }
}
