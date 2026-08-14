package org.zstack.baremetal.pxeserver;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.baremetal.instance.BaremetalInstanceSystemTags;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.ansible.SshYumRepoChecker;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.baremetal.instance.*;
import org.zstack.header.baremetal.network.*;
import org.zstack.header.baremetal.preconfiguration.CustomPreconfigurationVO;
import org.zstack.header.baremetal.preconfiguration.CustomPreconfigurationVO_;
import org.zstack.header.baremetal.preconfiguration.PreconfigurationTemplateVO;
import org.zstack.header.baremetal.pxeserver.*;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerCommands.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkDnsVO;
import org.zstack.header.network.l3.L3NetworkDnsVO_;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.storage.backup.imagestore.*;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.ssh.Ssh;
import org.zstack.utils.zsha2.ZSha2Helper;
import org.zstack.utils.ssh.SshException;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created by GuoYi on 2018-10-12.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BaremetalPxeServerBase implements BaremetalPxeServer {
    protected static final CLogger logger = Utils.getLogger(BaremetalPxeServerBase.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected EventFacade evf;
    @Autowired
    protected BaremetalPxeServerPingTracker tracker;
    @Autowired
    protected BaremetalPxeServerExtensionPointEmitter extpEmitter;

    protected BaremetalPxeServerVO self;
    BaremetalPxeServerBase(BaremetalPxeServerVO self) {
        this.self = self;
    }
    private String pxeAgentPackageName = BaremetalPxeServerGlobalProperty.AGENT_PACKAGE_NAME;
    private String zstAgentPackageName = ImageStoreBackupStorageGlobalProperty.AGENT_PACKAGE_NAME;
    private String zstClientAgentPackageName = ImageStoreBackupStorageGlobalProperty.AGENT_CLIENT_PACKAGE_NAME;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof ConnectBaremetalPxeServerMsg) {
            handle((ConnectBaremetalPxeServerMsg) msg);
        } else if (msg instanceof InitBaremetalPxeServerConfigsMsg) {
            handle((InitBaremetalPxeServerConfigsMsg) msg);
        } else if (msg instanceof CreateBaremetalInstanceConfigsMsg) {
            handle((CreateBaremetalInstanceConfigsMsg) msg);
        } else if (msg instanceof DeleteBaremetalInstanceConfigsMsg) {
            handle((DeleteBaremetalInstanceConfigsMsg) msg);
        } else if (msg instanceof CreateBaremetalNoVNCProxyMsg) {
            handle((CreateBaremetalNoVNCProxyMsg) msg);
        } else if (msg instanceof DeleteBaremetalNoVNCProxyMsg) {
            handle((DeleteBaremetalNoVNCProxyMsg) msg);
        } else if (msg instanceof CreateBaremetalTerminalProxyMsg) {
            handle((CreateBaremetalTerminalProxyMsg) msg);
        } else if (msg instanceof DeleteBaremetalTerminalProxyMsg) {
            handle((DeleteBaremetalTerminalProxyMsg) msg);
        } else if (msg instanceof StartBaremetalPxeServerMsg) {
            handle((StartBaremetalPxeServerMsg) msg);
        } else if (msg instanceof StopBaremetalPxeServerMsg) {
            handle((StopBaremetalPxeServerMsg) msg);
        } else if (msg instanceof DeleteBaremetalPxeServerMsg) {
            handle((DeleteBaremetalPxeServerMsg) msg);
        } else if (msg instanceof DownloadBaremetalImageCacheMsg) {
            handle((DownloadBaremetalImageCacheMsg) msg);
        } else if (msg instanceof DeleteBaremetalImageCacheMsg) {
            handle((DeleteBaremetalImageCacheMsg) msg);
        } else if (msg instanceof PingBaremetalPxeServerMsg) {
            handle((PingBaremetalPxeServerMsg) msg);
        } else if (msg instanceof CreateBaremetalDhcpConfigMsg) {
            handle((CreateBaremetalDhcpConfigMsg) msg);
        } else if (msg instanceof DeleteBaremetalDhcpConfigMsg) {
            handle((DeleteBaremetalDhcpConfigMsg) msg);
        } else if (msg instanceof DetachBaremetalPxeServerFromClusterMsg) {
            handle((DetachBaremetalPxeServerFromClusterMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(ConnectBaremetalPxeServerMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("connect-baremetal-pxeserver-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                final ConnectBaremetalPxeServerReply reply = new ConnectBaremetalPxeServerReply();
                final boolean needReInit = !CoreGlobalProperty.UNIT_TEST_ON && getPxeServerMd5Checker().needDeploy();

                final FlowChain flowChain = FlowChainBuilder.newShareFlowChain();
                flowChain.setName(String.format("connect-baremetal-pxeserver-%s", self.getUuid()));
                flowChain.then(new ShareFlow() {
                    @Override
                    public void setup() {
                        flow(new NoRollbackFlow() {
                            String __name__ = "connect-baremetal-pxeserver";

                            @Override
                            public void run(final FlowTrigger trigger, Map data) {
                                changeConnectionState(BaremetalPxeServerStatusEvent.connecting);
                                connectHook(msg.isNewAdd(), new Completion(trigger) {
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


                        flow(new NoRollbackFlow() {
                            String __name__ = "init-baremetal-pxeserver";

                            @Override
                            public boolean skip(Map data) {
                                return !msg.isNewAdd() && !needReInit;
                            }

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                InitBaremetalPxeServerConfigsMsg imsg = new InitBaremetalPxeServerConfigsMsg();
                                imsg.setPxeServerUuid(self.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(imsg, BaremetalPxeServerConstant.SERVICE_ID, self.getUuid());
                                bus.send(imsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (reply.isSuccess()) {
                                            trigger.next();
                                        } else {
                                            trigger.fail(reply.getError());
                                        }
                                    }
                                });
                            }
                        });


                        flow(new NoRollbackFlow() {
                            String __name__ = "ping-baremetal-pxeserver";

                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                PingBaremetalPxeServerMsg pmsg = new PingBaremetalPxeServerMsg();
                                pmsg.setUuid(self.getUuid());
                                bus.makeTargetServiceIdByResourceUuid(pmsg, BaremetalPxeServerConstant.SERVICE_ID, self.getUuid());
                                bus.send(pmsg, new CloudBusCallBack(trigger) {
                                    @Override
                                    public void run(MessageReply reply) {
                                        if (reply.isSuccess()) {
                                            trigger.next();
                                        } else {
                                            trigger.fail(reply.getError());
                                        }
                                    }
                                });
                            }
                        });

                        done(new FlowDoneHandler(msg) {
                            @Override
                            public void handle(Map data) {
                                changeConnectionState(BaremetalPxeServerStatusEvent.connected);
                                reply.setInventory(BaremetalPxeServerInventory.valueOf(self));
                                tracker.track(self.getUuid());
                                bus.reply(msg, reply);
                                chain.next();
                            }
                        });

                        error(new FlowErrorHandler(msg) {
                            @Override
                            public void handle(ErrorCode errCode, Map data) {
                                changeConnectionState(BaremetalPxeServerStatusEvent.disconnected);
                                if (!msg.isNewAdd()) {
                                    tracker.track(self.getUuid());
                                }

                                reply.setError(errCode);
                                bus.reply(msg, reply);
                                chain.next();
                            }
                        });
                    }
                }).start();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(InitBaremetalPxeServerConfigsMsg msg) {
        final InitBaremetalPxeServerConfigsReply reply = new InitBaremetalPxeServerConfigsReply();

        InitCmd cmd = new InitCmd();
        cmd.uuid = self.getUuid();
        cmd.managementIp = ZSha2Helper.isMNHaEnvironment() ?
                ZSha2Helper.getInfo().getDbvip() :
                Platform.getManagementServerIp();
        cmd.managementPort = String.valueOf(Platform.getManagementNodeServicePort());
        cmd.storagePath = self.getStoragePath();
        cmd.dhcpInterface = self.getDhcpInterface();
        cmd.dhcpRangeBegin = self.getDhcpRangeBegin();
        cmd.dhcpRangeEnd = self.getDhcpRangeEnd();
        cmd.dhcpRangeNetmask = self.getDhcpRangeNetmask();

        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.INIT_PATH), cmd, new JsonAsyncRESTCallback<InitRsp>(reply) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(InitRsp ret) {
                if (ret.success) {
                    logger.info(String.format("successfully init configs on baremetal pxeserver[uuid:%s]", self.getUuid()));
                } else {
                    reply.setError(operr("failed to init configs on baremetal pxeserver[uuid:%s]", self.getUuid()));
                }
                bus.reply(msg, reply);
            }

            @Override
            public Class<InitRsp> getReturnClass() {
                return InitRsp.class;
            }
        });
    }

    private void handle(CreateBaremetalInstanceConfigsMsg msg) {
        final CreateBaremetalInstanceConfigsReply reply = new CreateBaremetalInstanceConfigsReply();

        CreateBmConfigsCmd cmd = new CreateBmConfigsCmd();
        cmd.uuid = self.getUuid();
        cmd.dhcpInterface = self.getDhcpInterface();
        cmd.imageUuid = msg.getImageUuid();
        cmd.bmUuid = msg.getBmUuid();
        cmd.pxeNicMac = msg.getPxeNicMac();
        cmd.username = msg.getUsername();
        cmd.password = msg.getPassword();

        List<BaremetalNicCfg> nicCfgs = new SQLBatchWithReturn<List<BaremetalNicCfg>>() {
            @Override
            protected List<BaremetalNicCfg> scripts() {
                List<BaremetalNicCfg> nicCfgs = new ArrayList<>();

                List<BaremetalNicVO> nics = q(BaremetalNicVO.class)
                        .eq(BaremetalNicVO_.baremetalInstanceUuid, msg.getBmUuid())
                        .list();
                if (nics.isEmpty()) {
                    return nicCfgs;
                }

                List<String> l3Uuids = nics.stream().map(BaremetalNicVO::getL3NetworkUuid).collect(Collectors.toList());
                List<String> bondingUuids = nics.stream().map(BaremetalNicVO::getBaremetalBondingUuid).collect(Collectors.toList());

                List<BaremetalBondingVO> bondings = new ArrayList<>();
                if (!bondingUuids.isEmpty()) {
                    bondings = q(BaremetalBondingVO.class)
                            .in(BaremetalBondingVO_.uuid, bondingUuids)
                            .list();
                }

                List<Tuple> vlans = new ArrayList<>();
                List<Tuple> dnses = new ArrayList<>();
                if (!l3Uuids.isEmpty()) {
                    vlans = q(BaremetalVlanNicVO.class)
                            .select(BaremetalVlanNicVO_.l3NetworkUuid, BaremetalVlanNicVO_.vlan)
                            .in(BaremetalVlanNicVO_.l3NetworkUuid, l3Uuids)
                            .listTuple();
                    dnses = q(L3NetworkDnsVO.class)
                            .select(L3NetworkDnsVO_.l3NetworkUuid, L3NetworkDnsVO_.dns)
                            .in(L3NetworkDnsVO_.l3NetworkUuid, l3Uuids)
                            .limit(1)
                            .listTuple();
                }

                for (BaremetalNicVO nic : nics) {
                    BaremetalNicCfg cfg = new BaremetalNicCfg();
                    cfg.pxe = nic.getPxe();
                    cfg.mac = nic.getMac();
                    cfg.ip = nic.getIp();
                    cfg.netmask = nic.getNetmask();
                    cfg.gateway = nic.getGateway();

                    cfg.nameserver = "223.5.5.5";
                    for (Tuple dns : dnses) {
                        if (dns.get(0, String.class).equals(nic.getL3NetworkUuid())) {
                            cfg.nameserver = dns.get(1, String.class);
                            break;
                        }
                    }

                    for (Tuple vlan : vlans) {
                        if (vlan.get(0, String.class).equals(nic.getL3NetworkUuid())) {
                            cfg.vlanid = vlan.get(1, Integer.class);
                            break;
                        }
                    }

                    for (BaremetalBondingVO bonding : bondings) {
                        if (bonding.getUuid().equals(nic.getBaremetalBondingUuid())) {
                            cfg.bondName = bonding.getName();
                            cfg.bondMode = bonding.getMode();
                            cfg.bondSlaves = bonding.getSlaves();
                            cfg.bondOpts = bonding.getOpts();
                            break;
                        }
                    }

                    nicCfgs.add(cfg);
                }

                return nicCfgs;
            }
        }.execute();
        cmd.nicCfgs = JSONObjectUtil.toJsonString(nicCfgs);

        List<String> tags = BaremetalInstanceSystemTags.FORCE_INSTALL.getTags(cmd.bmUuid, BaremetalInstanceVO.class);
        if (tags != null && tags.contains(BaremetalInstanceSystemTags.FORCE_INSTALL_TOKEN)) {
            cmd.forceInstall = true;
        }

        if (msg.getTemplateUuid() != null) {
            PreconfigurationTemplateVO tmpl = dbf.findByUuid(msg.getTemplateUuid(), PreconfigurationTemplateVO.class);
            cmd.preconfigurationType = tmpl.getType();
            cmd.preconfigurationContent = tmpl.getContent();
            cmd.preconfigurationMd5sum = tmpl.getMd5sum();
        }

        List<Tuple> params = Q.New(CustomPreconfigurationVO.class)
                .select(CustomPreconfigurationVO_.param, CustomPreconfigurationVO_.value)
                .eq(CustomPreconfigurationVO_.baremetalInstanceUuid, msg.getBmUuid())
                .listTuple();
        if (!params.isEmpty()) {
            Map<String, String> paramMap = new HashMap<>();
            for (Tuple t : params) {
                String param = t.get(0, String.class);
                String value = t.get(1, String.class);
                paramMap.put(param, value);
            }
            cmd.customPreconfigurations = JSONObjectUtil.toJsonString(paramMap);
        }

        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.CREATE_BM_CONFIGS_PATH), cmd, new JsonAsyncRESTCallback<CreateBmConfigsRsp>(reply) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CreateBmConfigsRsp ret) {
                if (!ret.success) {
                    reply.setError(operr("failed to create bm instance configs on baremetal pxeserver[uuid:%s]", self.getUuid()));
                } else {
                    logger.info(String.format("successfully created bm instance configs on baremetal pxeserver[uuid:%s]", self.getUuid()));
                }
                bus.reply(msg,reply);
            }

            @Override
            public Class<CreateBmConfigsRsp> getReturnClass() {
                return CreateBmConfigsRsp.class;
            }
        });
    }

    private void handle(DeleteBaremetalInstanceConfigsMsg msg) {
        final DeleteBaremetalInstanceConfigsReply reply = new DeleteBaremetalInstanceConfigsReply();

        DeleteBmConfigsCmd cmd = new DeleteBmConfigsCmd();
        cmd.uuid = self.getUuid();
        cmd.pxeNicMac = msg.getPxeNicMac();

        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.DELETE_BM_CONFIGS_PATH), cmd, new JsonAsyncRESTCallback<DeleteBmConfigsRsp>(reply) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteBmConfigsRsp ret) {
                if (!ret.success) {
                    reply.setError(operr("failed to delete bm instance configs on baremetal pxeserver[uuid:%s]", self.getUuid()));
                } else {
                    logger.info(String.format("successfully deleted bm instance configs on baremetal pxeserver[uuid:%s]", self.getUuid()));
                }
                bus.reply(msg,reply);
            }

            @Override
            public Class<DeleteBmConfigsRsp> getReturnClass() {
                return DeleteBmConfigsRsp.class;
            }
        });
    }

    private void handle(CreateBaremetalNoVNCProxyMsg msg) {
        CreateBaremetalNoVNCProxyReply reply = new CreateBaremetalNoVNCProxyReply();

        CreateBmNoVNCProxyCmd cmd = new CreateBmNoVNCProxyCmd();
        cmd.uuid = self.getUuid();
        cmd.bmUuid = msg.getBaremetalInstanceUuid();
        cmd.upstream = msg.getUpstream();
        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.CREATE_BM_NOVNC_PROXY_PATH), cmd, new JsonAsyncRESTCallback<CreateBmNoVNCProxyRsp>(reply) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CreateBmNoVNCProxyRsp ret) {
                if (!ret.success) {
                    reply.setError(operr("failed to create bm instance novnc proxy on baremetal pxeserver[uuid:%s]", self.getUuid()));
                } else {
                    evf.fire(BaremetalInstanceCanonicalEvents.CREATE_NOVNC_NGINX_PROXY, msg.getBaremetalInstanceUuid());
                    logger.info(String.format("successfully created bm instance novnc proxy on baremetal pxeserver[uuid:%s]", self.getUuid()));
                }
                bus.reply(msg,reply);
            }

            @Override
            public Class<CreateBmNoVNCProxyRsp> getReturnClass() {
                return CreateBmNoVNCProxyRsp.class;
            }
        });
    }

    private void handle(DeleteBaremetalNoVNCProxyMsg msg) {
        DeleteBaremetalNoVNCProxyReply reply = new DeleteBaremetalNoVNCProxyReply();

        DeleteBmNoVNCProxyCmd cmd = new DeleteBmNoVNCProxyCmd();
        cmd.uuid = self.getUuid();
        cmd.bmUuid = msg.getBaremetalInstanceUuid();
        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.DELETE_BM_NOVNC_PROXY_PATH), cmd, new JsonAsyncRESTCallback<DeleteBmNoVNCProxyRsp>(reply) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteBmNoVNCProxyRsp ret) {
                if (!ret.success) {
                    reply.setError(operr("failed to delete bm instance novnc proxy on baremetal pxeserver[uuid:%s]", self.getUuid()));
                } else {
                    // no need to delete nginx proxy for novnc in mn because it will be overwrite soon
                    //evf.fire(BaremetalInstanceCanonicalEvents.DELETE_NGINX_PROXY, msg.getBaremetalInstanceUuid());
                    logger.info(String.format("successfully deleted bm instance novnc proxy on baremetal pxeserver[uuid:%s]", self.getUuid()));
                }
                bus.reply(msg,reply);
            }

            @Override
            public Class<DeleteBmNoVNCProxyRsp> getReturnClass() {
                return DeleteBmNoVNCProxyRsp.class;
            }
        });
    }

    private void handle(CreateBaremetalTerminalProxyMsg msg) {
        CreateBaremetalTerminalProxyReply reply = new CreateBaremetalTerminalProxyReply();

        CreateBmNginxProxyCmd cmd = new CreateBmNginxProxyCmd();
        cmd.uuid = self.getUuid();
        cmd.bmUuid = msg.getBaremetalInstanceUuid();
        cmd.upstream = msg.getUpstream();
        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.CREATE_BM_NGINX_PROXY_PATH), cmd, new JsonAsyncRESTCallback<CreateBmNginxProxyRsp>(reply) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CreateBmNginxProxyRsp ret) {
                if (!ret.success) {
                    reply.setError(operr("failed to create bm instance nginx proxy on baremetal pxeserver[uuid:%s]", self.getUuid()));
                } else {
                    evf.fire(BaremetalInstanceCanonicalEvents.CREATE_TERMINAL_NGINX_PROXY, msg.getBaremetalInstanceUuid());
                    logger.info(String.format("successfully created bm instance nginx proxy on baremetal pxeserver[uuid:%s]", self.getUuid()));
                }
                bus.reply(msg,reply);
            }

            @Override
            public Class<CreateBmNginxProxyRsp> getReturnClass() {
                return CreateBmNginxProxyRsp.class;
            }
        });
    }

    private void handle(DeleteBaremetalTerminalProxyMsg msg) {
        DeleteBaremetalTerminalProxyReply reply = new DeleteBaremetalTerminalProxyReply();

        DeleteBmNginxProxyCmd cmd = new DeleteBmNginxProxyCmd();
        cmd.uuid = self.getUuid();
        cmd.bmUuid = msg.getBaremetalInstanceUuid();
        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.DELETE_BM_NGINX_PROXY_PATH), cmd, new JsonAsyncRESTCallback<DeleteBmNginxProxyRsp>(reply) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteBmNginxProxyRsp ret) {
                if (!ret.success) {
                    reply.setError(operr("failed to delete bm instance nginx proxy on baremetal pxeserver[uuid:%s]", self.getUuid()));
                } else {
                    evf.fire(BaremetalInstanceCanonicalEvents.DELETE_NGINX_PROXY, msg.getBaremetalInstanceUuid());
                    logger.info(String.format("successfully deleted bm instance nginx proxy on baremetal pxeserver[uuid:%s]", self.getUuid()));
                }
                bus.reply(msg,reply);
            }

            @Override
            public Class<DeleteBmNginxProxyRsp> getReturnClass() {
                return DeleteBmNginxProxyRsp.class;
            }
        });
    }

    private void handle(StartBaremetalPxeServerMsg msg) {
        final StartBaremetalPxeServerReply reply = new StartBaremetalPxeServerReply();

        StartCmd cmd = new StartCmd();
        cmd.uuid = self.getUuid();

        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.START_PATH), cmd, new JsonAsyncRESTCallback<StartRsp>(reply) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(StartRsp ret) {
                if (!ret.success) {
                    reply.setError(operr("failed to start baremetal pxeserver[uuid:%s]", self.getUuid()));
                } else {
                    logger.info(String.format("successfully started baremetal pxeserver[uuid:%s]", self.getUuid()));
                }
                self.setState(BaremetalPxeServerState.Enabled);
                self = dbf.updateAndRefresh(self);
                reply.setInventory(BaremetalPxeServerInventory.valueOf(self));
                bus.reply(msg,reply);
            }

            @Override
            public Class<StartRsp> getReturnClass() {
                return StartRsp.class;
            }
        });
    }

    private void handle(StopBaremetalPxeServerMsg msg) {
        final StopBaremetalPxeServerReply reply = new StopBaremetalPxeServerReply();

        StopCmd cmd = new StopCmd();
        cmd.uuid = self.getUuid();

        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.STOP_PATH), cmd, new JsonAsyncRESTCallback<StopRsp>(reply) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(StopRsp ret) {
                if (!ret.success) {
                    reply.setError(operr("failed to stop baremetal pxeserver[uuid:%s]", self.getUuid()));
                } else {
                    logger.info(String.format("successfully stoped baremetal pxeserver[uuid:%s]", self.getUuid()));
                }
                self.setState(BaremetalPxeServerState.Disabled);
                self = dbf.updateAndRefresh(self);
                reply.setInventory(BaremetalPxeServerInventory.valueOf(self));
                bus.reply(msg,reply);
            }

            @Override
            public Class<StopRsp> getReturnClass() {
                return StopRsp.class;
            }
        });
    }

    private void handle(DeleteBaremetalPxeServerMsg msg) {
        final DeleteBaremetalPxeServerReply reply = new DeleteBaremetalPxeServerReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return BaremetalPxeServerConstant.SYNC_SIGNATURE_OF_BAREMETAL_PXESERVER + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                final FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
                flowChain.setName(String.format("delete-baremetal-pxeserver-%s", msg.getUuid()));
                flowChain.then(new NoRollbackFlow() {
                    String __name__ = "stop-baremetal-pxeserver";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        StopBaremetalPxeServerMsg smsg = new StopBaremetalPxeServerMsg();
                        smsg.setUuid(msg.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(smsg, BaremetalPxeServerConstant.SERVICE_ID, msg.getUuid());
                        bus.send(smsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.error(String.format("failed to stop baremetal pxeserver[uuid:%s] before deleting it", msg.getUuid()));
                                }
                                trigger.next();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "detach-baremetal-pxeserver";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> clusterUuids = Q.New(BaremetalPxeServerClusterRefVO.class)
                                .eq(BaremetalPxeServerClusterRefVO_.pxeServerUuid, msg.getUuid())
                                .select(BaremetalPxeServerClusterRefVO_.clusterUuid)
                                .listValues();
                        if (clusterUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<DetachBaremetalPxeServerFromClusterMsg> dmsgs = transform(clusterUuids, clusterUuid -> {
                            DetachBaremetalPxeServerFromClusterMsg dmsg = new DetachBaremetalPxeServerFromClusterMsg();
                            dmsg.setPxeServerUuid(msg.getUuid());
                            dmsg.setClusterUuid(clusterUuid);
                            bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, msg.getUuid());
                            return dmsg;
                        });

                        if (dmsgs.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        new While<>(dmsgs).all((dmsg, whileCompletion) -> bus.send(dmsg, new CloudBusCallBack(whileCompletion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.error(reply.getError().getDetails());
                                }
                                whileCompletion.done();
                            }
                        })).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                logger.debug(String.format("detached pxeserver[uuid:%s] from all attached clusters", msg.getUuid()));
                                trigger.next();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "delete-all-pxelinux-ks-cfg-on-pxeserver";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DeleteBaremetalInstanceConfigsMsg dmsg = new DeleteBaremetalInstanceConfigsMsg();
                        dmsg.setPxeServerUuid(msg.getUuid());
                        dmsg.setPxeNicMac("*");
                        bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, msg.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.error(String.format("failed to clean up configs on baremetal pxeserver[uuid:%s]", msg.getUuid()));
                                }
                                trigger.next();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "delete-baremetal-image-cache-on-pxeserver";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<Long> cacheIds = Q.New(BaremetalImageCacheVO.class)
                                .eq(BaremetalImageCacheVO_.pxeServerUuid, msg.getUuid())
                                .select(BaremetalImageCacheVO_.id)
                                .listValues();
                        if (cacheIds == null || cacheIds.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<DeleteBaremetalImageCacheMsg> dmsgs = new ArrayList<>();
                        for (Long cacheId : cacheIds) {
                            DeleteBaremetalImageCacheMsg dmsg = new DeleteBaremetalImageCacheMsg();
                            dmsg.setCacheId(cacheId);
                            dmsg.setPxeServerUuid(msg.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, msg.getUuid());
                            dmsgs.add(dmsg);
                        }

                        new While<>(dmsgs).all((dmsg, completion) -> bus.send(dmsg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                completion.done();
                            }
                        })).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                }).done(new FlowDoneHandler(reply) {
                    @Override
                    public void handle(Map data) {
                        tracker.untrack(msg.getUuid());
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                sql(BaremetalPxeServerClusterRefVO.class)
                                        .eq(BaremetalPxeServerClusterRefVO_.pxeServerUuid, msg.getUuid())
                                        .delete();
                                sql(BaremetalPxeServerVO.class)
                                        .eq(BaremetalPxeServerVO_.uuid, msg.getUuid())
                                        .delete();
                            }
                        }.execute();
                        logger.debug(String.format("successfully deleted baremetal pxeserver[uuid:%s]", msg.getUuid()));
                        bus.reply(msg, reply);
                        chain.next();
                    }
                }).error(new FlowErrorHandler(reply) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.error(String.format("failed to delete baremetal pxeserver[uuid:%s]", msg.getUuid()));
                        reply.setError(errCode);
                        bus.reply(msg, reply);
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

    private void handle(DownloadBaremetalImageCacheMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "download-baremetal-image-cache-for-image-" + msg.getImageUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                DownloadBaremetalImageCacheReply reply = new DownloadBaremetalImageCacheReply();
                doDownloadImageCache(msg, new ReturnValueCompletion<BaremetalImageCacheInventory>(chain) {
                    @Override
                    public void success(BaremetalImageCacheInventory cache) {
                        reply.setInventory(cache);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
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

    private void handle(DeleteBaremetalImageCacheMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "delete-baremetal-image-cache-" + msg.getCacheId();
            }

            @Override
            public void run(SyncTaskChain chain) {
                DeleteBaremetalImageCacheReply reply = new DeleteBaremetalImageCacheReply();
                DeleteBmImageCacheCmd cmd = new DeleteBmImageCacheCmd();

                BaremetalImageCacheVO cache = dbf.findById(msg.getCacheId(), BaremetalImageCacheVO.class);
                cmd.uuid = self.getUuid();
                cmd.imageUuid = cache.getImageUuid();
                cmd.cacheInstallPath = cache.getInstallUrl();

                restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.DELETE_BM_IMAGE_CACHE_PATH), cmd, new JsonAsyncRESTCallback<DeleteBmImageCacheRsp>(reply) {
                    @Override
                    public void fail(ErrorCode err) {
                        reply.setError(err);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void success(DeleteBmImageCacheRsp ret) {
                        if (!ret.success) {
                            reply.setError(Platform.operr("%s", ret.error));
                        } else {
                            updateCapacity(ret.totalCapacity, ret.availableCapacity);
                            dbf.remove(cache);
                        }
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public Class<DeleteBmImageCacheRsp> getReturnClass() {
                        return DeleteBmImageCacheRsp.class;
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(PingBaremetalPxeServerMsg msg) {
        final PingBaremetalPxeServerReply reply = new PingBaremetalPxeServerReply();

        pingHook(msg.isEnabled(), new Completion(msg) {
            private void reconnect() {
                ConnectBaremetalPxeServerMsg cmsg = new ConnectBaremetalPxeServerMsg();
                cmsg.setUuid(self.getUuid());
                bus.makeTargetServiceIdByResourceUuid(cmsg, BaremetalPxeServerConstant.SERVICE_ID, self.getUuid());
                bus.send(cmsg);
            }

            @Override
            public void success() {
                self = dbf.reload(self);
                // do not reconnect pxeserver when it's disabled
                if (self != null && self.getState() == BaremetalPxeServerState.Enabled &&
                        self.getStatus() == BaremetalPxeServerStatus.Disconnected) {
                    reconnect();
                }

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                changeConnectionState(BaremetalPxeServerStatusEvent.disconnected);
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CreateBaremetalDhcpConfigMsg msg) {
        CreateBaremetalDhcpConfigReply reply = new CreateBaremetalDhcpConfigReply();
        CreateBmDhcpConfigCmd cmd = new CreateBmDhcpConfigCmd();
        cmd.chassisUuid = msg.getChassisUuid();
        cmd.pxeNicMac = msg.getPxeNicMac();
        cmd.pxeNicIp = msg.getPxeNicIp();
        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.CREATE_BM_DHCP_CONFIG_PATH), cmd, new JsonAsyncRESTCallback<CreateBmDhcpConfigRsp>(reply) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(CreateBmDhcpConfigRsp ret) {
                if (!ret.success) {
                    reply.setError(operr("failed to create dhcp config of chassis[uuid:%s] on pxeserver[uuid:%s]", msg.getChassisUuid(), self.getUuid()));
                } else {
                    logger.info(String.format("successfully created dhcp config of chassis[uuid:%s] on baremetal pxeserver[uuid:%s]", msg.getChassisUuid(), self.getUuid()));
                }
                bus.reply(msg,reply);
            }

            @Override
            public Class<CreateBmDhcpConfigRsp> getReturnClass() {
                return CreateBmDhcpConfigRsp.class;
            }
        });
    }

    private void handle(DeleteBaremetalDhcpConfigMsg msg) {
        DeleteBaremetalDhcpConfigReply reply = new DeleteBaremetalDhcpConfigReply();
        DeleteBmDhcpConfigCmd cmd = new DeleteBmDhcpConfigCmd();
        cmd.chassisUuid = msg.getChassisUuid();

        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.DELETE_BM_DHCP_CONFIG_PATH), cmd, new JsonAsyncRESTCallback<DeleteBmDhcpConfigRsp>(reply) {
            @Override
            public void fail(ErrorCode err) {
                reply.setError(err);
                bus.reply(msg, reply);
            }

            @Override
            public void success(DeleteBmDhcpConfigRsp ret) {
                if (!ret.success) {
                    reply.setError(operr("failed to delete dhcp config of chassis[uuid:%s] on pxeserver[uuid:%s]", msg.getChassisUuid(), self.getUuid()));
                } else {
                    logger.info(String.format("successfully deleted dhcp config of chassis[uuid:%s] on baremetal pxeserver[uuid:%s]", msg.getChassisUuid(), self.getUuid()));
                }
                bus.reply(msg,reply);
            }

            @Override
            public Class<DeleteBmDhcpConfigRsp> getReturnClass() {
                return DeleteBmDhcpConfigRsp.class;
            }
        });
    }

    private void handle(DetachBaremetalPxeServerFromClusterMsg msg) {
        DetachBaremetalPxeServerFromClusterReply reply = new DetachBaremetalPxeServerFromClusterReply();
        thdf.chainSubmit(new ChainTask(reply) {
            @Override
            public String getSyncSignature() {
                return String.format("detach-baremetal-pxeserver-%s-from-cluster-%s", msg.getPxeServerUuid(), msg.getClusterUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                SQL.New(BaremetalPxeServerClusterRefVO.class)
                        .eq(BaremetalPxeServerClusterRefVO_.pxeServerUuid, msg.getPxeServerUuid())
                        .eq(BaremetalPxeServerClusterRefVO_.clusterUuid, msg.getClusterUuid())
                        .delete();

                // detach extension points
                self = dbf.reload(self);
                extpEmitter.afterDetach(self, msg.getClusterUuid());
                reply.setInventory(BaremetalPxeServerInventory.valueOf(self));
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void pingHook(final boolean enabled, Completion completion) {
        final PingCmd cmd = new PingCmd();
        cmd.uuid = self.getUuid();
        cmd.dhcpInterface = self.getDhcpInterface();
        cmd.enabled = enabled;
        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.PING_PATH), cmd, new JsonAsyncRESTCallback<PingRsp>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(PingRsp ret) {
                if (ret.success && !self.getUuid().equals(ret.uuid)) {
                    ErrorCode err = operr("the uuid of baremtal pxeserver agent changed[expected:%s, actual:%s], it's most likely" +
                            " the agent was manually restarted. Issue a reconnect to sync the status", self.getUuid(), ret.uuid);
                    completion.fail(err);
                } else if (!ret.success) {
                    completion.fail(operr("operation error, because:%s", ret.error));
                } else {
                    completion.success();
                }
            }

            @Override
            public Class<PingRsp> getReturnClass() {
                return PingRsp.class;
            }
        });
    }

    private SshFileMd5Checker getPxeServerMd5Checker() {
        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setTargetIp(self.getHostname());
        checker.setUsername(self.getSshUsername());
        checker.setPassword(self.getSshPassword());
        checker.setSshPort(self.getSshPort());
        checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format(
                "%s/baremetalpxeserver/package/%s", AnsibleGlobalProperty.ZSTACK_ROOT, AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME)
        );
        checker.addSrcDestPair(PathUtil.findFileOnClassPath(
                String.format("ansible/baremetalpxeserver/%s", pxeAgentPackageName), true).getAbsolutePath(),
                String.format("%s/baremetalpxeserver/package/%s", AnsibleGlobalProperty.ZSTACK_ROOT, pxeAgentPackageName)
        );

        return checker;
    }

    private void connectHook(final boolean newAdd, final Completion completion) {
        self = dbf.reload(self);

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            continueConnect(completion);
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("deploy baremetal pxeserver agents");

        chain.then(new NoRollbackFlow() {
            String __name__ = "deploy-baremetal-pxeserver-agent";

            @Override
            public void run(FlowTrigger trigger, Map data) {

                SshFileMd5Checker checker = getPxeServerMd5Checker();
                SshYumRepoChecker repoChecker = new SshYumRepoChecker();
                repoChecker.setTargetIp(self.getHostname());
                repoChecker.setUsername(self.getSshUsername());
                repoChecker.setPassword(self.getSshPassword());
                repoChecker.setSshPort(self.getSshPort());

                AnsibleRunner runner = new AnsibleRunner();
                runner.installChecker(checker);
                runner.installChecker(repoChecker);
                runner.setTargetIp(self.getHostname());
                runner.setTargetUuid(self.getUuid());
                runner.setUsername(self.getSshUsername());
                runner.setPassword(self.getSshPassword());
                runner.setSshPort(self.getSshPort());
                runner.setAgentPort(BaremetalPxeServerGlobalProperty.AGENT_PORT);
                runner.setPlayBookName(BaremetalPxeServerConstant.ANSIBLE_PLAYBOOK_NAME);
                if (newAdd) {
                    runner.setFullDeploy(true);
                }
                BaremetalDeployArguments deployArguments = new BaremetalDeployArguments();
                deployArguments.setUpdatePackages(String.valueOf(CoreGlobalProperty.UPDATE_PKG_WHEN_CONNECT));
                runner.setDeployArguments(deployArguments);
                runner.run(new ReturnValueCompletion<Boolean>(completion) {
                    @Override
                    public void success(Boolean deployed) {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            // should use BaremetalPxeServerConnectExtensionPoint, but that will cause dependence circle
            String __name__ = "deploy-imagestore-client";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (Q.New(ImageStoreBackupStorageVO.class)
                        .eq(ImageStoreBackupStorageVO_.hostname, self.getHostname())
                        .isExists()) {
                    logger.warn("skip imagestore client deployment on pxe-server, it is also zstore.");
                    trigger.next();
                    return;
                }

                SshFileMd5Checker checker = new SshFileMd5Checker();
                String hostname = self.getHostname();
                int port = self.getSshPort();
                String username = self.getSshUsername();
                String password = self.getSshPassword();

                checker.setTargetIp(hostname);
                checker.setUsername(username);
                checker.setPassword(password);
                checker.setSshPort(port);
                checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format(
                        "ansible/imagestorebackupstorage/%s", zstAgentPackageName), true).getAbsolutePath(),
                        String.format("/var/lib/zstack/imagestorebackupstorage/package/%s", zstClientAgentPackageName)
                );
                checker.addSrcDestPair((PathUtil.join(PathUtil.getZStackHomeFolder(), "imagestore", "bin") + "/certs/ca.pem"),
                        ImageStoreBackupStorageGlobalProperty.REGISTRY_CERTS);

                AnsibleRunner runner = new AnsibleRunner();
                runner.installChecker(checker);
                runner.setPassword(password);
                runner.setUsername(username);
                runner.setTargetIp(hostname);
                runner.setTargetUuid(self.getUuid());
                runner.setSshPort(port);
                runner.setPlayBookName(ImageStoreBackupStorageConstant.ANSIBLE_PLAYBOOK_NAME);

                ImageStoreAgentDeployArguments arguments = new ImageStoreAgentDeployArguments();
                arguments.setClient("true");
                if (newAdd) {
                    runner.setFullDeploy(true);
                }
                runner.setDeployArguments(arguments);
                runner.run(new ReturnValueCompletion<Boolean>(trigger) {
                    @Override
                    public void success(Boolean deployed) {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new Flow(){
            String __name__ = "configure-iptables";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                StringBuilder builder = new StringBuilder();
                if (!ImageStoreBackupStorageGlobalProperty.MN_NETWORKS.isEmpty()) {
                    builder.append(String.format("sudo bash %s -m %s -p %s -c %s;",
                            "/var/lib/zstack/baremetal/baremetal-iptables",
                            BaremetalPxeServerConstant.IPTABLES_COMMENTS,
                            BaremetalPxeServerGlobalConfig.PXESERVER_ALLOW_PORTS.value(String.class),
                            String.join(",", BaremetalPxeServerGlobalProperty.MN_NETWORKS)));
                } else {
                    builder.append(String.format("sudo bash %s -m %s -p %s;",
                            "/var/lib/zstack/baremetal/baremetal-iptables",
                            BaremetalPxeServerConstant.IPTABLES_COMMENTS,
                            BaremetalPxeServerGlobalConfig.PXESERVER_ALLOW_PORTS.value(String.class)));
                }

                // remove default FORWARD chain rejection rule
                String removeRejectRule = "which iptables > /dev/null && " +
                        "iptables -C INPUT -j REJECT --reject-with icmp-host-prohibited > /dev/null 2>&1 && " +
                        "iptables -D INPUT -j REJECT --reject-with icmp-host-prohibited > /dev/null 2>&1 || true";
                builder.append(removeRejectRule);

                try {
                    new Ssh().shell(builder.toString())
                            .setUsername(self.getSshUsername())
                            .setPassword(self.getSshPassword())
                            .setHostname(self.getHostname())
                            .setPort(self.getSshPort()).runErrorByExceptionAndClose();
                } catch (SshException ex) {
                    throw new OperationFailureException(operr(ex.toString()));
                }

                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                continueConnect(completion);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void continueConnect(final Completion complete) {
        restf.echo(buildUrl(BaremetalPxeServerConstant.ECHO_PATH), new Completion(complete) {
            @Override
            public void success() {
                String url = buildUrl(BaremetalPxeServerConstant.CONNECT_PATH);
                ConnectCmd cmd = new ConnectCmd();
                cmd.uuid = self.getUuid();
                cmd.storagePath = self.getStoragePath();
                ConnectRsp rsp = restf.syncJsonPost(url, cmd, ConnectRsp.class);
                if (!rsp.success) {
                    ErrorCode err = operr("unable to connect to baremetal pxeserver[url:%s], because %s", url, rsp.error);
                    complete.fail(err);
                    return;
                }

                updateCapacity(rsp.totalCapacity, rsp.availableCapacity);
                logger.debug(String.format("connected to baremetal pxeserver[uuid:%s, name:%s, total capacity:%sG, available capacity: %sG",
                        self.getUuid(), self.getName(), rsp.totalCapacity, rsp.availableCapacity));
                complete.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                complete.fail(errorCode);
            }
        });
    }

    private void updateCapacity(Long totalCapacity, Long availableCapacity) {
        if (totalCapacity == null || availableCapacity == null) {
            return;
        }

        self.setTotalCapacity(totalCapacity);
        self.setAvailableCapacity(availableCapacity);
        self = dbf.updateAndRefresh(self);
    }

    private String buildUrl(String subPath) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(BaremetalPxeServerGlobalProperty.AGENT_URL_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(self.getHostname());
        }

        ub.port(BaremetalPxeServerGlobalProperty.AGENT_PORT);
        if (!"".equals(BaremetalPxeServerGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(BaremetalPxeServerGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);
        return ub.build().toUriString();
    }

    private void changeConnectionState(BaremetalPxeServerStatusEvent event) {
        String uuid = self.getUuid();
        self = dbf.reload(self);
        if(self == null){
            throw new CloudRuntimeException(String.format(
                    "change baremetal pxeserver connection state fail, can not find the pxe server[uuid:%s]", uuid)
            );
        }

        BaremetalPxeServerStatus before = self.getStatus();
        BaremetalPxeServerStatus next = before.nextStatus(event);
        if (before == next) {
            return;
        }

        self.setStatus(next);
        self = dbf.updateAndRefresh(self);
        logger.debug(String.format("Baremetal pxe server %s [uuid:%s] changed connection state from %s to %s",
                self.getName(), self.getUuid(), before, next));

        BaremetalPxeServerCanonicalEvents.BaremetalPxeServerStatusChangeData data = new BaremetalPxeServerCanonicalEvents.BaremetalPxeServerStatusChangeData();
        data.setOldStatus(before.toString());
        data.setNewStatus(self.getStatus().toString());
        data.setPxeServerUuid(self.getUuid());
        data.setPxeServerHostName(self.getHostname());
        evf.fire(BaremetalPxeServerCanonicalEvents.BAREMETAL_PXE_SERVER_STATUS_CHANGE, data);
    }

    private String makeCachedImageInstallPath(String imageUuid) {
        return PathUtil.join(self.getStoragePath(), "imagecache", imageUuid, imageUuid);
    }

    private void doDownloadImageCache(DownloadBaremetalImageCacheMsg msg, ReturnValueCompletion<BaremetalImageCacheInventory> completion) {
        // already in cache
        BaremetalImageCacheVO cache = Q.New(BaremetalImageCacheVO.class)
                .eq(BaremetalImageCacheVO_.imageUuid, msg.getImageUuid())
                .eq(BaremetalImageCacheVO_.pxeServerUuid, msg.getPxeServerUuid())
                .find();
        if (cache != null) {
            MountBmImageCacheCmd cmd = new MountBmImageCacheCmd();
            cmd.uuid = self.getUuid();
            cmd.imageUuid = cache.getImageUuid();
            cmd.cacheInstallPath = cache.getInstallUrl();
            restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.MOUNT_BM_IMAGE_CACHE_PATH), cmd,
                    new JsonAsyncRESTCallback<MountBmImageCacheRsp>(completion) {
                @Override
                public void fail(ErrorCode err) {
                    completion.fail(Platform.operr("failed to mount baremetal cache of image[uuid:%s]", cache.getImageUuid()));
                }

                @Override
                public void success(MountBmImageCacheRsp ret) {
                    if (!ret.success) {
                        completion.fail(Platform.operr("%s", ret.error));
                    } else {
                        cache.setUtilization(cache.getUtilization() + 1);
                        completion.success(BaremetalImageCacheInventory.valueOf(dbf.updateAndRefresh(cache)));
                    }
                }

                @Override
                public Class<MountBmImageCacheRsp> getReturnClass() {
                    return MountBmImageCacheRsp.class;
                }
            });
            return;
        }

        // download to cache
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-image-%s-to-baremetal-image-cache", msg.getImageUuid()));
        chain.then(new ShareFlow() {
            BaremetalImageCacheInventory cacheInv = null;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-orphan-baremetal-image-cache";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<Long> orphanCacheIds = SQL.New(
                                "select cache.id from BaremetalImageCacheVO cache where " +
                                "cache.imageUuid not in (select uuid from ImageVO) " +
                                "group by cache.id", Long.class)
                                .list();
                        if (orphanCacheIds == null || orphanCacheIds.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        List<DeleteBaremetalImageCacheMsg> msgs = new ArrayList<>();
                        for (Long cacheId : orphanCacheIds) {
                            DeleteBaremetalImageCacheMsg msg = new DeleteBaremetalImageCacheMsg();
                            msg.setPxeServerUuid(self.getUuid());
                            msg.setCacheId(cacheId);
                            bus.makeLocalServiceId(msg, BaremetalPxeServerConstant.SERVICE_ID);
                            msgs.add(msg);
                        }

                        new While<>(msgs).all((m, comp) -> bus.send(m, new CloudBusCallBack(comp) {
                            @Override
                            public void run(MessageReply reply) {
                                comp.done();
                            }
                        })).run(new WhileDoneCompletion(msg) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "make-space-for-new-image";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        Long sizeInNeed = Q.New(ImageVO.class)
                                .eq(ImageVO_.uuid, msg.getImageUuid())
                                .select(ImageVO_.actualSize)
                                .findValue();
                        long sizeLeft = self.getAvailableCapacity();
                        long reservedCapacity = SizeUtils.sizeStringToBytes(BaremetalPxeServerGlobalConfig.RESERVED_CAPACITY.value());
                        if (sizeLeft > sizeInNeed + reservedCapacity) {
                            trigger.next();
                            return;
                        }

                        List<DeleteBaremetalImageCacheMsg> msgs = new ArrayList<>();
                        List<Tuple> tuples = Q.New(BaremetalImageCacheVO.class)
                                .select(BaremetalImageCacheVO_.id, BaremetalImageCacheVO_.actualSize)
                                .orderBy(BaremetalImageCacheVO_.utilization, SimpleQuery.Od.ASC)
                                .listTuple();
                        for (Tuple tuple : tuples) {
                            Long cacheId = tuple.get(0, Long.class);
                            Long imageSize   = tuple.get(1, Long.class);
                            DeleteBaremetalImageCacheMsg dmsg = new DeleteBaremetalImageCacheMsg();
                            dmsg.setPxeServerUuid(self.getUuid());
                            dmsg.setCacheId(cacheId);
                            bus.makeLocalServiceId(dmsg, BaremetalPxeServerConstant.SERVICE_ID);
                            msgs.add(dmsg);

                            if (sizeInNeed < imageSize) {
                                sizeInNeed = 0L;
                                break;
                            } else {
                                sizeInNeed -= imageSize;
                            }
                        }

                        if (sizeInNeed != 0) {
                            trigger.fail(operr(
                                    "no enough space left in baremetal image cache for image[uuid:%s]",
                                    msg.getImageUuid()
                            ));
                            return;
                        }

                        new While<>(msgs).all((m, comp) -> bus.send(m, new CloudBusCallBack(comp) {
                            @Override
                            public void run(MessageReply reply) {
                                comp.done();
                            }
                        })).run(new WhileDoneCompletion(msg) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-baremetal-image-cache-vo";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ImageVO image = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
                        BaremetalImageCacheVO cache = new BaremetalImageCacheVO();
                        cache.setPxeServerUuid(msg.getPxeServerUuid());
                        cache.setImageUuid(msg.getImageUuid());
                        cache.setUrl(image.getUrl());
                        cache.setInstallUrl(makeCachedImageInstallPath(msg.getImageUuid()));
                        cache.setMediaType(image.getMediaType());
                        cache.setSize(image.getSize());
                        cache.setActualSize(image.getActualSize());
                        cache.setMd5sum(image.getMd5Sum());
                        cache.setUtilization(1L);
                        cache = dbf.persistAndRefresh(cache);
                        cacheInv = BaremetalImageCacheInventory.valueOf(cache);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (cacheInv != null) {
                            SQL.New(BaremetalImageCacheVO.class)
                                    .eq(BaremetalImageCacheVO_.imageUuid, msg.getImageUuid())
                                    .delete();
                            cacheInv = null;
                        }
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "download-image-into-baremetal-image-cache";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (CoreGlobalProperty.UNIT_TEST_ON) {
                            trigger.next();
                            return;
                        }

                        ImageBackupStorageRefVO ref = Q.New(ImageBackupStorageRefVO.class)
                                .eq(ImageBackupStorageRefVO_.imageUuid, msg.getImageUuid())
                                .limit(1)
                                .find();

                        CreateBmImageCacheCmd cmd = new CreateBmImageCacheCmd();
                        cmd.uuid = self.getUuid();
                        cmd.imageUuid = msg.getImageUuid();
                        cmd.imageInstallPath = ref.getInstallPath();
                        cmd.cacheInstallPath = cacheInv.getInstallUrl();

                        // FIXME
                        String url;
                        Tuple bsInfo = SQL.New("select bs.uuid, bs.type from BackupStorageVO bs, ImageBackupStorageRefVO ref " +
                                "where ref.imageUuid = (:imageUuid) and bs.uuid = ref.backupStorageUuid", Tuple.class)
                                .param("imageUuid", cacheInv.getImageUuid())
                                .limit(1)
                                .find();
                        String bsUuid = (String) bsInfo.get(0);
                        String bsType = (String) bsInfo.get(1);
                        switch (bsType) {
                            case ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE:
                                url = buildUrl(BaremetalPxeServerConstant.DOWNLOAD_FROM_IMAGESTORE_PATH);
                                cmd.hostname = Q.New(ImageStoreBackupStorageVO.class)
                                        .eq(ImageStoreBackupStorageVO_.uuid, bsUuid)
                                        .select(ImageStoreBackupStorageVO_.hostname)
                                        .findValue();
                                break;
                            case CephConstants.CEPH_BACKUP_STORAGE_TYPE:
                                url = buildUrl(BaremetalPxeServerConstant.DOWNLOAD_FROM_CEPHB_PATH);
                                break;
                            default:
                                trigger.fail(Platform.operr("unsupported backup storage type for baremetal"));
                                return;
                        }
                        restf.asyncJsonPost(url, cmd, new JsonAsyncRESTCallback<CreateBmImageCacheRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.fail(err);
                            }

                            @Override
                            public void success(CreateBmImageCacheRsp ret) {
                                if (!ret.success) {
                                    trigger.fail(Platform.operr("%s", ret.error));
                                } else {
                                    updateCapacity(ret.totalCapacity, ret.availableCapacity);
                                    trigger.next();
                                }
                            }

                            @Override
                            public Class<CreateBmImageCacheRsp> getReturnClass() {
                                return CreateBmImageCacheRsp.class;
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        DeleteBmImageCacheCmd cmd = new DeleteBmImageCacheCmd();
                        cmd.uuid = self.getUuid();
                        cmd.imageUuid = cacheInv.getImageUuid();
                        cmd.cacheInstallPath = cacheInv.getInstallUrl();
                        restf.asyncJsonPost(buildUrl(BaremetalPxeServerConstant.DELETE_BM_IMAGE_CACHE_PATH), cmd,
                                new JsonAsyncRESTCallback<DeleteBmImageCacheRsp>(trigger) {
                            @Override
                            public void fail(ErrorCode err) {
                                trigger.rollback();
                            }

                            @Override
                            public void success(DeleteBmImageCacheRsp ret) {
                                updateCapacity(ret.totalCapacity, ret.availableCapacity);
                                trigger.rollback();
                            }

                            @Override
                            public Class<DeleteBmImageCacheRsp> getReturnClass() {
                                return DeleteBmImageCacheRsp.class;
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success(cacheInv);
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    protected class BaremetalNicCfg {
        private boolean pxe;
        private String mac;
        private String ip;
        private String netmask;
        private String gateway;
        private String nameserver;

        // for vlan
        private Integer vlanid;

        // for bonding
        private String bondName;
        private Integer bondMode;
        private String bondSlaves;
        private String bondOpts;
    }
}
