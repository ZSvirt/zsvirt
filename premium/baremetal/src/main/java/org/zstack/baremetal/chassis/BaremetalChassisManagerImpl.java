package org.zstack.baremetal.chassis;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.baremetal.BaremetalUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.baremetal.chassis.*;
import org.zstack.header.baremetal.instance.BaremetalInstanceState;
import org.zstack.header.baremetal.instance.BaremetalInstanceVO;
import org.zstack.header.baremetal.instance.BaremetalInstanceVO_;
import org.zstack.header.baremetal.network.BaremetalBondingVO;
import org.zstack.header.baremetal.network.BaremetalBondingVO_;
import org.zstack.header.baremetal.network.BaremetalNicInfoStruct;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerConstant;
import org.zstack.header.baremetal.pxeserver.CreateBaremetalDhcpConfigMsg;
import org.zstack.header.baremetal.pxeserver.DeleteBaremetalDhcpConfigMsg;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.longjob.LongJobConstants;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.longjob.SubmitLongJobReply;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.IpRangeSet;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.form.Form;
import org.zstack.utils.function.ValidateFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.verify.ParamValidator;
import org.zstack.utils.verify.Verifiable;

import javax.persistence.PersistenceException;
import java.lang.reflect.Type;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;

import static org.zstack.core.Platform.*;
import static org.zstack.longjob.LongJobUtils.jobCanceled;

/**
 * Created by GuoYi on 2017/3/23.
 */
public class BaremetalChassisManagerImpl extends AbstractService implements BaremetalChassisManager {
    private static final CLogger logger = Utils.getLogger(BaremetalChassisManagerImpl.class);

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected EventFacade evf;
    @Autowired
    protected BaremetalChassisApiInterceptor chassisValidator;

    private static final int BAREMETAL_CHASSIS_SYNC_LEVEL = 10;
    private static final String baremetalModuleName = "baremetal";

    @Override
    public boolean start() {
        restf.registerSyncHttpCallHandler(BaremetalChassisConstant.SEND_HARDWARE_INFO,
                BaremetalChassisCommands.SendHardwareInfoCmd.class,
                cmd -> {
                    handleSendHardwareInfo(cmd);
                    return null;
                });
        return true;
    }

