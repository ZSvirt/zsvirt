package org.zstack.simulator.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.rest.RESTFacade;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.*;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterKvmBackendCommands.CreateVritualRouterBootstrapIsoCmd;
import org.zstack.network.service.virtualrouter.VirtualRouterKvmBackendCommands.CreateVritualRouterBootstrapIsoRsp;
import org.zstack.network.service.virtualrouter.VirtualRouterKvmBackendCommands.DeleteVirtualRouterBootstrapIsoCmd;
import org.zstack.network.service.virtualrouter.VirtualRouterKvmBackendCommands.DeleteVirtualRouterBootstrapIsoRsp;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.DeleteLbCmd;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.DeleteLbRsp;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.RefreshLbCmd;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.RefreshLbRsp;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.CertificateCmd;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend.CertificateRsp;
import org.zstack.simulator.AsyncRESTReplyer;
import org.zstack.simulator.SimulatorGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;

public class VirtualRouterSimulator {
    CLogger logger = Utils.getLogger(VirtualRouterSimulator.class);

    @Autowired
    private RESTFacade restf;
    @Autowired
    private VirtualRouterSimulatorConfig config;

    private AsyncRESTReplyer replyer = new AsyncRESTReplyer();

    @AsyncThread
    private void doSetDhcpEntry(HttpEntity<String> entity) {
        AddDhcpEntryCmd cmd = JSONObjectUtil.toObject(entity.getBody(), AddDhcpEntryCmd.class);
        AddDhcpEntryRsp rsp = new AddDhcpEntryRsp();
        if (!config.setDhcpEntrySuccess) {
            rsp.setError("fail on purpose");
            rsp.setSuccess(false);
            replyer.reply(entity, rsp);
            return;
        }

        if (cmd.isRebuild()) {
            config.dhcpInfos.clear();
        }

        if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
            config.dhcpInfos.addAll(cmd.getDhcpEntries());
            for (DhcpInfo info : cmd.getDhcpEntries()) {
                config.dhcpInfoMap.put(info.getMac(), info);
            }
        }
        logger.debug(String.format("successfully set dhcp entries: %s", JSONObjectUtil.toJsonString(cmd.getDhcpEntries())));
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = VirtualRouterConstant.VR_INIT, method = RequestMethod.POST)
    private @ResponseBody
    String init(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doInit(entity);
        return null;
    }

    private void doInit(HttpEntity<String> entity) {
        InitCommand cmd = JSONObjectUtil.toObject(entity.getBody(), InitCommand.class);
        config.initCommands.add(cmd);
        config.uuid = cmd.getUuid();
        replyer.reply(entity, new InitRsp());
    }

    @RequestMapping(value = VirtualRouterConstant.VR_ADD_DHCP_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String setDhcpEntry(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doSetDhcpEntry(entity);
        return null;
    }


    @RequestMapping(value = VirtualRouterConstant.VR_REVOKE_PORT_FORWARDING, method = RequestMethod.POST)
    private @ResponseBody
    String revokePortForwardingRules(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doRevokePortForwardingRules(entity);
        return null;
    }

    @RequestMapping(value = VirtualRouterConstant.VR_CREATE_EIP, method = RequestMethod.POST)
    private @ResponseBody
    String createEip(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        createEip(entity);
        return null;
    }

    @AsyncThread
    private void createEip(HttpEntity<String> entity) {
        CreateEipCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateEipCmd.class);
        CreateEipRsp rsp = new CreateEipRsp();
        if (config.eipSuccess) {
            if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
                config.eips.add(cmd.getEip());
            }
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = VirtualRouterConstant.VR_REMOVE_EIP, method = RequestMethod.POST)
    private @ResponseBody
    String removeEip(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        removeEip(entity);
        return null;
    }

    @AsyncThread
    private void removeEip(HttpEntity<String> entity) {
        RemoveEipCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RemoveEipCmd.class);
        RemoveEipRsp rsp = new RemoveEipRsp();
        if (config.removeEipSuccess) {
            if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
                config.removedEips.add(cmd.getEip());
            }
        } else {
            rsp.setSuccess(false);
            rsp.setError("on purpose");
        }
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = VirtualRouterConstant.VR_SYNC_EIP, method = RequestMethod.POST)
    private @ResponseBody
    String syncEip(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        syncEip(entity);
        return null;
    }

    @AsyncThread
    private void syncEip(HttpEntity<String> entity) {
        SyncEipCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SyncEipCmd.class);
        SyncEipRsp rsp = new SyncEipRsp();
        if (config.syncEipSuccess) {
            config.eips.clear();
            if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
                config.eips.addAll(cmd.getEips());
            }
        } else {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        }
        replyer.reply(entity, rsp);
    }

    @AsyncThread
    private void doRevokePortForwardingRules(HttpEntity<String> entity) {
        RevokePortForwardingRuleCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RevokePortForwardingRuleCmd.class);
        RevokePortForwardingRuleRsp rsp = new RevokePortForwardingRuleRsp();
        if (!config.portForwardingSuccess) {
            rsp.setError("failed on purpose");
            rsp.setSuccess(false);
        } else {
            logger.debug(String.format("successfully removed port forwarding rules:\n%s", JSONObjectUtil.toJsonString(cmd.getRules())));
            config.removedPortForwardingRules.addAll(cmd.getRules());
        }
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = VirtualRouterConstant.VR_CREATE_VIP, method = RequestMethod.POST)
    private @ResponseBody
    String createVip(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doCreateVip(entity);
        return null;
    }

    @AsyncThread
    private void doCreateVip(HttpEntity<String> entity) {
        CreateVipCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateVipCmd.class);
        CreateVipRsp rsp = new CreateVipRsp();
        if (!config.vipSuccess) {
            rsp.setError("failed on purpose");
            rsp.setSuccess(false);
        } else {
            if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
                config.vips.addAll(cmd.getVips());
            }
            logger.debug(String.format("successfully created vips %s", JSONObjectUtil.toJsonString(cmd.getVips())));
        }
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = VirtualRouterConstant.VR_REMOVE_VIP, method = RequestMethod.POST)
    private @ResponseBody
    String removeVip(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doRemoveVip(entity);
        return null;
    }

    @AsyncThread
    private void doRemoveVip(HttpEntity<String> entity) {
        RemoveVipCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RemoveVipCmd.class);
        RemoveVipRsp rsp = new RemoveVipRsp();
        if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
            config.removedVips.addAll(cmd.getVips());
        }
        logger.debug(String.format("successfully removed vips %s", JSONObjectUtil.toJsonString(cmd.getVips())));
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = VirtualRouterConstant.VR_SYNC_PORT_FORWARDING, method = RequestMethod.POST)
    private @ResponseBody
    String syncPortForwardingRules(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doSyncPortForwardingRules(entity);
        return null;
    }

    private void doSyncPortForwardingRules(HttpEntity<String> entity) {
        SyncPortForwardingRuleCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SyncPortForwardingRuleCmd.class);
        SyncPortForwardingRuleRsp rsp = new SyncPortForwardingRuleRsp();
        if (!config.portForwardingSuccess) {
            rsp.setError("failed on purpose");
            rsp.setSuccess(false);
        } else {
            config.portForwardingRules.clear();
            if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
                config.portForwardingRules.addAll(cmd.getRules());
            }
            logger.debug(String.format("successfully synced port forwarding rules: \n%s", JSONObjectUtil.toJsonString(cmd.getRules())));
        }
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = VirtualRouterConstant.VR_CREATE_PORT_FORWARDING, method = RequestMethod.POST)
    private @ResponseBody
    String createPortForwardingRules(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doCreatePortForwardingRules(entity);
        return null;
    }

    @AsyncThread
    private void doCreatePortForwardingRules(HttpEntity<String> entity) {
        CreatePortForwardingRuleCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreatePortForwardingRuleCmd.class);
        CreatePortForwardingRuleRsp rsp = new CreatePortForwardingRuleRsp();
        if (!config.portForwardingSuccess) {
            rsp.setError("failed on purpose");
            rsp.setSuccess(false);
        } else {
            if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
                config.portForwardingRules.addAll(cmd.getRules());
            }
            logger.debug(String.format("successfully added port forwarding rules:\n%s", JSONObjectUtil.toJsonString(cmd.getRules())));
        }
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = VirtualRouterConstant.VR_ECHO_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String echo(HttpServletRequest req) {
        logger.debug("virtual router connected");
        return null;
    }

    @RequestMapping(value = VirtualRouterConstant.VR_PING, method = RequestMethod.POST)
    private @ResponseBody
    String ping(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        PingCmd cmd = JSONObjectUtil.toObject(entity.getBody(), PingCmd.class);
        config.pingCmds.add(cmd);

        PingRsp rsp = new PingRsp();
        rsp.setVersion("3.10.0.0");
        rsp.setUuid(config.uuid);
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = VirtualRouterConstant.VR_SYNC_SNAT_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String syncSNAT(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        syncSNAT(entity);
        return null;
    }

    @AsyncThread
    private void syncSNAT(HttpEntity<String> entity) {
        SyncSNATCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SyncSNATCmd.class);
        SyncSNATRsp rsp = new SyncSNATRsp();
        if (!config.setSNATSuccess) {
            rsp.setError("fail on purpose");
            rsp.setSuccess(false);
        } else {
            config.snatInfos.clear();
            if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
                config.snatInfos.addAll(cmd.getSnats());
            }
            logger.debug(String.format("successfully sync snats: %s", JSONObjectUtil.toJsonString(cmd.getSnats())));
        }
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String refreshLb(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        RefreshLbCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RefreshLbCmd.class);
        RefreshLbRsp rsp = new RefreshLbRsp();

        if (!config.refreshLbSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            config.refreshLbCmds.add(cmd);
        }

        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = VirtualRouterLoadBalancerBackend.DELETE_LB_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String deleteLb(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        DeleteLbCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteLbCmd.class);
        DeleteLbRsp rsp = new DeleteLbRsp();
        config.deleteLbCmds.add(cmd);
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = VirtualRouterLoadBalancerBackend.CREATE_CERTIFICATE_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String createCertificate(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        CertificateCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CertificateCmd.class);
        CertificateRsp rsp = new CertificateRsp();

        if (!config.refreshLbSuccess) {
            rsp.setError("on purpose");
            rsp.setSuccess(false);
        } else {
            config.certificateCmd = cmd;
        }

        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = VirtualRouterLoadBalancerBackend.DELETE_CERTIFICATE_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String deleteCertificate(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        CertificateCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CertificateCmd.class);
        CertificateRsp rsp = new CertificateRsp();
        config.certificateCmd = cmd;
        replyer.reply(entity, rsp);
        return null;
    }

    @RequestMapping(value = VirtualRouterConstant.VR_SET_SNAT_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String setSNAT(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doSetSNAT(entity);
        return null;
    }

    @AsyncThread
    private void doSetSNAT(HttpEntity<String> entity) {
        SetSNATCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SetSNATCmd.class);
        SetSNATRsp rsp = new SetSNATRsp();
        if (!config.setSNATSuccess) {
            rsp.setError("fail on purpose");
            rsp.setSuccess(false);
            replyer.reply(entity, rsp);
            return;
        }

        if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
            config.snatInfos.add(cmd.getSnat());
        }
        logger.debug(String.format("successfully set snat: %s", JSONObjectUtil.toJsonString(cmd.getSnat())));
        replyer.reply(entity, rsp);
    }

    @RequestMapping(value = VirtualRouterConstant.VR_REMOVE_DNS_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String removeDNS(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        RemoveDnsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RemoveDnsCmd.class);
        config.removeDnsCmds.add(cmd);
        replyer.reply(entity, new RemoveDnsRsp());
        return null;
    }

    @RequestMapping(value = VirtualRouterConstant.VR_SET_DNS_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String setDNS(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doSetDNS(entity);
        return null;
    }

    private void doSetDNS(HttpEntity<String> entity) {
        SetDnsCmd cmd = JSONObjectUtil.toObject(entity.getBody(), SetDnsCmd.class);
        SetDnsRsp rsp = new SetDnsRsp();
        if (!config.setDnsSuccess) {
            rsp.setError("fail on purpose");
            rsp.setSuccess(true);
            replyer.reply(entity, rsp);
            return;
        }

        config.dnsInfo.clear();
        if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
            config.dnsInfo.addAll(cmd.getDns());
        }
        logger.debug(String.format("successfully configured dns: %s", JSONObjectUtil.toJsonString(cmd.getDns())));
        replyer.reply(entity, rsp);
    }

    @AsyncThread
    private void doConfigureNic(HttpEntity<String> entity) {
        ConfigureNicCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ConfigureNicCmd.class);
        ConfigureNicRsp rsp = new ConfigureNicRsp();
        if (!config.configureNicSuccess) {
            rsp.setError("fail on purpose");
            rsp.setSuccess(false);
            replyer.reply(entity, rsp);
            return;
        }

        logger.debug(String.format("successfully configured nics: %s", JSONObjectUtil.toJsonString(cmd.getNics())));
        replyer.reply(entity, rsp);
        return;
    }

    @RequestMapping(value = VirtualRouterConstant.VR_CONFIGURE_NIC_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String configureNic(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doConfigureNic(entity);
        return null;
    }

    @AsyncThread
    private void doConfigureNicFirewallDefaultAction(HttpEntity<String> entity) {
        ConfigureNicFirewallDefaultActionCmd cmd = JSONObjectUtil.toObject(entity.getBody(), ConfigureNicFirewallDefaultActionCmd.class);
        ConfigureNicFirewallDefaultActionRsp rsp = new ConfigureNicFirewallDefaultActionRsp();

        logger.debug(String.format("successfully configured nics: %s firewall default action", JSONObjectUtil.toJsonString(cmd.getNics())));
        replyer.reply(entity, rsp);
        return;
    }

    @RequestMapping(value = VirtualRouterConstant.VR_CONFIGURE_NIC_FIREWALL_DEFAULT_ACTION_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String configureNicFirewallDefaultAction(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doConfigureNicFirewallDefaultAction(entity);
        return null;
    }

    @RequestMapping(value = VirtualRouterConstant.VR_REMOVE_DHCP_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String removeDchpEntry(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doRemoveDhcpEntry(entity);
        return null;
    }

    @RequestMapping(value = VirtualRouterConstant.VR_KVM_CREATE_BOOTSTRAP_ISO_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String prepareIso(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doPrepareIso(entity);
        return null;
    }

    @RequestMapping(value = VirtualRouterConstant.VR_KVM_DELETE_BOOTSTRAP_ISO_PATH, method = RequestMethod.POST)
    private @ResponseBody
    String deleteIso(HttpServletRequest req) {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        doDeleteIso(entity);
        return null;
    }

    private void doDeleteIso(HttpEntity<String> entity) {
        DeleteVirtualRouterBootstrapIsoCmd cmd = JSONObjectUtil.toObject(entity.getBody(), DeleteVirtualRouterBootstrapIsoCmd.class);
        logger.debug(String.format("successfully deleted iso at %s", cmd.getIsoPath()));
        DeleteVirtualRouterBootstrapIsoRsp rsp = new DeleteVirtualRouterBootstrapIsoRsp();
        replyer.reply(entity, rsp);
    }

    private void doPrepareIso(HttpEntity<String> entity) {
        CreateVritualRouterBootstrapIsoCmd cmd = JSONObjectUtil.toObject(entity.getBody(), CreateVritualRouterBootstrapIsoCmd.class);
        logger.debug(String.format("successfully create iso at %s, %s", cmd.getIsoPath(), JSONObjectUtil.toJsonString(cmd.getIsoInfo())));
        CreateVritualRouterBootstrapIsoRsp rsp = new CreateVritualRouterBootstrapIsoRsp();
        replyer.reply(entity, rsp);
    }

    private void doRemoveDhcpEntry(HttpEntity<String> entity) {
        RemoveDhcpEntryRsp rsp = new RemoveDhcpEntryRsp();
        if (config.removedDhcpSuccess) {
            RemoveDhcpEntryCmd cmd = JSONObjectUtil.toObject(entity.getBody(), RemoveDhcpEntryCmd.class);
            if (!SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND) {
                config.removedDhcp.addAll(cmd.getDhcpEntries());
            }
            logger.debug(String.format("successfully removed dhcp entries: %s", JSONObjectUtil.toJsonString(cmd.getDhcpEntries())));
        } else {
            String err = "failed to remove dhcp on purpose";
            logger.debug(err);
            rsp.setError(err);
            rsp.setSuccess(false);
        }

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
