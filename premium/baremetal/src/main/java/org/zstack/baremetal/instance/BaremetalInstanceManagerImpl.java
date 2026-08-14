package org.zstack.baremetal.instance;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.baremetal.BaremetalConstant;
import org.zstack.header.baremetal.chassis.BaremetalChassisStatus;
import org.zstack.header.baremetal.chassis.BaremetalChassisVO;
import org.zstack.header.baremetal.instance.*;
import org.zstack.header.baremetal.network.*;
import org.zstack.header.baremetal.preconfiguration.CustomPreconfigurationVO;
import org.zstack.header.baremetal.pxeserver.*;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.allocator.BeforeAllocateIpExtensionPoint;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Type;
import java.util.*;

import static org.zstack.core.Platform.err;

/**
 * Created by GuoYi on 7/5/18.
 */
public class BaremetalInstanceManagerImpl extends AbstractService implements
        BaremetalInstanceManager,
        GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(BaremetalInstanceManagerImpl.class);
    private Map<String, BaremetalNicFactory> bmNicFactories = Collections.synchronizedMap(new HashMap<>());

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected TagManager tagMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected EventFacade evf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected BaremetalInstanceDeletionPolicyManager deletionPolicyMgr;
    @Autowired
    protected BaremetalInstanceStaticConfigManager cfgMgr;

    private void handleNotifyDeployBegin(BaremetalInstanceCommands.NotifyDeployBeginCmd cmd) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return "notify-deploy-begin-for-bm-" + cmd.baremetalInstanceUuid;
            }

            @Override
            public void run(SyncTaskChain chain) {
                BaremetalInstanceVO bm = dbf.findByUuid(cmd.baremetalInstanceUuid, BaremetalInstanceVO.class);
                if (bm == null) {
                    logger.error(String.format("Baremetal instance %s does not exist", cmd.baremetalInstanceUuid));
                    chain.next();
                    return;
                }

                FlowChain fc = FlowChainBuilder.newSimpleFlowChain();
                fc.setName("handle-bm-begin-deploy-notification");
                fc.then(new Flow() {
                    String __name__ = "create-baremetal-novnc-proxy";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateBaremetalNoVNCProxyMsg cmsg = new CreateBaremetalNoVNCProxyMsg();
                        cmsg.setPxeServerUuid(bm.getPxeServerUuid());
                        cmsg.setBaremetalInstanceUuid(cmd.baremetalInstanceUuid);
                        cmsg.setUpstream(String.format(
                                "%s: %s:5901",
                                cmd.baremetalInstanceUuid,
                                getPxeBootIpAddress(cmd.baremetalInstanceUuid)
                        ));
                        bus.makeLocalServiceId(cmsg, BaremetalPxeServerConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    logger.info("Successfully created novnc proxy for baremetal instance " + cmd.baremetalInstanceUuid);
                                    trigger.next();
                                } else {
                                    logger.error("Failed to create novnc proxy for baremetal instance " + cmd.baremetalInstanceUuid);
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        DeleteBaremetalNoVNCProxyMsg dmsg = new DeleteBaremetalNoVNCProxyMsg();
                        dmsg.setPxeServerUuid(bm.getPxeServerUuid());
                        dmsg.setBaremetalInstanceUuid(cmd.baremetalInstanceUuid);
                        bus.makeLocalServiceId(dmsg, BaremetalPxeServerConstant.SERVICE_ID);
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.rollback();
                            }
                        });
                    }
                }).done(new FlowDoneHandler(chain) {
                    @Override
                    public void handle(Map data) {
                        BaremetalInstanceVO bm = dbf.findByUuid(cmd.baremetalInstanceUuid, BaremetalInstanceVO.class);
                        bm.setState(BaremetalInstanceState.Running);
                        bm.setStatus(BaremetalInstanceStatus.Provisioning);
                        dbf.update(bm);
                        logger.info("Successfully handled bm instance begin deploy notification.");
                        chain.next();
                    }
                }).error(new FlowErrorHandler(chain) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        BaremetalInstanceVO bm = dbf.findByUuid(cmd.baremetalInstanceUuid, BaremetalInstanceVO.class);
                        bm.setState(BaremetalInstanceState.Error);
                        dbf.update(bm);
                        logger.info("Failed to handle bm instance begin deploy notification.");
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

    private void handleNotifyDeployComplete(BaremetalInstanceCommands.NotifyDeployCompleteCmd cmd) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return "notify-deploy-complete-for-bm-" + cmd.baremetalInstanceUuid;
            }

            @Override
            public void run(SyncTaskChain chain) {
                BaremetalInstanceVO bm = dbf.findByUuid(cmd.baremetalInstanceUuid, BaremetalInstanceVO.class);
                if (bm == null) {
                    logger.error(String.format("Baremetal instance %s does not exist", cmd.baremetalInstanceUuid));
                    chain.next();
                    return;
                }

                FlowChain fc = FlowChainBuilder.newSimpleFlowChain();
                fc.setName("clean-up-after-deploy-complete");
                fc.then(new NoRollbackFlow() {
                    String __name__ = "delete-baremetal-instance-configs";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DeleteBaremetalInstanceConfigsMsg dmsg = new DeleteBaremetalInstanceConfigsMsg();
                        dmsg.setPxeServerUuid(bm.getPxeServerUuid());
                        dmsg.setPxeNicMac(getPxeBootMacAddress(cmd.baremetalInstanceUuid));
                        bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, bm.getPxeServerUuid());
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.next();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "delete-baremetal-novnc-proxy";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        DeleteBaremetalNoVNCProxyMsg dmsg = new DeleteBaremetalNoVNCProxyMsg();
                        dmsg.setPxeServerUuid(bm.getPxeServerUuid());
                        dmsg.setBaremetalInstanceUuid(cmd.baremetalInstanceUuid);
                        bus.makeLocalServiceId(dmsg, BaremetalPxeServerConstant.SERVICE_ID);
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
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
                }).then(new Flow() {
                    String __name__ = "create-baremetal-terminal-proxy";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CreateBaremetalTerminalProxyMsg cmsg = new CreateBaremetalTerminalProxyMsg();
                        cmsg.setPxeServerUuid(bm.getPxeServerUuid());
                        cmsg.setBaremetalInstanceUuid(cmd.baremetalInstanceUuid);
                        cmsg.setUpstream(String.format(
                                "location /%s { proxy_pass http://%s:%s/; }",
                                cmd.baremetalInstanceUuid,
                                getPxeBootIpAddress(cmd.baremetalInstanceUuid),
                                BaremetalConstant.SHELLINABOXD_PORT
                        ));
                        bus.makeLocalServiceId(cmsg, BaremetalPxeServerConstant.SERVICE_ID);
                        bus.send(cmsg, new CloudBusCallBack(trigger) {
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

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        DeleteBaremetalTerminalProxyMsg dmsg = new DeleteBaremetalTerminalProxyMsg();
                        dmsg.setPxeServerUuid(bm.getPxeServerUuid());
                        dmsg.setBaremetalInstanceUuid(cmd.baremetalInstanceUuid);
                        bus.makeLocalServiceId(dmsg, BaremetalPxeServerConstant.SERVICE_ID);
                        bus.send(dmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                trigger.rollback();
                            }
                        });
                    }
                }).done(new FlowDoneHandler(chain) {
                    @Override
                    public void handle(Map data) {
                        BaremetalInstanceVO bm = dbf.findByUuid(cmd.baremetalInstanceUuid, BaremetalInstanceVO.class);
                        bm.setState(BaremetalInstanceState.Rebooting);
                        bm.setStatus(BaremetalInstanceStatus.Provisioned);
                        dbf.update(bm);
                        logger.info("Successfully handled bm complete deploy notification.");
                        chain.next();
                    }
                }).error(new FlowErrorHandler(chain) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        BaremetalInstanceVO bm = dbf.findByUuid(cmd.baremetalInstanceUuid, BaremetalInstanceVO.class);
                        bm.setState(BaremetalInstanceState.Error);
                        dbf.update(bm);
                        logger.error("Failed to handled bm complete deploy notification.");
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

    private void handleNotifyOsRunning(BaremetalInstanceCommands.NotifyOSRunningCmd cmd) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return "notify-os-running-for-bm-" + cmd.baremetalInstanceUuid;
            }

            @Override
            public void run(SyncTaskChain chain) {
                BaremetalInstanceVO bm = Q.New(BaremetalInstanceVO.class)
                        .eq(BaremetalInstanceVO_.uuid, cmd.baremetalInstanceUuid)
                        .find();
                if (bm == null) {
                    logger.error(String.format("Baremetal instance %s does not exist",
                            cmd.baremetalInstanceUuid)
                    );
                    chain.next();
                    return;
                }

                bm.setState(BaremetalInstanceState.Running);
                dbf.update(bm);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @Override
    public boolean start() {
        restf.registerSyncHttpCallHandler(BaremetalInstanceConstant.NOTIFY_DEPLOY_BEGIN,
                BaremetalInstanceCommands.NotifyDeployBeginCmd.class,
                cmd -> {
                    // ABORT THE PROVISIONING PROGRESS THAT'S NOT STARTED BY ZSTACK!
                    BaremetalInstanceVO bm = dbf.findByUuid(cmd.baremetalInstanceUuid, BaremetalInstanceVO.class);
                    if (bm == null || bm.getState() != BaremetalInstanceState.Rebooting || bm.getStatus() != BaremetalInstanceStatus.Unprovisioned) {
                        throw new CloudRuntimeException(String.format(
                                "Abort the provisioning of bm[uuid:%s] because the command seems like not sent by ZStack",
                                cmd.baremetalInstanceUuid
                        ));
                    }

                    handleNotifyDeployBegin(cmd);
                    return null;
                });

        restf.registerSyncHttpCallHandler(BaremetalInstanceConstant.NOTIFY_DEPLOY_COMPLETE,
                BaremetalInstanceCommands.NotifyDeployCompleteCmd.class,
                cmd -> {
                    handleNotifyDeployComplete(cmd);
                    return null;
                });

        restf.registerSyncHttpCallHandler(BaremetalInstanceConstant.NOTIFY_OS_RUNNING,
                BaremetalInstanceCommands.NotifyOSRunningCmd.class,
                cmd -> {
                    handleNotifyOsRunning(cmd);
                    return null;
                });

        setupEvents();
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(BaremetalInstanceConstant.SERVICE_ID);
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
        if (msg instanceof APICreateBaremetalInstanceMsg) {
            handle((APICreateBaremetalInstanceMsg) msg);
        } else if (msg instanceof APIUpdateBaremetalInstanceMsg) {
            handle((APIUpdateBaremetalInstanceMsg) msg);
        } else if (msg instanceof APIDestroyBaremetalInstanceMsg) {
            handle((APIDestroyBaremetalInstanceMsg) msg);
        } else if (msg instanceof APIExpungeBaremetalInstanceMsg) {
            handle((APIExpungeBaremetalInstanceMsg) msg);
        } else if (msg instanceof APIRecoverBaremetalInstanceMsg) {
            handle((APIRecoverBaremetalInstanceMsg) msg);
        } else if (msg instanceof APIStartBaremetalInstanceMsg) {
            handle((APIStartBaremetalInstanceMsg) msg);
        } else if (msg instanceof APIStopBaremetalInstanceMsg) {
            handle((APIStopBaremetalInstanceMsg) msg);
        } else if (msg instanceof APIRebootBaremetalInstanceMsg) {
            handle((APIRebootBaremetalInstanceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof BaremetalInstanceMessage) {
            passThrough((BaremetalInstanceMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(BaremetalInstanceMessage msg) {
        BaremetalInstanceVO bm = dbf.findByUuid(msg.getBaremetalInstanceUuid(), BaremetalInstanceVO.class);
        if (bm == null) {
            throw new CloudRuntimeException(String.format(
                    "cannot find baremetal instance[uuid:%s], it may have beed deleted.",
                    msg.getBaremetalInstanceUuid()
            ));
        }

        new BaremetalInstanceBase(bm).handleMessage((Message) msg);
    }

    private void handle(final APICreateBaremetalInstanceMsg msg) {
        final APICreateBaremetalInstanceEvent evt = new APICreateBaremetalInstanceEvent(msg.getId());

        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-baremetal-instance-on-chassis-%s", msg.getChassisUuid()));
        chain.then(new ShareFlow() {
            BaremetalInstanceVO bm = new BaremetalInstanceVO();
            List<UsedIpInventory> usedIps = new ArrayList<>();
            List<String> customParamUuids = new ArrayList<>();

            @Override
            public void setup() {
                Type listType = new TypeToken<ArrayList<BaremetalNicInfoStruct>>(){}.getType();
                List<BaremetalNicInfoStruct> structs = new Gson().fromJson(msg.getNicInfo(), listType);

                flow(new Flow() {
                    String __name__ = "occupy-baremetal-chassis";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        BaremetalChassisVO chassis = dbf.findByUuid(msg.getChassisUuid(), BaremetalChassisVO.class);
                        chassis.setStatus(BaremetalChassisStatus.Allocated);
                        dbf.update(chassis);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        BaremetalChassisVO chassis = dbf.findByUuid(msg.getChassisUuid(), BaremetalChassisVO.class);
                        chassis.setStatus(BaremetalChassisStatus.Available);
                        dbf.update(chassis);
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-baremetal-instance-vo";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                if (msg.getResourceUuid() != null) {
                                    bm.setUuid(msg.getResourceUuid());
                                } else {
                                    bm.setUuid(Platform.getUuid());
                                }
                                bm.setName(msg.getName());
                                bm.setDescription(msg.getDescription());
                                bm.setInternalId(dbf.generateSequenceNumber(BaremetalInstanceSequenceNumberVO.class));

                                BaremetalChassisVO chassis = findByUuid(msg.getChassisUuid(), BaremetalChassisVO.class);
                                bm.setClusterUuid(chassis.getClusterUuid());
                                bm.setPxeServerUuid(chassis.getPxeServerUuid());

                                String zoneUuid = q(ClusterVO.class)
                                        .eq(ClusterVO_.uuid, chassis.getClusterUuid())
                                        .select(ClusterVO_.zoneUuid)
                                        .findValue();
                                bm.setZoneUuid(zoneUuid);
                                bm.setChassisUuid(msg.getChassisUuid());
                                bm.setImageUuid(msg.getImageUuid());
                                bm.setTemplateUuid(msg.getTemplateUuid());
                                bm.setPlatform(msg.getPlatform());
                                if (structs != null) {
                                    for (BaremetalNicInfoStruct struct : structs) {
                                        if (struct.getPxe()) {
                                            bm.setManagementIp(struct.getIp());
                                            break;
                                        }
                                    }
                                }
                                bm.setPort(22);
                                bm.setUsername(msg.getUsername());
                                bm.setPassword(msg.getPassword());
                                bm.setState(BaremetalInstanceState.Created);
                                bm.setStatus(BaremetalInstanceStatus.Unprovisioned);
                                bm.setAccountUuid(msg.getSession().getAccountUuid());
                                persist(bm);

                                tagMgr.createTagsFromAPICreateMessage(msg, bm.getUuid(), BaremetalInstanceVO.class.getSimpleName());
                                tagMgr.copySystemTag(msg.getImageUuid(), ImageVO.class.getSimpleName(), bm.getUuid(), BaremetalInstanceVO.class.getSimpleName(), false);
                            }
                        }.execute();
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        dbf.remove(bm);
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-baremetal-nic-vo-for-pxe-boot-device";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (msg.getNicInfo() == null || msg.getNicInfo().equals("")) {
                            logger.warn(String.format("No hardware info found for baremetal chassis[uuid:%s]", msg.getChassisUuid()));
                            trigger.next();
                            return;
                        }

                        BaremetalNicInfoStruct pxeStruct = null;
                        for (BaremetalNicInfoStruct struct : structs) {
                            if (struct.getPxe()) {
                                pxeStruct = struct;
                                break;
                            }
                        }
                        if (pxeStruct == null) {
                            logger.warn(String.format("No pxe boot device found for baremetal chassis[uuid:%s]", msg.getChassisUuid()));
                            trigger.next();
                            return;
                        }

                        // ifcfg of pxe boot nic if not got from AllocateIpMsg
                        BaremetalPxeServerVO pxe = Q.New(BaremetalPxeServerVO.class)
                                .eq(BaremetalPxeServerVO_.uuid, bm.getPxeServerUuid())
                                .find();
                        BaremetalNicVO nic = new BaremetalNicVO();
                        nic.setUuid(Platform.getUuid());
                        nic.setBaremetalInstanceUuid(bm.getUuid());
                        nic.setMac(pxeStruct.getMac());
                        nic.setIp(pxeStruct.getIp());
                        nic.setNetmask(pxe == null ? null : pxe.getDhcpRangeNetmask());
                        nic.setGateway(pxe == null ? null : pxe.getDhcpInterfaceAddress());
                        nic.setPxe(true);
                        nic.setAccountUuid(msg.getSession().getAccountUuid());
                        dbf.persist(nic);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                sql(BaremetalNicVO.class)
                                        .eq(BaremetalNicVO_.baremetalInstanceUuid, bm.getUuid())
                                        .delete();

                                sql(BaremetalBondingVO.class)
                                        .eq(BaremetalBondingVO_.chassisUuid, bm.getChassisUuid())
                                        .delete();
                            }
                        }.execute();
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-baremetal-nic-vo-for-other-devices";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if ((msg.getNicCfgs() == null || msg.getNicCfgs().size() == 0) &&
                                (msg.getBondingCfgs() == null || msg.getBondingCfgs().size() == 0)) {
                            trigger.next();
                            return;
                        }

                        List<AllocateIpMsg> msgs = new ArrayList<>();
                        Map<String, String> bmStaticIps = new BaremetalInstanceStaticIpOperator().getStaticIpbyBmUuid(bm.getUuid());
                        // nic ip and uuid map for extend sdn
                        Map<String, String> nicIpUuidMap = new HashMap<String, String>();

                        // allocate ip for non-bonding nics
                        for (final String l3Uuid : msg.getNicCfgs().values()) {
                            AllocateIpMsg amsg = new AllocateIpMsg();
                            String staticIp = bmStaticIps.get(l3Uuid);
                            if (staticIp != null) {
                                amsg.setRequiredIp(staticIp);
                            }

                            amsg.setL3NetworkUuid(l3Uuid);
                            bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, l3Uuid);
                            msgs.add(amsg);
                            // allocate ip from extend sdn
                            for (BeforeAllocateIpExtensionPoint extension: pluginRgty.getExtensionList(BeforeAllocateIpExtensionPoint.class)) {
                                String mac = MapUtils.invertMap(msg.getNicCfgs()).get(l3Uuid).toString();
                                // SystemTag format: switch_ip,switch_interface_name
                                // 10.0.16.253,Eth0/0/1
                                String swInfo = getSwitchSystemTags(msg.getSystemTags(), mac);
                                String nicUuid = extension.allocateIpBySdn(amsg, bm.getUuid(), mac, swInfo);
                                if (nicUuid != null) {
                                    nicIpUuidMap.put(amsg.getRequiredIp(), nicUuid);
                                }
                            }
                        }

                        // allocate ip for bonding nics
                        for (final String l3Uuid : msg.getBondingCfgs().values()) {
                            AllocateIpMsg amsg = new AllocateIpMsg();
                            String staticIp = bmStaticIps.get(l3Uuid);
                            if (staticIp != null) {
                                amsg.setRequiredIp(staticIp);
                            }

                            amsg.setL3NetworkUuid(l3Uuid);
                            bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, l3Uuid);
                            msgs.add(amsg);
                            // allocate ip from extend sdn
                            for (BeforeAllocateIpExtensionPoint extension: pluginRgty.getExtensionList(BeforeAllocateIpExtensionPoint.class)) {
                                String bondUuid = MapUtils.invertMap(msg.getBondingCfgs()).get(l3Uuid).toString();
                                BaremetalBondingVO bondingVO = dbf.findByUuid(bondUuid, BaremetalBondingVO.class);
                                String macs = bondingVO.getSlaves();
                                List<String> macList = Arrays.asList(macs.split(","));
                                String swInfo = "";
                                for (String mac: macList) {
                                    String macSwitchInfo = getSwitchSystemTags(msg.getSystemTags(), mac);
                                    if (macSwitchInfo == null) {
                                        continue;
                                    }
                                    swInfo += macSwitchInfo + '-';
                                }
                                swInfo = swInfo.length() == 0 ? swInfo : swInfo.substring(0, swInfo.length() - 1);
                                String nicUuid = extension.allocateIpBySdn(amsg, bm.getUuid(), macList.get(0), swInfo);
                                if (nicUuid != null) {
                                    nicIpUuidMap.put(amsg.getRequiredIp(), nicUuid);
                                }
                            }
                        }

                        bus.send(msgs, new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                ErrorCode err = null;
                                for (MessageReply reply : replies) {
                                    if (reply.isSuccess()) {
                                        AllocateIpReply areply = reply.castReply();
                                        usedIps.add(areply.getIpInventory());

                                        BaremetalNicVO nic = new BaremetalNicVO();
                                        // check and replace nic uuid if create by extend sdn
                                        String ip = areply.getIpInventory().getIp();
                                        String nicUuid = nicIpUuidMap.get(ip) == null ? Platform.getUuid() : nicIpUuidMap.get(ip);
                                        nic.setUuid(nicUuid);
                                        nic.setBaremetalInstanceUuid(bm.getUuid());
                                        nic.setL3NetworkUuid(areply.getIpInventory().getL3NetworkUuid());
                                        nic.setUsedIpUuid(areply.getIpInventory().getUuid());
                                        nic.setIp(areply.getIpInventory().getIp());
                                        nic.setNetmask(areply.getIpInventory().getNetmask());
                                        nic.setGateway(areply.getIpInventory().getGateway());
                                        nic.setAccountUuid(msg.getSession().getAccountUuid());

                                        // one mac, one l3
                                        for (Map.Entry<String, String> entry : msg.getNicCfgs().entrySet()) {
                                            if (entry.getValue().equals(nic.getL3NetworkUuid())) {
                                                nic.setMac(entry.getKey().toLowerCase());
                                                break;
                                            }
                                        }

                                        // one bond, one l3
                                        for (Map.Entry<String, String> entry : msg.getBondingCfgs().entrySet()) {
                                            String slaves = Q.New(BaremetalBondingVO.class)
                                                    .select(BaremetalBondingVO_.slaves)
                                                    .eq(BaremetalBondingVO_.uuid, entry.getKey())
                                                    .findValue();

                                            if (entry.getValue().equals(nic.getL3NetworkUuid())) {
                                                nic.setMac(slaves);
                                                nic.setBaremetalBondingUuid(entry.getKey());
                                                break;
                                            }
                                        }

                                        if (nic.getMac() == null && nic.getBaremetalBondingUuid() == null) {
                                            logger.warn(String.format("no device found for BaremetalNicVO[uuid:%s]", nic.getUuid()));
                                        }

                                        // persist nic vo
                                        L2NetworkVO l2 = SQL.New("select l2 from L2NetworkVO l2, L3NetworkVO l3 where l2.uuid = l3.l2NetworkUuid and l3.uuid = :l3Uuid")
                                                .param("l3Uuid", nic.getL3NetworkUuid())
                                                .find();
                                        BaremetalNicFactory factory = bmNicFactories.get(l2.getType());
                                        // BaremetalNoVlanNicFactory is default
                                        if (factory == null) {
                                            factory = bmNicFactories.get(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
                                        }
                                        factory.createBaremetalNic(nic, l2.toInventory());
                                    } else {
                                        err = reply.getError();
                                        // release ip from sugon sdn
                                        AllocateIpReply areply = reply.castReply();
                                        String ip = areply.getIpInventory().getIp();
                                        String nicUuid = nicIpUuidMap.get(ip);
                                        if (nicUuid != null) {
                                            for (BeforeAllocateIpExtensionPoint extension: pluginRgty.getExtensionList(BeforeAllocateIpExtensionPoint.class)) {
                                                extension.releaseIpFromSdn(nicUuid, areply.getIpInventory().getL3NetworkUuid());
                                            }
                                        }
                                        break;
                                    }
                                }

                                if (err == null) {
                                    trigger.next();
                                } else {
                                    trigger.fail(err);
                                }
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        // return allocated ips
                        List<ReturnIpMsg> msgs = new ArrayList<>();
                        for (UsedIpInventory ip : usedIps) {
                            ReturnIpMsg msg = new ReturnIpMsg();
                            msg.setL3NetworkUuid(ip.getL3NetworkUuid());
                            msg.setUsedIpUuid(ip.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
                            msgs.add(msg);
                        }

                        if (msgs.isEmpty()) {
                            trigger.rollback();
                            return;
                        }

                        bus.send(msgs, 1, new CloudBusListCallBack(trigger) {
                            @Override
                            public void run(List<MessageReply> replies) {
                                trigger.rollback();
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "create-bm-custom-preconfiguration-vos";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (msg.getCustomConfigurations() == null) {
                            trigger.next();
                            return;
                        }

                        List<CustomPreconfigurationVO> vos = new ArrayList<>();
                        for (Map.Entry<String, String> entry : msg.getCustomConfigurations().entrySet()) {
                            CustomPreconfigurationVO vo = new CustomPreconfigurationVO();
                            vo.setUuid(Platform.getUuid());
                            vo.setBaremetalInstanceUuid(bm.getUuid());
                            vo.setParam(entry.getKey());
                            vo.setValue(entry.getValue());
                            vos.add(vo);
                            customParamUuids.add(vo.getUuid());
                        }
                        dbf.persistCollection(vos);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (!customParamUuids.isEmpty()) {
                            dbf.removeCollection(customParamUuids, CustomPreconfigurationVO.class);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "pxe-boot-baremetal-instance-if-InstantStart";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (msg.getStrategy().equals(BaremetalCreationStrategy.InstantStart.toString())) {
                            StartBaremetalInstanceMsg smsg = new StartBaremetalInstanceMsg();
                            smsg.setUuid(bm.getUuid());
                            smsg.setPxeBoot(true);
                            smsg.setReboot(true);
                            bus.makeTargetServiceIdByResourceUuid(smsg, BaremetalInstanceConstant.SERVICE_ID, bm.getUuid());
                            bus.send(smsg, new CloudBusCallBack(evt) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (reply.isSuccess()) {
                                        trigger.next();
                                    } else {
                                        trigger.fail(reply.getError());
                                    }
                                }
                            });
                        } else {
                            trigger.next();
                        }
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        evt.setInventory(BaremetalInstanceInventory.valueOf(dbf.reload(bm)));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(APIUpdateBaremetalInstanceMsg msg) {
        APIUpdateBaremetalInstanceEvent evt = new APIUpdateBaremetalInstanceEvent(msg.getId());
        BaremetalInstanceVO bm = dbf.findByUuid(msg.getUuid(), BaremetalInstanceVO.class);
        if (msg.getName() != null) {
            bm.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            bm.setDescription(msg.getDescription());
        }
        if (msg.getPassword() != null) {
            bm.setPassword(msg.getPassword());
        }
        if (msg.getPlatform() != null) {
            bm.setPlatform(msg.getPlatform());
        }
        bm = dbf.updateAndRefresh(bm);
        evt.setInventory(BaremetalInstanceInventory.valueOf(bm));
        bus.publish(evt);
    }

    private void handle(APIDestroyBaremetalInstanceMsg msg) {
        APIDestroyBaremetalInstanceEvent evt = new APIDestroyBaremetalInstanceEvent(msg.getId());
        destroyBM(msg, new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIExpungeBaremetalInstanceMsg msg) {
        APIExpungeBaremetalInstanceEvent evt = new APIExpungeBaremetalInstanceEvent(msg.getId());
        DestroyBaremetalInstanceMsg dmsg = new DestroyBaremetalInstanceMsg();
        dmsg.setUuid(msg.getUuid());
        dmsg.setDeletionPolicy(BaremetalInstanceDeletionPolicyManager.BaremetalInstanceDeletionPolicy.Direct.toString());
        bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalInstanceConstant.SERVICE_ID, msg.getUuid());
        bus.send(dmsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }

    private void handle(APIRecoverBaremetalInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return BaremetalInstanceConstant.SYNC_SIGNATURE_OF_BAREMETAL_INSTANCE + msg.getUuid();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                final APIRecoverBaremetalInstanceEvent evt = new APIRecoverBaremetalInstanceEvent(msg.getId());
                BaremetalInstanceVO bm = dbf.findByUuid(msg.getUuid(), BaremetalInstanceVO.class);
                bm.setState(BaremetalInstanceState.Stopped);
                bm = dbf.updateAndRefresh(bm);
                evt.setInventory(BaremetalInstanceInventory.valueOf(bm));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APIStartBaremetalInstanceMsg msg) {
        APIStartBaremetalInstanceEvent evt = new APIStartBaremetalInstanceEvent(msg.getId());
        StartBaremetalInstanceMsg smsg = new StartBaremetalInstanceMsg();
        smsg.setUuid(msg.getUuid());
        smsg.setPxeBoot(msg.getPxeBoot());
        bus.makeTargetServiceIdByResourceUuid(smsg, BaremetalInstanceConstant.SERVICE_ID, msg.getUuid());
        bus.send(smsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    BaremetalInstanceVO bm = dbf.findByUuid(msg.getUuid(), BaremetalInstanceVO.class);
                    evt.setInventory(BaremetalInstanceInventory.valueOf(bm));
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }

    private void handle(APIStopBaremetalInstanceMsg msg) {
        APIStopBaremetalInstanceEvent evt = new APIStopBaremetalInstanceEvent(msg.getId());
        StopBaremetalInstanceMsg smsg = new StopBaremetalInstanceMsg();
        smsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(smsg, BaremetalInstanceConstant.SERVICE_ID, msg.getUuid());
        bus.send(smsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    BaremetalInstanceVO bm = dbf.findByUuid(msg.getUuid(), BaremetalInstanceVO.class);
                    evt.setInventory(BaremetalInstanceInventory.valueOf(bm));
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }

    private void handle(APIRebootBaremetalInstanceMsg msg) {
        APIRebootBaremetalInstanceEvent evt = new APIRebootBaremetalInstanceEvent(msg.getId());
        StartBaremetalInstanceMsg smsg = new StartBaremetalInstanceMsg();
        smsg.setUuid(msg.getUuid());
        smsg.setReboot(true);
        smsg.setPxeBoot(msg.getPxeBoot());
        bus.makeTargetServiceIdByResourceUuid(smsg, BaremetalInstanceConstant.SERVICE_ID, msg.getUuid());
        bus.send(smsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    BaremetalInstanceVO bm = dbf.findByUuid(msg.getUuid(), BaremetalInstanceVO.class);
                    evt.setInventory(BaremetalInstanceInventory.valueOf(bm));
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }

    private void destroyBM(APIDestroyBaremetalInstanceMsg msg, final Completion completion) {
        final String issuer = BaremetalInstanceVO.class.getSimpleName();
        final List<BaremetalInstanceDeletionStruct> ctx = new ArrayList<>();
        BaremetalInstanceDeletionStruct s = new BaremetalInstanceDeletionStruct();
        BaremetalInstanceVO bm = dbf.findByUuid(msg.getUuid(), BaremetalInstanceVO.class);
        s.setInventory(BaremetalInstanceInventory.valueOf(bm));
        s.setDeletionPolicy(deletionPolicyMgr.getDeletionPolicy(msg.getUuid()));
        ctx.add(s);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-baremetal-instance-%s", msg.getUuid()));
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

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                completion.success();
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(err(SysErrors.DELETE_RESOURCE_ERROR, errCode.getDetails()).withCause(errCode));
            }
        }).start();
    }

    private String getPxeBootMacAddress(String bmUuid) {
        return Q.New(BaremetalNicVO.class)
                .eq(BaremetalNicVO_.baremetalInstanceUuid, bmUuid)
                .eq(BaremetalNicVO_.pxe, true)
                .select(BaremetalNicVO_.mac)
                .limit(1)
                .findValue();
    }

    private Object getPxeBootIpAddress(String bmUuid) {
        return Q.New(BaremetalNicVO.class)
                .eq(BaremetalNicVO_.baremetalInstanceUuid, bmUuid)
                .eq(BaremetalNicVO_.pxe, true)
                .select(BaremetalNicVO_.ip)
                .limit(1)
                .findValue();
    }

    private void setupEvents() {
        evf.on(BaremetalInstanceCanonicalEvents.CREATE_NOVNC_NGINX_PROXY, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                cfgMgr.writeNoVNCProxy((String) data);
            }
        });

        evf.on(BaremetalInstanceCanonicalEvents.CREATE_TERMINAL_NGINX_PROXY, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                cfgMgr.writeTerminalProxy((String) data);
            }
        });

        evf.on(BaremetalInstanceCanonicalEvents.DELETE_NGINX_PROXY, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                cfgMgr.deleteNginxProxy((String) data);
            }
        });
    }

    private void populateExtensions() {
        for (BaremetalNicFactory f : pluginRgty.getExtensionList(BaremetalNicFactory.class)) {
            BaremetalNicFactory old = bmNicFactories.get(f.getType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate BaremetalNicFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getType()));
            }
            bmNicFactories.put(f.getType(), f);
        }
    }

    // TODO: no need to stop these actions when we have bm agent
    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
                APIDetachL2NetworkFromClusterMsg.class,
                APIDeleteL2NetworkMsg.class,
                APIDeleteL3NetworkMsg.class,
                APIDeleteIpRangeMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDetachL2NetworkFromClusterMsg) {
            validate((APIDetachL2NetworkFromClusterMsg) msg);
        } else if (msg instanceof APIDeleteL2NetworkMsg) {
            validate((APIDeleteL2NetworkMsg) msg);
        } else if (msg instanceof APIDeleteL3NetworkMsg) {
            validate((APIDeleteL3NetworkMsg) msg);
        } else if (msg instanceof APIDeleteIpRangeMsg) {
            validate((APIDeleteIpRangeMsg) msg);
        }
        return msg;
    }

    private void validate(APIDetachL2NetworkFromClusterMsg msg) {
        List<String> l3Uuids = getL3NetworkUuids(msg.getL2NetworkUuid());
        if (l3Uuids.isEmpty()) {
            return;
        }

        List<String> bmUuids = Q.New(BaremetalInstanceVO.class)
                .eq(BaremetalInstanceVO_.clusterUuid, msg.getClusterUuid())
                .select(BaremetalInstanceVO_.uuid)
                .listValues();
        if (bmUuids.isEmpty()) {
            return;
        }

        boolean exists = Q.New(BaremetalNicVO.class)
                .in(BaremetalNicVO_.baremetalInstanceUuid, bmUuids)
                .in(BaremetalNicVO_.l3NetworkUuid, l3Uuids)
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "there are bm instances using ip address allocated from l2[uuid:%s]", msg.getL2NetworkUuid()));
        }
    }

    private void validate(APIDeleteL2NetworkMsg msg) {
        List<String> l3Uuids = getL3NetworkUuids(msg.getL2NetworkUuid());
        if (l3Uuids.isEmpty()) {
            return;
        }

        boolean exists = Q.New(BaremetalNicVO.class)
                .in(BaremetalNicVO_.l3NetworkUuid, l3Uuids)
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "there are bm instances using ip address allocated from l2[uuid:%s]", msg.getL2NetworkUuid()));
        }
    }

    private List<String> getL3NetworkUuids(String l2NetworkUuid) {
        List<String> l3Uuids = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.l2NetworkUuid, l2NetworkUuid)
                .select(L3NetworkVO_.uuid)
                .listValues();

        String l2Type = Q.New(L2NetworkVO.class)
                .eq(L2NetworkVO_.uuid, l2NetworkUuid)
                .select(L2NetworkVO_.type)
                .findValue();

        for (L2NetworkOwnedL3ExtensionPoint ext : pluginRgty.getExtensionList(L2NetworkOwnedL3ExtensionPoint.class)) {
            if (Objects.equals(ext.getType().toString(), l2Type)) {
                l3Uuids.addAll(ext.getOwnedL3NetworkUuids(l2NetworkUuid));
            }
        }

        return l3Uuids;
    }

    private void validate(APIDeleteL3NetworkMsg msg) {
        boolean exists = Q.New(BaremetalNicVO.class).eq(BaremetalNicVO_.l3NetworkUuid, msg.getL3NetworkUuid()).isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "there are bm instances using ip address allocated from l3[uuid:%s]", msg.getL3NetworkUuid()));
        }
    }

    private void validate(APIDeleteIpRangeMsg msg) {
        List<String> usedUuids = Q.New(BaremetalNicVO.class)
                .notNull(BaremetalNicVO_.usedIpUuid)
                .select(BaremetalNicVO_.usedIpUuid)
                .groupBy(BaremetalNicVO_.usedIpUuid)
                .listValues();
        if (usedUuids.isEmpty()) {
            return;
        }

        boolean exists = Q.New(UsedIpVO.class)
                .in(UsedIpVO_.uuid, usedUuids)
                .eq(UsedIpVO_.ipRangeUuid, msg.getIpRangeUuid())
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "there are bm instances using ip address allocated from ip range[uuid:%s]", msg.getIpRangeUuid()));
        }
    }

    private String getSwitchSystemTags(List<String> tags, String mac) {
        String swInfo = null;
        try {
            if (tags == null || tags.size() == 0) {
                return null;
            }
            for (String tag : tags) {
                String[] items = tag.split("::");
                if (BaremetalInstanceSystemTags.SWITCH_INFO_TOKEN.equals(items[0])) {
                    if (items[1].equals(mac)) {
                        swInfo = items[2];
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("The switch system tag format is not currect.");
        }
        return swInfo;
    }
}
