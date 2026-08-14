package org.zstack.snmp.agent;

import org.apache.commons.lang.StringUtils;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.EventRunnable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeReturnValueCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.snmp.agent.mib.MOTableFactory;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;
import org.zstack.utils.IptablesUtils;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * @Author : jingwang
 * @create 2023/7/18 2:48 PM
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SnmpAgentInstance extends BaseSnmpAgent {
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;

    private static final CLogger logger = Utils.getLogger(SnmpAgentInstance.class);
    private SnmpAgentVO self;
    private static SnmpAgentInstance instance;

    private SnmpAgentInstance() {
        List<SnmpAgentVO> lst = dbf.listAll(SnmpAgentVO.class);
        if (lst.isEmpty()) {
            throw new OperationFailureException(operr("can't get SnmpAgentImpl instance, due to no SnmpAgentVO exist."));
        }
        if (lst.size() > 1) {
            throw new OperationFailureException(operr("more than one SnmpAgentVO exist."));
        }
        onSnmpAgentEvents();
        self = lst.get(0);
    }

    private void onSnmpAgentEvents() {
        evtf.on(SnmpAgentEvents.SNMP_AGENT_START_FAILED, new EventRunnable() {
            @Override
            protected void run() {
                if (agentState == SnmpAgentState.STATE_RUNNING) {
                    stop(new NopeReturnValueCompletion());
                } else {
                    logger.warn(String.format("SNMP agent is on %s state", agentState.name()));
                }
            }
        });
    }

    public static synchronized SnmpAgentInstance getInstance() {
        if (instance == null) {
            instance = new SnmpAgentInstance();
        }
        return instance;
    }

    @Override
    public void start(ReturnValueCompletion<SnmpAgentInventory> completion) {
        refreshVO();
        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("start-snmp-agent-%s-on-port-%s", getSelf().getUuid(), getSelf().getPort()));
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("init-snmp-agent-%s", getSelf().getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                initSnmpAgent(new Completion(trigger) {
                    @Override
                    public void success() {
                        addCommunityAndUsmUser();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void initSnmpAgent(Completion completion) {
                try {
                    init();
                    completion.success();
                } catch (Exception e) {
                    completion.fail(operr(String.format("failed to start snmp agent[%s], %s", getSelf().getUuid(), e.getMessage())));
                }
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("finish-init-snmp-agent-%s", getSelf().getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                finishInit();
                trigger.next();
            }
        }).then(new Flow() {
            String __name__ = String.format("run-snmp-agent-%s", getSelf().getUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                listenSnmpAgentSession(new Completion(trigger) {
                    @Override
                    public void success() {
                        agentState = SnmpAgentState.STATE_RUNNING;
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void listenSnmpAgentSession(Completion c) {
                try {
                    session.listen();
                    c.success();
                } catch (IOException e) {
                    c.fail(operr("failed to start snmp agent[%s] on port %s, due to %s",
                            getSelf().getUuid(), getSelf().getPort(), e.getMessage()));
                }
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                closeSession();
                trigger.rollback();
            }
        }).then(new Flow() {
            String __name__ = "insert-snmp-agent-iptables-rules";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                addSnmpAgentIptablesRule(getSelf().getPort());
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                deleteSnmpAgentIptablesRule(getSelf().getPort());
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                SnmpAgentVO vo = ObjectUtils.newAndCopy(getSelf(), SnmpAgentVO.class);
                if (vo.getStatus() == SnmpAgentStatus.Disable) {
                    vo.setStatus(SnmpAgentStatus.Enable);
                    self = dbf.updateAndRefresh(vo);
                }
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(SnmpAgentInventory.valueOf(getSelf()));
                logger.info(String.format("Snmp agent success to listen on port %s", getSelf().getPort()));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                agentState = SnmpAgentState.STATE_STOPPED;
                instance = null;
                evtf.fire(SnmpAgentEvents.SNMP_AGENT_START_FAILED, null);
                completion.fail(errCode);
            }
        }).start();
    }

    private SnmpAgentVO refreshVO() {
        SnmpAgentVO vo = self;
        self = dbf.findByUuid(self.getUuid(), SnmpAgentVO.class);

        if (self == null) {
            throw new OperationFailureException(operr("snmp[uuid:%s] has not been created", vo.getUuid()));
        }

        return self;
    }

    private void init() throws IOException, DuplicateRegistrationException {
        agentState = SnmpAgentState.STATE_INIT_STARTED;
        localEngineID = MPv3.createLocalEngineID();
        agent = new CommandProcessor(new OctetString(localEngineID));
        initTransportMappings();
        initMessageDispatcher();
        initMOServer();
        snmpTargetMIB = new SnmpTargetMIB(dispatcher);
        snmpCommunityMIB = new SnmpCommunityMIB(snmpTargetMIB);
        vacmMIB = new VacmMIB(new MOServer[]{server});
        addViews(vacmMIB);
        registerNecessarySnmpMIBs();
        registerMOs();
    }

    private void registerMOs() {
        registerManagedObject(new MOScalar<>(new OID(org.snmp4j.mp.SnmpConstants.sysDescr),
                MOAccessImpl.ACCESS_READ_ONLY,
                sysDescr));

        registerMOTables();
    }

    private void initTransportMappings() throws IOException {
        transportMappings = new ArrayList<>(1);
        transportMappings.add(new DefaultUdpTransportMapping(new UdpAddress(getSelf().getPort()), true));
        logger.info(String.format("Snmp agent socket success to bind port %s", getSelf().getPort()));
    }

    @Override
    public void stop(ReturnValueCompletion<SnmpAgentInventory> completion) {
        refreshVO();
        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("stop-snmp-agent-%s", getSelf().getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "close-snmp-agent-session";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                closeSession();
                agentState = SnmpAgentState.STATE_STOPPED;
                instance = null;
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "update-snmp-agent-status-in-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SnmpAgentVO vo = ObjectUtils.newAndCopy(getSelf(), SnmpAgentVO.class);
                if (vo.getStatus() == SnmpAgentStatus.Enable) {
                    vo.setStatus(SnmpAgentStatus.Disable);
                    self = dbf.updateAndRefresh(vo);
                }
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-snmp-agent-iptables-rule";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                deleteSnmpAgentIptablesRule(getSelf().getPort());
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(SnmpAgentInventory.valueOf(getSelf()));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void update(ReturnValueCompletion<SnmpAgentInventory> completion) {
        SnmpAgentVO originalCopy = ObjectUtils.newAndCopy(getSelf(), SnmpAgentVO.class);
        refreshVO();
        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("update-snmp-agent-%s", getSelf().getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "update-snmp-security-info";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                clearReadCommunity();
                clearUsmUser();
                addCommunityAndUsmUser();
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "close-old-snmp-agent-session";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (originalCopy.getStatus() == SnmpAgentStatus.Disable) {
                    trigger.next();
                    return;
                }
                if (originalCopy.getPort() != getSelf().getPort()) {
                    closeSession();
                    deleteSnmpAgentIptablesRule(originalCopy.getPort());
                }
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "create-new-snmp-agent-session";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                try {
                    addNewTransportMapping(getSelf().getPort());
                    initSnmpSession();
                    session.listen();
                    trigger.next();
                } catch (IOException e) {
                    trigger.fail(operr("failed to change snmp agent port from %s to %s, duet to %s",
                            originalCopy.getPort(), getSelf().getPort(), e.getMessage()));
                }
            }
        }).then(new Flow() {
            String __name__ = "add-new-snmp-agent-iptables-rule";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                addSnmpAgentIptablesRule(getSelf().getPort());
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                deleteSnmpAgentIptablesRule(getSelf().getPort());
                trigger.rollback();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(SnmpAgentInventory.valueOf(getSelf()));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                agentState = SnmpAgentState.STATE_STOPPED;
                SnmpAgentVO vo = ObjectUtils.newAndCopy(getSelf(), SnmpAgentVO.class);
                vo.setStatus(SnmpAgentStatus.Disable);
                self = dbf.updateAndRefresh(vo);
                instance = null;
                evtf.fire(SnmpAgentEvents.SNMP_AGENT_START_FAILED, null);
                completion.fail(errCode);
            }
        }).start();
    }

    protected void addCommunityAndUsmUser() {
        if (self.getVersion() == SnmpAgentVersion.v2c
                && StringUtils.isNotBlank(self.getReadCommunity())) {
            addReadCommunity(self.getReadCommunity());
            return;
        }
        if (self.getVersion() == SnmpAgentVersion.v3) {
            if (self.getSecurityLevel() == SecurityLevel.authPriv) {
                addUsmUsr(self.getUserName(),
                        self.getAuthAlgorithm(),
                        self.getAuthPassword(),
                        self.getPrivacyAlgorithm(),
                        self.getPrivacyPassword());
            } else if (self.getSecurityLevel() == SecurityLevel.authNoPriv) {
                addUsmUsr(self.getUserName(),
                        self.getAuthAlgorithm(),
                        self.getAuthPassword(),
                        null, null);
            } else if (self.getSecurityLevel() == SecurityLevel.noAuthNoPriv) {
                addUsmUsr(self.getUserName(),
                        null, null,
                        null, null);
            }
        }
    }

    private void registerMOTables() {
        for (MOTableFactory moTableFactory : pluginRgty.getExtensionList(MOTableFactory.class)) {
            registerManagedObject(moTableFactory.createMOTable());
        }
    }

    public void addUsmUsr(String userName, SnmpAgentAuthAlgorithm authAlgorithm,
                          String authPassword, SnmpAgentPrivacyAlgorithm privacyAlgorithm,
                          String privacyPassword) {
        UsmUser user = new UsmUser(
                new OctetString(userName),
                authAlgorithm == null ? null : authAlgorithm.oid,
                authPassword == null ? null : new OctetString(authPassword),
                privacyAlgorithm == null ? null : privacyAlgorithm.getOid(),
                privacyPassword == null ? null : new OctetString(privacyPassword));
        super.addUsmUser(user);
    }

    private void closeSession() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (IOException e) {
            throw new OperationFailureException(operr("failed to close snmp agent session[%s] on port %s, due to %s",
                    getSelf().getUuid(), getSelf().getPort(), e.getMessage()));
        } finally {
            removeTransportMappingInSession();
            session = null;
        }
    }

    public SnmpAgentVO getSelf() {
        return self;
    }

    private void deleteSnmpAgentIptablesRule(int port) {
        IptablesUtils.deleteRuleFromFilterTable(String.format("-A INPUT -p udp -m comment --comment \"snmp.agent.port\" -m state --state NEW -m udp --dport %s -j ACCEPT", port));
    }

    private void addSnmpAgentIptablesRule(int port) {
        IptablesUtils.insertRuleToFilterTable(String.format("-A INPUT -p udp -m comment --comment \"snmp.agent.port\" -m state --state NEW -m udp --dport %s -j ACCEPT", port));
    }

    private void addNewTransportMapping(int port) {
        try {
            transportMappings.add(new DefaultUdpTransportMapping(new UdpAddress(getSelf().getPort()), true));
            logger.info(String.format("SNMP agent socket success to bind port %s", getSelf().getPort()));
        } catch (IOException e) {
            String errMsg = String.format("failed to bond socket on port %s, due to %s", port, e.getMessage());
            throw new OperationFailureException(operr(errMsg));
        }
    }

    private void removeTransportMappingInSession() {
        if (session == null) {
            return;
        }
        for (TransportMapping<? extends Address> transportMapping : transportMappings) {
            session.removeTransportMapping(transportMapping);
        }
        transportMappings.clear();
    }
}
