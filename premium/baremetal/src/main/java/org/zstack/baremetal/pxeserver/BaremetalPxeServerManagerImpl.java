package org.zstack.baremetal.pxeserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.*;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.baremetal.pxeserver.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.tag.TagManager;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.err;

/**
 * Created by GuoYi on 2017/3/25.
 */
public class BaremetalPxeServerManagerImpl extends AbstractService implements BaremetalPxeServerManager, ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener, Component {
    private static final CLogger logger = Utils.getLogger(BaremetalPxeServerManagerImpl.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected TagManager tagMgr;
    @Autowired
    protected AnsibleFacade asf;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected EventFacade evf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    protected BaremetalPxeServerExtensionPointEmitter extpEmitter;

    private static final int BAREMETAL_PXESERVER_SYNC_LEVEL = 10;

    @Override
    public boolean start() {
        setupEvents();
        deployAnsible();
        startProxyOnManagementNode();
        installValidatorToGlobalConfig();
        return true;
    }

    private List<String> getBaremetalPxeServerManagedByUs() {
        return new SQLBatchWithReturn<List<String>>() {
            @Override
            protected List<String> scripts() {
                List<String> uuids = new ArrayList<>();

                long count = q(BaremetalPxeServerVO.class).count();
                sql("select pxe from BaremetalPxeServerVO pxe", BaremetalPxeServerVO.class)
                        .limit(1000)
                        .paginate(count, pxes -> pxes.forEach(pxe -> {
                            BaremetalPxeServerVO vo = (BaremetalPxeServerVO) pxe;
                            if (!destinationMaker.isManagedByUs(vo.getUuid())) {
                                return;
                            }
                            uuids.add(vo.getUuid());
                        }));
                return uuids;
            }
        }.execute();
    }

    @AsyncThread
    private void loadBaremetalPxeServer() {
        List<String> uuids = getBaremetalPxeServerManagedByUs();
        if (uuids.isEmpty()) {
            return;
        }

        List<ConnectBaremetalPxeServerMsg> cmsgs = new ArrayList<>();
        for (String uuid : uuids) {
            ConnectBaremetalPxeServerMsg cmsg = new ConnectBaremetalPxeServerMsg();
            cmsg.setUuid(uuid);
            cmsg.setNewAdd(true);
            bus.makeTargetServiceIdByResourceUuid(cmsg, BaremetalPxeServerConstant.SERVICE_ID, uuid);
            cmsgs.add(cmsg);
        }

        bus.send(cmsgs);
    }

    @AsyncThread
    private static void startProxyOnManagementNode() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        if (!Q.New(BaremetalPxeServerVO.class).isExists()) {
            return;
        }

        int ret = ShellUtils.runAndReturn("systemctl start nginx && sudo systemctl reload nginx").getRetCode();
        if (ret == 0) {
            logger.info(String.format("successfully started nginx proxy on mn[uuid:%s]", Platform.getManagementServerId()));
        } else {
            logger.error(String.format("failed to start nginx proxy on mn[uuid:%s]", Platform.getManagementServerId()));
        }
    }

    private void startProxyAfterCreatePxeServer() {
        startProxyOnManagementNode();
        evf.fire(BaremetalPxeServerCanonicalEvents.CREATE_BAREMETAL_PXE_SERVER, null);
    }