    private void handleSendHardwareInfo(BaremetalChassisCommands.SendHardwareInfoCmd cmd) {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return BaremetalChassisConstant.SYNC_SIGNATURE_OF_BAREMETAL_CHASSIS + cmd.ipmiAddress + "-" + cmd.ipmiPort;
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (StringUtils.isBlank(cmd.ipmiAddress)) {
                    logger.error("received no ipmi address from the chassis, which is unlikely");
                    chain.next();
                    return;
                }

                List<String> addrs = Arrays.asList(cmd.ipmiAddress.split("\\|"));
                if (addrs.size() > 1) {
                    logger.error("chassis with multiple ipmi addresses is not allowed, " +
                            "please try to change the bios setting and fix it");
                    chain.next();
                    return;
                }

                BaremetalChassisVO chassis = Q.New(BaremetalChassisVO.class)
                        .eq(BaremetalChassisVO_.ipmiAddress, cmd.ipmiAddress)
                        .eq(BaremetalChassisVO_.ipmiPort, cmd.ipmiPort)
                        .find();
                if (chassis == null) {
                    logger.error(String.format(
                            "The %s hardware info belongs to chassis[ipmiaddr:%s, ipmiport:%s], which not exists",
                            cmd.type, cmd.ipmiAddress, cmd.ipmiPort)
                    );
                    chain.next();
                    return;
                }

                logger.debug(String.format("received %s hardware info from chassis[uuid:%s]: %s", cmd.type, chassis.getUuid(), cmd.content));
                if (cmd.type.equals(BaremetalChassisConstant.BAREMETAL_HARDWARE_INFO_PXESERVER_TYPE)
                        && StringDSL.isZStackUuid(cmd.content)) {
                    if (chassis.getPxeServerUuid() == null) {
                        chassis.setPxeServerUuid(cmd.content);
                        dbf.updateAndRefresh(chassis);
                    } else if (!chassis.getPxeServerUuid().equals(cmd.content)) {
                        // report error when wrong pxeserver handled chassis's dhcp request
                        SQL.New(BaremetalHardwareInfoVO.class)
                                .eq(BaremetalHardwareInfoVO_.chassisUuid, chassis.getUuid())
                                .delete();
                        chassis.setStatus(BaremetalChassisStatus.HWInfoUnknown);
                        dbf.updateAndRefresh(chassis);
                        throw new OperationFailureException(operr(
                                "baremetal chassis[uuid:%s] is supposed to using pxeserver[uuid:%s], but it was pxeserver[uuid:%s] that actually handled the DHCP request",
                                chassis.getUuid(), chassis.getPxeServerUuid(), cmd.content
                        ));
                    }

                    // update chassis status
                    BaremetalChassisVO bmc = dbf.findByUuid(chassis.getUuid(), BaremetalChassisVO.class);
                    bmc.setState(BaremetalChassisState.Enabled);
                    if (Q.New(BaremetalInstanceVO.class).eq(BaremetalInstanceVO_.chassisUuid, bmc.getUuid()).isExists()) {
                        bmc.setStatus(BaremetalChassisStatus.Allocated);
                    } else {
                        bmc.setStatus(BaremetalChassisStatus.Available);
                    }
                    dbf.update(bmc);

                    // create dhcp config for pxe boot nic
                    String nicContent = Q.New(BaremetalHardwareInfoVO.class)
                            .eq(BaremetalHardwareInfoVO_.chassisUuid, chassis.getUuid())
                            .eq(BaremetalHardwareInfoVO_.type, BaremetalChassisConstant.BAREMETAL_HARDWARE_INFO_NIC_TYPE)
                            .select(BaremetalHardwareInfoVO_.content)
                            .findValue();
                    if (nicContent.isEmpty()) {
                        logger.error(String.format("cannot find pxe boot nic info of chassis[uuid:%s]", chassis.getUuid()));
                        chain.next();
                        return;
                    }

                    CreateBaremetalDhcpConfigMsg cmsg = new CreateBaremetalDhcpConfigMsg();
                    cmsg.setPxeServerUuid(chassis.getPxeServerUuid());
                    cmsg.setChassisUuid(chassis.getUuid());

                    Type listType = new TypeToken<ArrayList<BaremetalNicInfoStruct>>(){}.getType();
                    List<BaremetalNicInfoStruct> structs = new Gson().fromJson(nicContent, listType);
                    for (BaremetalNicInfoStruct struct : structs) {
                        if (struct.getPxe()) {
                            cmsg.setPxeNicMac(struct.getMac());
                            cmsg.setPxeNicIp(struct.getIp());
                            break;
                        }
                    }
                    bus.makeTargetServiceIdByResourceUuid(cmsg, BaremetalPxeServerConstant.SERVICE_ID, chassis.getPxeServerUuid());
                    bus.send(cmsg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.debug(String.format(
                                        "successfully created dhcp config for chassis[uuid:%s] in pxeserver[uuid:%s]",
                                        chassis.getUuid(), chassis.getPxeServerUuid()
                                ));
                            } else {
                                logger.error(String.format(
                                        "failed to create dhcp config for chassis[uuid:%s] in pxeserver[uuid:%s]",
                                        chassis.getUuid(), chassis.getPxeServerUuid()
                                ));
                            }

                            chain.next();
                        }
                    });
                } else {
                    new SQLBatch() {
                        @Override
                        protected void scripts() {
                            // check existence
                            BaremetalHardwareInfoVO info =  q(BaremetalHardwareInfoVO.class)
                                    .eq(BaremetalHardwareInfoVO_.chassisUuid, chassis.getUuid())
                                    .eq(BaremetalHardwareInfoVO_.type, cmd.type)
                                    .find();

                            if (info == null) {
                                info = new BaremetalHardwareInfoVO();
                                info.setUuid(Platform.getUuid());
                                info.setChassisUuid(chassis.getUuid());
                                info.setType(cmd.type);
                                info.setContent(cmd.content);
                                persist(info);
                            } else if (!info.getContent().equals(cmd.content)){
                                info.setContent(cmd.content);
                                merge(info);
                            }
                        }
                    }.execute();

                    logger.info(String.format("Successfully re-created %s hardware info of chassis[uuid:%s] " +
                            "using the hardware info been sent.", cmd.type, chassis.getUuid()));
                    chain.next();
                }
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(BaremetalChassisConstant.SERVICE_ID);
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
        if (msg instanceof APICreateBaremetalChassisMsg) {
            handle((APICreateBaremetalChassisMsg) msg);
        } else if (msg instanceof APIUpdateBaremetalChassisMsg) {
            handle((APIUpdateBaremetalChassisMsg) msg);
        } else if (msg instanceof APIDeleteBaremetalChassisMsg) {
            handle((APIDeleteBaremetalChassisMsg) msg);
        } else if (msg instanceof APIChangeBaremetalChassisStateMsg) {
            handle((APIChangeBaremetalChassisStateMsg) msg);
        } else if (msg instanceof APIPowerOnBaremetalChassisMsg) {
            handle((APIPowerOnBaremetalChassisMsg) msg);
        } else if (msg instanceof APIPowerOffBaremetalChassisMsg) {
            handle((APIPowerOffBaremetalChassisMsg) msg);
        } else if (msg instanceof APIPowerResetBaremetalChassisMsg) {
            handle((APIPowerResetBaremetalChassisMsg) msg);
        } else if (msg instanceof APIGetBaremetalChassisPowerStatusMsg) {
            handle((APIGetBaremetalChassisPowerStatusMsg) msg);
        } else if (msg instanceof APIInspectBaremetalChassisMsg) {
            handle((APIInspectBaremetalChassisMsg) msg);
        } else if (msg instanceof APICheckBaremetalChassisConfigFileMsg) {
            handle((APICheckBaremetalChassisConfigFileMsg) msg);
        } else if (msg instanceof APIBatchCreateBaremetalChassisMsg) {
            handle((APIBatchCreateBaremetalChassisMsg) msg);
        } else if (msg instanceof APICleanUpBaremetalChassisBondingMsg) {
            handle((APICleanUpBaremetalChassisBondingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DeleteBaremetalChassisMsg) {
            handle((DeleteBaremetalChassisMsg) msg);
        } else if (msg instanceof CreateBaremetalChassisMsg) {
            handle((CreateBaremetalChassisMsg) msg);
        } else if (msg instanceof BatchCreateBaremetalChassisMsg) {
            handle((BatchCreateBaremetalChassisMsg) msg);
        } else if (msg instanceof InspectBaremetalChassisMsg) {
            handle((InspectBaremetalChassisMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateBaremetalChassisMsg msg) {
        final APICreateBaremetalChassisEvent evt = new APICreateBaremetalChassisEvent(msg.getId());

        CreateBaremetalChassisMsg cmsg = CreateBaremetalChassisMsg.valueOf(msg);
        bus.makeLocalServiceId(cmsg, BaremetalChassisConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    CreateBaremetalChassisReply rly = reply.castReply();
                    evt.setInventory(rly.getInventory());
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIUpdateBaremetalChassisMsg msg) {
        final APIUpdateBaremetalChassisEvent evt = new APIUpdateBaremetalChassisEvent(msg.getId());
        BaremetalChassisVO bmc = dbf.findByUuid(msg.getUuid(), BaremetalChassisVO.class);
        if (msg.getName() != null)
            bmc.setName(msg.getName());
        if (msg.getDescription() != null)
            bmc.setDescription(msg.getDescription());
        if (msg.getIpmiAddress() != null)
            bmc.setIpmiAddress(msg.getIpmiAddress());
        if (msg.getIpmiPort() != null)
            bmc.setIpmiPort(msg.getIpmiPort());
        if (msg.getIpmiUsername() != null)
            bmc.setIpmiUsername(msg.getIpmiUsername());
        if (msg.getIpmiPassword() != null)
            bmc.setIpmiPassword(msg.getIpmiPassword());
        bmc = dbf.updateAndRefresh(bmc);
        evt.setInventory(BaremetalChassisInventory.valueOf(bmc));
        bus.publish(evt);
    }

    private void handle(APIDeleteBaremetalChassisMsg msg) {
        final APIDeleteBaremetalChassisEvent evt = new APIDeleteBaremetalChassisEvent(msg.getId());

        final String issuer = BaremetalChassisVO.class.getSimpleName();
        final BaremetalChassisVO chassis = dbf.findByUuid(msg.getUuid(), BaremetalChassisVO.class);
        final List<BaremetalChassisInventory> ctx = Collections.singletonList(BaremetalChassisInventory.valueOf(chassis));

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-baremetal-chassis-%s", msg.getUuid()));
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

    private void handle(APIChangeBaremetalChassisStateMsg msg) {
        final APIChangeBaremetalChassisStateEvent evt = new APIChangeBaremetalChassisStateEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return BaremetalChassisConstant.SYNC_SIGNATURE_OF_BAREMETAL_CHASSIS + msg.getUuid();
            }

            @Override
            public void run(final SyncTaskChain chain) {
                BaremetalChassisVO chassis = dbf.findByUuid(msg.getUuid(), BaremetalChassisVO.class);
                chassis.setState(chassis.getState().nextState(BaremetalChassisStateEvent.valueOf(msg.getStateEvent())));
                evt.setInventory(BaremetalChassisInventory.valueOf(dbf.updateAndRefresh(chassis)));
                bus.publish(evt);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public int getSyncLevel() {
                return BAREMETAL_CHASSIS_SYNC_LEVEL;
            }
        });
    }

    private void handle(APIPowerOnBaremetalChassisMsg msg) {
        final APIPowerOnBaremetalChassisEvent evt = new APIPowerOnBaremetalChassisEvent(msg.getId());
        final BaremetalChassisVO bmc = dbf.findByUuid(msg.getChassisUuid(), BaremetalChassisVO.class);
        powerOnBaremetalChassis(bmc, new Completion(evt) {
            @Override
            public void success() {
                updateBaremetalInstanceState(msg.getChassisUuid(), BaremetalInstanceState.Starting);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIPowerOffBaremetalChassisMsg msg) {
        final APIPowerOffBaremetalChassisEvent evt = new APIPowerOffBaremetalChassisEvent(msg.getId());
        final BaremetalChassisVO bmc = dbf.findByUuid(msg.getChassisUuid(), BaremetalChassisVO.class);
        powerOffBaremetalChassis(bmc, new Completion(evt) {
            @Override
            public void success() {
                updateBaremetalInstanceState(msg.getChassisUuid(), BaremetalInstanceState.Stopped);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIPowerResetBaremetalChassisMsg msg) {
        final APIPowerResetBaremetalChassisEvent evt = new APIPowerResetBaremetalChassisEvent(msg.getId());
        final BaremetalChassisVO bmc = dbf.findByUuid(msg.getChassisUuid(), BaremetalChassisVO.class);
        powerResetBaremetalChassis(bmc, new Completion(evt) {
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

    private void handle(APIGetBaremetalChassisPowerStatusMsg msg) {
        final APIGetBaremetalChassisPowerStatusReply reply =  new APIGetBaremetalChassisPowerStatusReply();
        final BaremetalChassisVO bmc = dbf.findByUuid(msg.getUuid(), BaremetalChassisVO.class);
        reply.setStatus(BaremetalUtils.getServerPowerStatus(bmc.toInventory()));

        // FIXME: use baremetal instance agent to report instance power status
        if (reply.getStatus().equals(BaremetalChassisConstant.SERVER_POWER_ON)) {
            updateBaremetalInstanceState(msg.getUuid(), BaremetalInstanceState.Running);
        } else if (reply.getStatus().equals(BaremetalChassisConstant.SERVER_POWER_OFF)){
            updateBaremetalInstanceState(msg.getUuid(), BaremetalInstanceState.Stopped);
        } else if (reply.getStatus().equals(BaremetalChassisConstant.SERVER_POWER_UNKNOWN)) {
            updateBaremetalInstanceState(msg.getUuid(), BaremetalInstanceState.UNKNOWN);
        }
        bus.reply(msg, reply);
    }

    private void handle(APIInspectBaremetalChassisMsg msg) {
        final APIInspectBaremetalChassisEvent evt = new APIInspectBaremetalChassisEvent(msg.getId());
        InspectBaremetalChassisMsg imsg = new InspectBaremetalChassisMsg();
        imsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(imsg, BaremetalChassisConstant.SERVICE_ID, msg.getUuid());
        bus.send(imsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    InspectBaremetalChassisReply rly = reply.castReply();
                    evt.setInventory(rly.getInventory());
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(DeleteBaremetalChassisMsg msg) {
        DeleteBaremetalChassisReply rly = new DeleteBaremetalChassisReply();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-baremetal-chassis-%s", msg.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-dhcp-config-in-pxeserver-if-needed";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String pxeServerUuid = Q.New(BaremetalChassisVO.class)
                        .eq(BaremetalChassisVO_.uuid, msg.getUuid())
                        .select(BaremetalChassisVO_.pxeServerUuid)
                        .findValue();
                if (pxeServerUuid == null) {
                    trigger.next();
                    return;
                }

                DeleteBaremetalDhcpConfigMsg dmsg = new DeleteBaremetalDhcpConfigMsg();
                dmsg.setChassisUuid(msg.getUuid());
                dmsg.setPxeServerUuid(pxeServerUuid);
                bus.makeTargetServiceIdByResourceUuid(dmsg, BaremetalPxeServerConstant.SERVICE_ID, pxeServerUuid);
                bus.send(dmsg, new CloudBusCallBack(rly) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format(
                                    "failed to delete dhcp config in pxeserver[uuid:%s] for chassis[uuid:%s]",
                                    pxeServerUuid, msg.getUuid()
                            ));
                        }
                        trigger.next();
                    }
                });
            }
        });

        chain.done(new FlowDoneHandler(rly) {
            @Override
            public void handle(Map data) {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        BaremetalChassisVO bmc = findByUuid(msg.getUuid(), BaremetalChassisVO.class);
                        sql(BaremetalHardwareInfoVO.class)
                                .eq(BaremetalHardwareInfoVO_.chassisUuid, bmc.getUuid())
                                .delete();
                        remove(bmc);
                    }
                }.execute();
                logger.info("deleted baremetal chassis " + msg.getUuid());
                bus.reply(msg, rly);
            }
        }).error(new FlowErrorHandler(rly) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                rly.setError(operr("failed to delete baremetal chassis %s", msg.getUuid()));
                bus.reply(msg, rly);
            }
        }).start();
    }

    private BaremetalChassisVO createBaremetalChassisVO(final CreateBaremetalChassisMsg msg) {
        BaremetalChassisVO bmc = new BaremetalChassisVO();
        bmc.setUuid(Platform.getUuid());
        bmc.setName(msg.getName());
        bmc.setDescription(msg.getDescription());
        bmc.setIpmiAddress(msg.getIpmiAddress());
        bmc.setIpmiPort(msg.getIpmiPort());
        bmc.setIpmiUsername(msg.getIpmiUsername());
        bmc.setIpmiPassword(msg.getIpmiPassword());
        bmc.setZoneUuid(Q.New(ClusterVO.class)
                .eq(ClusterVO_.uuid, msg.getClusterUuid())
                .select(ClusterVO_.zoneUuid)
                .findValue()
        );
        bmc.setClusterUuid(msg.getClusterUuid());
        bmc.setState(BaremetalChassisState.Enabled);
        bmc.setStatus(BaremetalChassisStatus.HWInfoUnknown);
        try {
            dbf.persistAndRefresh(bmc);
        } catch (PersistenceException e) {
            if (ExceptionDSL.isCausedBy(e, SQLIntegrityConstraintViolationException.class,
                    Arrays.asList("Duplicate entry", "ukBaremetalChassisVO"))) {
                throw new CloudRuntimeException(
                        String.format(
                                "Baremetal Chassis of IPMI address %s and IPMI port %d has already been created.",
                                msg.getIpmiAddress(),
                                msg.getIpmiPort()),
                        e
                );
            } else {
                throw e;
            }
        }

        if (msg.getReboot()) {
            getHardwareInfo(bmc, new Completion(null) {
                @Override
                public void success() {
                    logger.debug(String.format(
                            "successfully power reset baremetal chassis whose ipmiAddress:ipmiPort is %s:%s",
                            msg.getIpmiAddress(), msg.getIpmiPort()
                    ));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.error(String.format(
                            "failed to power reset baremetal chassis whose ipmiAddress:ipmiPort is %s:%s because %s",
                            msg.getIpmiAddress(), msg.getIpmiPort(), errorCode.getDetails()
                    ));
                }
            });
        }
        return bmc;
    }

    private void powerOnBaremetalChassis(BaremetalChassisVO bmc, Completion completion) {
        if (BaremetalUtils.powerOnRemoteServer(bmc.toInventory())) {
            completion.success();
        } else {
            completion.fail(operr("Failed to remotely power on baremetal chassis[uuid:%s]", bmc.getUuid()));
        }
    }

    private void powerOffBaremetalChassis(BaremetalChassisVO bmc, Completion completion) {
        if (BaremetalUtils.powerOffRemoteServer(bmc.toInventory())) {
            completion.success();
        } else {
            completion.fail(operr("Failed to remotely power off baremetal chassis[uuid:%s]", bmc.getUuid()));
        }
    }

    private void powerResetBaremetalChassis(BaremetalChassisVO bmc, Completion completion) {
        if (BaremetalUtils.powerResetRemoteServer(bmc.toInventory())) {
            completion.success();
        } else {
            completion.fail(operr("Failed to remotely power reset baremetal chassis[uuid:%s]", bmc.getUuid()));
        }
    }

    private void getHardwareInfo(BaremetalChassisVO bmc, Completion completion) {
        if (BaremetalUtils.powerResetRemoteServer(bmc.toInventory(), BaremetalChassisConstant.SERVER_BOOT_DEV_PXE)) {
            bmc = dbf.reload(bmc);
            bmc.setStatus(BaremetalChassisStatus.PxeBooting);
            dbf.update(bmc);
            completion.success();
        } else {
            completion.fail(operr("Failed to remotely pxe boot chassis[uuid:%s]", bmc.getUuid()));
        }
    }

    private void updateBaremetalInstanceState(String chassisUuid, BaremetalInstanceState state) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                String bmUuid = q(BaremetalInstanceVO.class)
                        .eq(BaremetalInstanceVO_.chassisUuid, chassisUuid)
                        .select(BaremetalInstanceVO_.uuid)
                        .findValue();
                if (bmUuid != null) {
                    BaremetalInstanceVO bm = findByUuid(bmUuid, BaremetalInstanceVO.class);
                    // update bm state only when it's running or stopped or unknown
                    if (bm.getState() == BaremetalInstanceState.Running
                            || bm.getState() == BaremetalInstanceState.Stopped
                            || bm.getState() == BaremetalInstanceState.UNKNOWN) {
                        bm.setState(state);
                    }
                    merge(bm);
                }
            }
        }.execute();
    }

    // check HWInfo every minute for 10 minutes
    @AsyncThread
    private void checkHwInfo(final String chassisUuid) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        final long current = System.currentTimeMillis();
        final long timeout = BaremetalChassisGlobalConfig.GET_CHASSIS_HW_INFO_TIMEOUT.value(Integer.class) * 60 * 1000L;
        final long expiredTime = current + timeout;
        final long interval = 10 * 1000L;

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            long count = current;

            @Override
            public void run() {
                boolean hwInfoExists = Q.New(BaremetalHardwareInfoVO.class)
                        .eq(BaremetalHardwareInfoVO_.chassisUuid, chassisUuid)
                        .isExists();

                if (hwInfoExists) {
                    timer.cancel();
                    return;
                }

                if (count >= expiredTime) {
                    logger.error(String.format(
                            "Failed to get Hardware Info of chassis %s. " +
                                    "It's most likely that chassis cannot get dhcp response from the pxe server. " +
                                    "Please check your network configuration.", chassisUuid
                    ));
                    BaremetalChassisVO bmc = dbf.findByUuid(chassisUuid, BaremetalChassisVO.class);
                    bmc.setStatus(BaremetalChassisStatus.PxeBootFailed);
                    dbf.update(bmc);
                    timer.cancel();
                }
                count += interval;
            }
        }, 0, interval);
    }

    private void handle(APICheckBaremetalChassisConfigFileMsg msg) {
        APICheckBaremetalChassisConfigFileReply reply = new APICheckBaremetalChassisConfigFileReply();
        buildCreateBaremetalChassisMsgs(msg.getBaremetalChassisInfo(), ParamValidator::validate);
        bus.reply(msg, reply);
    }

    private void handle(APIBatchCreateBaremetalChassisMsg msg) {
        APIBatchCreateBaremetalChassisEvent evt = new APIBatchCreateBaremetalChassisEvent(msg.getId());

        BatchCreateBaremetalChassisMsg cmsg = new BatchCreateBaremetalChassisMsg();
        cmsg.setBaremetalChassisInfo(msg.getBaremetalChassisInfo());

        SubmitLongJobMsg smsg = new SubmitLongJobMsg();
        smsg.setName(msg.getLongJobName());
        smsg.setDescription(msg.getLongJobDescription());
        smsg.setJobName(APIBatchCreateBaremetalChassisMsg.class.getSimpleName());
        smsg.setJobData(JSONObjectUtil.toJsonString(cmsg));
        smsg.setAccountUuid(msg.getSession().getAccountUuid());
        bus.makeLocalServiceId(smsg, LongJobConstants.SERVICE_ID);
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                SubmitLongJobReply rly = reply.castReply();
                evt.setInventory(rly.getInventory());
                bus.publish(evt);
            }
        });
    }

    private void handle(APICleanUpBaremetalChassisBondingMsg msg) {
        APICleanUpBaremetalChassisBondingEvent evt = new APICleanUpBaremetalChassisBondingEvent(msg.getId());
        SQL.New(BaremetalBondingVO.class)
                .eq(BaremetalBondingVO_.chassisUuid, msg.getChassisUuid())
                .delete();
        bus.publish(evt);
    }

    private void handle(CreateBaremetalChassisMsg msg) {
        final CreateBaremetalChassisReply reply = new CreateBaremetalChassisReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "create-baremetal-chassis-with-license-checking";
            }

            @Override
            public void run(SyncTaskChain chain) {
                ErrorCode err = chassisValidator.validate(msg);
                if (err != null) {
                    reply.setError(err);
                } else {
                    reply.setInventory(BaremetalChassisInventory.valueOf(createBaremetalChassisVO(msg)));
                }
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(BatchCreateBaremetalChassisMsg msg) {
        BatchCreateBaremetalChassisReply reply = new BatchCreateBaremetalChassisReply();
        batchCreateBaremetalChassis(msg, new ReturnValueCompletion<List<CreateBaremetalChassisResult>>(msg) {
            @Override
            public void success(List<CreateBaremetalChassisResult> results) {
                reply.setResults(results);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(InspectBaremetalChassisMsg msg) {
        final InspectBaremetalChassisReply reply = new InspectBaremetalChassisReply();
        final BaremetalChassisVO bmc = dbf.findByUuid(msg.getUuid(), BaremetalChassisVO.class);

        // check ipmi connection before provision
        if (!BaremetalUtils.isIpmiServerReachable(bmc.toInventory())) {
            reply.setError(operr("failed to connect to chassis [uuid:%s], please check ipmi connection.", bmc.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        // delete old baremetal hardware info
        SQL.New(BaremetalHardwareInfoVO.class)
                .eq(BaremetalHardwareInfoVO_.chassisUuid, msg.getUuid())
                .hardDelete();

        // make sure hardware info is available before provision
        checkHwInfo(bmc.getUuid());

        getHardwareInfo(bmc, new Completion(reply) {
            @Override
            public void success() {
                reply.setInventory(BaremetalChassisInventory.valueOf(dbf.reload(bmc)));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void batchCreateBaremetalChassis(BatchCreateBaremetalChassisMsg msg, ReturnValueCompletion<List<CreateBaremetalChassisResult>> completion) {
        List<CreateBaremetalChassisResult> results = Collections.synchronizedList(new ArrayList<>());
        List<CreateBaremetalChassisMsg> cmsgs = buildCreateBaremetalChassisMsgs(msg.getBaremetalChassisInfo(), null);

        new While<>(cmsgs)
                .enableProgressReport("create-bare-metal-chassis")
                .each((cmsg, coml) -> {
            if (jobCanceled()) {
                logger.debug("add host job has been canceled, abort rest hosts");
                coml.allDone();
            }

            ErrorCode err = validate(cmsg);
            if (err != null) {
                results.add(new CreateBaremetalChassisResult(cmsg.getIpmiAddress(), cmsg.getIpmiPort(), err));
                coml.done();
                return;
            }

            bus.makeLocalServiceId(cmsg, BaremetalChassisConstant.SERVICE_ID);
            bus.send(cmsg, new CloudBusCallBack(coml) {
                @Override
                public void run(MessageReply reply) {
                    CreateBaremetalChassisResult result = new CreateBaremetalChassisResult(
                            cmsg.getIpmiAddress(), cmsg.getIpmiPort(), reply.getError()
                    );
                    results.add(result);

                    // report job progress
                    coml.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                results.sort(Comparator.comparing(CreateBaremetalChassisResult::getIpmiAddress));
                completion.success(results);
            }
        });
    }

    private List<CreateBaremetalChassisMsg> buildCreateBaremetalChassisMsgs(
            String content, ValidateFunction<CreateBaremetalChassisMsg> validator) {
        try {
            int limit = BaremetalChassisGlobalConfig.BATCH_CREATE_CHASSIS_MAX_NUMBER.value(Integer.class);
            Form<CreateBaremetalChassisMsg> form = Form.New(CreateBaremetalChassisMsg.class, content, limit)
                    .addHeaderConverter(head -> (head.matches(".*\\(.*\\).*") ? head.split("[()]")[1] : head).replaceAll("\\*", ""))
                    .addColumnConverter("ipmiAddress", it  -> IpRangeSet.listAllIps(it, limit), CreateBaremetalChassisMsg::setIpmiAddress)
                    .addColumnConverter("reboot", CreateBaremetalChassisMsg::setReboot);
            return form.withValidator(validator).load();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new OperationFailureException(argerr("fail to load chassis info from file, because: %s", e.getMessage()));
        }
    }

    private ErrorCode validate(Verifiable msg) {
        try {
            ParamValidator.validate(msg);
        } catch (Exception e) {
            return argerr(e.getMessage());
        }
        return null;
    }
}
