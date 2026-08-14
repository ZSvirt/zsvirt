package org.zstack.simulator.consoleproxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.console.ConsoleConstants;
import org.zstack.header.console.ConsoleProxyCommands;
import org.zstack.header.console.ConsoleProxyCommands.DeleteProxyCmd;
import org.zstack.header.console.ConsoleProxyCommands.DeleteProxyRsp;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.RESTFacade;
import org.zstack.simulator.AsyncRESTReplyer;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;

public class ConsoleProxySimulator {
    CLogger logger = Utils.getLogger(ConsoleProxySimulator.class);
    
    @Autowired
    private ConsoleProxySimulatorConfig config;
    @Autowired
    private RESTFacade restf;
    
    private AsyncRESTReplyer replyer = new AsyncRESTReplyer();

    @RequestMapping(value= ConsoleConstants.CONSOLE_PROXY_PING_PATH, method= RequestMethod.POST)
    public @ResponseBody
    String ping(HttpEntity<String> entity) {
        ConsoleProxyCommands.PingCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ConsoleProxyCommands.PingCmd.class);
        ConsoleProxyCommands.PingRsp rsp = new ConsoleProxyCommands.PingRsp();
        if (!config.pingSuccess) {
            throw new CloudRuntimeException("on purpose");
        } else {
            config.pingCmdList.add(cmd);
        }
        return JSONObjectUtil.toJsonString(rsp);
    }

    @AsyncThread
    private void doCheck(HttpEntity<String> entity) {
        ConsoleProxyCommands.CheckAvailabilityCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ConsoleProxyCommands.CheckAvailabilityCmd.class);
        ConsoleProxyCommands.CheckAvailabilityRsp rsp = new ConsoleProxyCommands.CheckAvailabilityRsp();
        if (!config.availableSuccess) {
            rsp.setSuccess(false);
            rsp.setError("Fail check on purpose");
        } else {
            rsp.setAvailable(config.isAvailable);
        }

        replyer.reply(entity, rsp);
    }

    @RequestMapping(value= ConsoleConstants.CONSOLE_PROXY_CHECK_PROXY_PATH, method=RequestMethod.POST)
    public @ResponseBody String check(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doCheck(entity);
        return null;
    }

    @RequestMapping(value= ConsoleConstants.CONSOLE_PROXY_ESTABLISH_PROXY_PATH, method=RequestMethod.POST)
    public @ResponseBody String estabilish(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doEstabilish(entity);
        return null;
    }

    private void doEstabilish(HttpEntity<String> entity) {
        ConsoleProxyCommands.EstablishProxyCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ConsoleProxyCommands.EstablishProxyCmd.class);
        ConsoleProxyCommands.EstablishProxyRsp rsp = new ConsoleProxyCommands.EstablishProxyRsp();
        if (!config.proxySuccess) {
            rsp.setSuccess(false);
            rsp.setError("fail establishing proxy on purpose");
        } else {
            rsp.setProxyPort(config.proxyPort);
            logger.debug(String.format("successfully establish console proxy %s at port %s", JSONObjectUtil.toJsonString(cmd), config.proxyPort));
        }

        replyer.reply(entity, rsp);
    }

    @RequestMapping(value= ConsoleConstants.CONSOLE_PROXY_DELETE_PROXY_PATH, method=RequestMethod.POST)
    public @ResponseBody String delete(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        delete(entity);
        return null;
    }

    private void delete(HttpEntity<String> entity) {
        DeleteProxyCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteProxyCmd.class);
        config.deleteProxyCmdList.add(cmd);
        DeleteProxyRsp rsp = new DeleteProxyRsp();
        replyer.reply(entity, rsp);
    }
}