    private void onBaremetalPxeServerCreatedEvent() {
        evf.on(BaremetalPxeServerCanonicalEvents.CREATE_BAREMETAL_PXE_SERVER, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                if (evf.isFromThisManagementNode(tokens)) {
                    return;
                }
                startProxyOnManagementNode();
            }
        });
    }

    @AsyncThread
    private static void stopProxyOnManagementNode() {
        // no need to stop nginx any more

        /*
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        int ret = ShellUtils.runAndReturn("systemctl stop nginx").getRetCode();
        if (ret == 0) {
            logger.info(String.format("successfully stopped nginx proxy on mn[uuid:%s]", Platform.getManagementServerId()));
        } else {
            logger.error(String.format("failed to stop nginx proxy on mn[uuid:%s]", Platform.getManagementServerId()));
        }
         */
    }

    private void stopProxyAfterDeletePxeServer(String uuid) {
        if (Q.New(BaremetalPxeServerVO.class).isExists()) {
            return;
        }

        stopProxyOnManagementNode();
        evf.fire(BaremetalPxeServerCanonicalEvents.DELETE_BAREMETAL_PXE_SERVER, uuid);
    }

    private void onBaremetalPxeServerDeletedEvent() {
        evf.on(BaremetalPxeServerCanonicalEvents.DELETE_BAREMETAL_PXE_SERVER, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                if (evf.isFromThisManagementNode(tokens)) {
                    return;
                }
                stopProxyOnManagementNode();
            }
        });
    }

    private void setupEvents() {
        onBaremetalPxeServerCreatedEvent();
        onBaremetalPxeServerDeletedEvent();
    }

    @AsyncThread
    private void deployAnsible() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        asf.deployModule(BaremetalPxeServerConstant.ANSIBLE_MODULE_PATH, BaremetalPxeServerConstant.ANSIBLE_PLAYBOOK_NAME);
    }

    private void installValidatorToGlobalConfig() {
        BaremetalPxeServerGlobalConfig.RESERVED_CAPACITY.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (!SizeUtils.isSizeString(newValue)) {
                    throw new GlobalConfigException(String.format("%s is not a size string; a size string consists of a number ending with suffix B/K/M/G/T or without suffix; for example, 512M, 1G", newValue));
                }
            }
        });
    }

    @Override
    public boolean stop() {
        stopProxyOnManagementNode();
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(BaremetalPxeServerConstant.SERVICE_ID);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateBaremetalPxeServerMsg) {
            handle((APICreateBaremetalPxeServerMsg) msg);
        } else if (msg instanceof APIUpdateBaremetalPxeServerMsg) {
            handle((APIUpdateBaremetalPxeServerMsg) msg);
        } else if (msg instanceof APIDeleteBaremetalPxeServerMsg) {
            handle((APIDeleteBaremetalPxeServerMsg) msg);
        } else if (msg instanceof APIStartBaremetalPxeServerMsg) {
            handle((APIStartBaremetalPxeServerMsg) msg);
        } else if (msg instanceof APIStopBaremetalPxeServerMsg) {
            handle((APIStopBaremetalPxeServerMsg) msg);
        } else if (msg instanceof APIReconnectBaremetalPxeServerMsg) {
            handle((APIReconnectBaremetalPxeServerMsg) msg);
        } else if (msg instanceof APIAttachBaremetalPxeServerToClusterMsg) {
            handle((APIAttachBaremetalPxeServerToClusterMsg) msg);
        } else if (msg instanceof APIDetachBaremetalPxeServerFromClusterMsg) {
            handle((APIDetachBaremetalPxeServerFromClusterMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof BaremetalPxeServerMessage) {
            passThrough((BaremetalPxeServerMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(BaremetalPxeServerMessage msg) {
        BaremetalPxeServerVO pxe = dbf.findByUuid(msg.getPxeServerUuid(), BaremetalPxeServerVO.class);
        if (pxe == null) {
            throw new CloudRuntimeException(String.format(
                    "cannot find baremetal pxeserver[uuid:%s], it may have beed deleted.",
                    msg.getPxeServerUuid()
            ));
        }

        new BaremetalPxeServerBase(pxe).handleMessage((Message) msg);
    }

    private void handle(final APICreateBaremetalPxeServerMsg msg) {
        final APICreateBaremetalPxeServerEvent evt = new APICreateBaremetalPxeServerEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("create-baremetal-pxeserver-%s", msg.getHostname());
            }

            @Override
            public void run(SyncTaskChain chain) {
                BaremetalPxeServerVO vo = new BaremetalPxeServerVO();
                if (msg.getResourceUuid() != null) {
                    vo.setUuid(msg.getResourceUuid());
                } else {
                    vo.setUuid(Platform.getUuid());
                }
                vo.setZoneUuid(msg.getZoneUuid());
                vo.setName(msg.getName());
                vo.setDescription(msg.getDescription());
                vo.setHostname(msg.getHostname());
                vo.setSshUsername(msg.getSshUsername());
                vo.setSshPassword(msg.getSshPassword());
                vo.setSshPort(msg.getSshPort());
                vo.setStoragePath(msg.getStoragePath());
                vo.setDhcpInterface(msg.getDhcpInterface());
                vo.setDhcpInterfaceAddress(msg.getDhcpInterfaceAddress());
                vo.setDhcpRangeBegin(msg.getDhcpRangeBegin());
                vo.setDhcpRangeEnd(msg.getDhcpRangeEnd());
                vo.setDhcpRangeNetmask(msg.getDhcpRangeNetmask());
                vo.setState(BaremetalPxeServerState.Enabled);
                vo.setStatus(BaremetalPxeServerStatus.Connecting);
                dbf.persistAndRefresh(vo);

                // create system tags
                tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), BaremetalPxeServerVO.class.getSimpleName());

                // connect new pxeserver
                FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
                fchain.setName("connect-new-baremetal-pxeserver");
                fchain.then(new Flow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ConnectBaremetalPxeServerMsg cmsg = new ConnectBaremetalPxeServerMsg();
                        cmsg.setUuid(vo.getUuid());
                        cmsg.setNewAdd(true);
                        bus.makeTargetServiceIdByResourceUuid(cmsg, BaremetalPxeServerConstant.SERVICE_ID, vo.getUuid());
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    BaremetalPxeServerInventory inv = BaremetalPxeServerInventory.valueOf(dbf.reload(vo));
                                    data.put("pxeInv", inv);
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        DeleteBaremetalPxeServerMsg dmsg = new DeleteBaremetalPxeServerMsg();
                        dmsg.setUuid(vo.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, vo.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                fchain.done(new FlowDoneHandler(evt) {
                    @Override
                    public void handle(Map data) {
                        startProxyAfterCreatePxeServer();
                        logger.debug("successfully created baremetal pxeserver " + msg.getHostname());
                        evt.setInventory((BaremetalPxeServerInventory) data.get("pxeInv"));
                        bus.publish(evt);
                        chain.next();
                    }
                }).error(new FlowErrorHandler(evt) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.error("failed to create baremetal pxeserver " + msg.getHostname());
                        evt.setError(errCode);
                        bus.publish(evt);
                        chain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APIUpdateBaremetalPxeServerMsg msg) {
        final APIUpdateBaremetalPxeServerEvent evt = new APIUpdateBaremetalPxeServerEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "update-baremetal-pxeserver-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                boolean newAdd = false;
                BaremetalPxeServerVO pxeServer = dbf.findByUuid(msg.getUuid(), BaremetalPxeServerVO.class);
                if (msg.getName() != null) {
                    pxeServer.setName(msg.getName());
                }

                if (msg.getDescription() != null) {
                    pxeServer.setDescription(msg.getDescription());
                }

                if (msg.getDhcpRangeBegin() != null) {
                    pxeServer.setDhcpRangeBegin(msg.getDhcpRangeBegin());
                    newAdd = true;
                }

                if (msg.getDhcpRangeEnd() != null) {
                    pxeServer.setDhcpRangeEnd(msg.getDhcpRangeEnd());
                    newAdd = true;
                }

                if (msg.getDhcpRangeNetmask() != null) {
                    pxeServer.setDhcpRangeNetmask(msg.getDhcpRangeNetmask());
                    newAdd = true;
                }
                pxeServer = dbf.updateAndRefresh(pxeServer);

                // re-init pxeserver dnsmasq configs if newAdd
                if (!newAdd) {
                    BaremetalPxeServerInventory inv = BaremetalPxeServerInventory.valueOf(pxeServer);
                    evt.setInventory(inv);
                    bus.publish(evt);
                    chain.next();
                    return;
                }

                ConnectBaremetalPxeServerMsg cmsg = new ConnectBaremetalPxeServerMsg();
                cmsg.setUuid(msg.getUuid());
                cmsg.setNewAdd(true);
                bus.makeTargetServiceIdByResourceUuid(cmsg, BaremetalPxeServerConstant.SERVICE_ID, msg.getUuid());
                bus.send(cmsg, new CloudBusCallBack(msg) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            BaremetalPxeServerInventory inv = BaremetalPxeServerInventory.valueOf(
                                    dbf.findByUuid(msg.getUuid(), BaremetalPxeServerVO.class)
                            );
                            evt.setInventory(inv);
                        } else {
                            evt.setError(reply.getError());
                        }
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APIDeleteBaremetalPxeServerMsg msg) {
        final APIDeleteBaremetalPxeServerEvent evt = new APIDeleteBaremetalPxeServerEvent(msg.getId());

        final String issuer = BaremetalPxeServerVO.class.getSimpleName();
        final BaremetalPxeServerVO pxe = dbf.findByUuid(msg.getUuid(), BaremetalPxeServerVO.class);
        final List<BaremetalPxeServerInventory> ctx = Collections.singletonList(BaremetalPxeServerInventory.valueOf(pxe));

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-baremetal-pxeserver-%s", msg.getUuid()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        chain.done(new FlowDoneHandler(evt) {
            @Override
            public void handle(Map data) {
                stopProxyAfterDeletePxeServer(pxe.getUuid());
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(evt) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errCode.getDetails()).withCause(errCode));
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(APIStartBaremetalPxeServerMsg msg) {
        final APIStartBaremetalPxeServerEvent evt = new APIStartBaremetalPxeServerEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return BaremetalPxeServerConstant.SYNC_SIGNATURE_OF_BAREMETAL_PXESERVER + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                StartBaremetalPxeServerMsg smsg = new StartBaremetalPxeServerMsg();
                smsg.setUuid(msg.getUuid());
                bus.makeTargetServiceIdByResourceUuid(smsg, BaremetalPxeServerConstant.SERVICE_ID, msg.getUuid());
                bus.send(smsg, new CloudBusCallBack(evt) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            StartBaremetalPxeServerReply rly = reply.castReply();
                            evt.setInventory(rly.getInventory());
                        } else {
                            evt.setError(reply.getError());
                        }
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public int getSyncLevel() {
                return BAREMETAL_PXESERVER_SYNC_LEVEL;
            }
        });
    }

    private void handle(APIStopBaremetalPxeServerMsg msg) {
        final APIStopBaremetalPxeServerEvent evt = new APIStopBaremetalPxeServerEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return BaremetalPxeServerConstant.SYNC_SIGNATURE_OF_BAREMETAL_PXESERVER + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                StopBaremetalPxeServerMsg smsg = new StopBaremetalPxeServerMsg();
                smsg.setUuid(msg.getUuid());
                bus.makeTargetServiceIdByResourceUuid(smsg, BaremetalPxeServerConstant.SERVICE_ID, msg.getUuid());
                bus.send(smsg, new CloudBusCallBack(evt) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            StopBaremetalPxeServerReply rly = reply.castReply();
                            evt.setInventory(rly.getInventory());
                        } else {
                            evt.setError(reply.getError());
                        }
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public int getSyncLevel() {
                return BAREMETAL_PXESERVER_SYNC_LEVEL;
            }
        });
    }

    private void handle(APIReconnectBaremetalPxeServerMsg msg) {
        APIReconnectBaremetalPxeServerEvent evt = new APIReconnectBaremetalPxeServerEvent(msg.getId());
        ConnectBaremetalPxeServerMsg cmsg = new ConnectBaremetalPxeServerMsg();
        cmsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, BaremetalPxeServerConstant.SERVICE_ID, msg.getUuid());
        bus.send(cmsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setError(reply.getError());
                } else {
                    ConnectBaremetalPxeServerReply rly = reply.castReply();
                    evt.setInventory(rly.getInventory());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIAttachBaremetalPxeServerToClusterMsg msg) {
        APIAttachBaremetalPxeServerToClusterEvent evt = new APIAttachBaremetalPxeServerToClusterEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(evt) {
            @Override
            public String getSyncSignature() {
                return String.format("attach-baremetal-pxeserver-to-cluster-%s", msg.getClusterUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                BaremetalPxeServerClusterRefVO ref = new BaremetalPxeServerClusterRefVO();
                ref.setPxeServerUuid(msg.getPxeServerUuid());
                ref.setClusterUuid(msg.getClusterUuid());
                dbf.persist(ref);

                // attach extension points
                BaremetalPxeServerVO pxe = dbf.findByUuid(msg.getPxeServerUuid(), BaremetalPxeServerVO.class);
                extpEmitter.afterAttach(pxe, msg.getClusterUuid());
                evt.setInventory(BaremetalPxeServerInventory.valueOf(pxe));
                bus.publish(evt);
                logger.info(String.format("successfully attached pxeserver[uuid:%s] to baremetal cluster[uuid:%s]",
                        pxe.getUuid(), msg.getClusterUuid()
                ));

                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APIDetachBaremetalPxeServerFromClusterMsg msg) {
        APIDetachBaremetalPxeServerFromClusterEvent evt = new APIDetachBaremetalPxeServerFromClusterEvent(msg.getId());
        DetachBaremetalPxeServerFromClusterMsg dmsg = new DetachBaremetalPxeServerFromClusterMsg();
        dmsg.setPxeServerUuid(msg.getPxeServerUuid());
        dmsg.setClusterUuid(msg.getClusterUuid());
        bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, msg.getPxeServerUuid());
        bus.send(dmsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    DetachBaremetalPxeServerFromClusterReply rly = reply.castReply();
                    evt.setInventory(rly.getInventory());
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {

    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        logger.debug(String.format("management node[uuid:%s] left, node[uuid:%s] starts to taking over baremetal pxeservers ...",
                inv.getUuid(), Platform.getManagementServerId()));
        loadBaremetalPxeServer();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {

    }

    @Override
    public void managementNodeReady() {
        logger.debug(String.format("management node[uuid:%s] joins and starts to load baremetal pxeservers ...",
                Platform.getManagementServerId()));
        loadBaremetalPxeServer();
    }
}