package org.zstack.simulator.appliancevm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.appliancevm.ApplianceVmCommands.InitCmd;
import org.zstack.appliancevm.ApplianceVmCommands.InitRsp;
import org.zstack.appliancevm.ApplianceVmCommands.RefreshFirewallCmd;
import org.zstack.appliancevm.ApplianceVmCommands.RefreshFirewallRsp;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmKvmCommands;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.rest.RESTFacade;
import org.zstack.simulator.AsyncRESTReplyer;
import org.zstack.simulator.SimulatorGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;

/**
 */
public class ApplianceVmSimulator {
    private static final CLogger logger = Utils.getLogger(ApplianceVmSimulator.class);

    @Autowired
    private ApplianceVmSimulatorConfig config;
    @Autowired
    private RESTFacade restf;

    AsyncRESTReplyer replyer = new AsyncRESTReplyer();

    @RequestMapping(value = ApplianceVmConstant.INIT_PATH, method= RequestMethod.POST)
    private @ResponseBody String init(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        InitCmd cmd = JSONObjectUtil.toObject(entity.getBody(), InitCmd.class);
        config.initCmds.add(cmd);
        replyer.reply(entity, new InitRsp());
        return null;
    }

    @RequestMapping(value = ApplianceVmConstant.REFRESH_FIREWALL_PATH, method= RequestMethod.POST)
    private @ResponseBody String refreshFirewall(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        refreshFirewall(entity);
        return null;
    }

    @AsyncThread
    private void refreshFirewall(HttpEntity<String> entity) {
        RefreshFirewallCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RefreshFirewallCmd.class);
        RefreshFirewallRsp rsp = new RefreshFirewallRsp();
        if (!config.refreshFirewallSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            logger.debug(entity.getBody());
            if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
                synchronized (config.firewallRules) {
                    config.firewallRules.clear();
                    config.firewallRules.addAll(cmd.getRules());
                }
            }
        }

        replyer.reply(entity, rsp);
    }

    @RequestMapping(value= ApplianceVmKvmCommands.PrepareBootstrapInfoCmd.PATH, method= RequestMethod.POST)
    private @ResponseBody String prepareBootStrapInfo(HttpServletRequest req) throws InterruptedException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doPrepareBootStrapInfo(entity);
        return null;
    }

    @AsyncThread
    private void doPrepareBootStrapInfo(HttpEntity<String> entity) {
        ApplianceVmKvmCommands.PrepareBootstrapInfoCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ApplianceVmKvmCommands.PrepareBootstrapInfoCmd.class);
        ApplianceVmKvmCommands.PrepareBootstrapInfoRsp rsp = new ApplianceVmKvmCommands.PrepareBootstrapInfoRsp();
        if (!config.prepareBootstrapInfoSuccess) {
            rsp.setError(String.format("failed on purpose"));
            rsp.setSuccess(false);
        } else {
            config.bootstrapInfo = cmd.getInfo();
            logger.debug(String.format("successfully set bootstrap info: %s", JSONObjectUtil.toJsonString(cmd.getInfo())));
        }
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = ApplianceVmConstant.ECHO_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String echo(HttpServletRequest req) {
        logger.debug("appliance vm connected");
        return null;
    }
}
