package org.zstack.compute.bonding;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import static org.zstack.core.Platform.err;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.bonding.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.network.hostNetworkInterface.*;
import org.zstack.pciDevice.PciDeviceUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

public class HostNetworkBondingManagerImpl extends AbstractService implements HostNetworkBondingManager {
    private static final CLogger logger = Utils.getLogger(HostNetworkBondingManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;

    private Map<String, HostNetworkBondingFactory> hostNetworkBondingFactories = new HashMap<>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof  BondingMessage) {
            passThrough((BondingMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(BondingMessage msg) {
        HostNetworkBondingVO vo = dbf.findByUuid(msg.getBondingUuid(), HostNetworkBondingVO.class);
        if (vo == null) {
            bus.replyErrorByMessageType((Message) msg, err(SysErrors.RESOURCE_NOT_FOUND, "unable to find bonding[uuid=%s]", msg.getBondingUuid()));
            return;
        }

        HostNetworkBondingBase base = new HostNetworkBondingBase(vo);
        base.handleMessage((Message) msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateBondingMsg) {
            handle((APICreateBondingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateBondingMsg) {
            handle((CreateBondingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CreateBondingMsg msg) {
        CreateBondingReply reply = new CreateBondingReply();

        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("add-bonding-%s-to-host-%s", msg.getBondingName(), msg.getHostUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {;
                flow(new Flow() {
                    String __name__ = "write-bonding-to-db";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                // create bonding
                                HostNetworkBondingVO bondingVO = new HostNetworkBondingVO();
                                bondingVO.setUuid(Platform.getUuid());
                                bondingVO.setHostUuid(msg.getHostUuid());
                                bondingVO.setBondingName(msg.getBondingName());
                                bondingVO.setType(msg.getType());
                                bondingVO.setMode(msg.getMode());
                                bondingVO.setXmitHashPolicy(msg.getXmitHashPolicy());
                                bondingVO.setDescription(msg.getDescription());
                                bondingVO = dbf.persistAndRefresh(bondingVO);

                                // update interface
                                HostNetworkBondingInventory bondingInv = HostNetworkBondingInventory.valueOf(bondingVO);
                                Set<HostNetworkInterfaceVO> nicVOs = new HashSet<>();
                                if (msg.getSlaveUuids() != null) {
                                    List<HostNetworkInterfaceInventory> slaveInv = new ArrayList<>();
                                    if (msg.getSlaveUuids() != null && !msg.getSlaveUuids().isEmpty()) {
                                        for (String slaveUuid : msg.getSlaveUuids()) {
                                            HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(slaveUuid, HostNetworkInterfaceVO.class);
                                            if (interfaceVO != null) {
                                                interfaceVO.setBondingUuid(bondingVO.getUuid());
                                                interfaceVO.setInterfaceType(NetworkInterfaceType.bondingSlave.toString());
                                                nicVOs.add(interfaceVO);
                                                slaveInv.add(HostNetworkInterfaceInventory.valueOf(interfaceVO));

                                                PciDeviceUtils.updatePciDevicePassThroughStateFromAvailableToDisabled(
                                                        interfaceVO.getInterfaceName(), interfaceVO.getHostUuid());
                                            }
                                        }
                                        dbf.updateCollection(nicVOs);
                                    }
                                    bondingInv.setSlaves(slaveInv);
                                }

                                data.put(HostNetworkBondingConstant.Param.BONDINGINV.toString(), bondingInv);
                            }
                        }.execute();

                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        HostNetworkBondingInventory bondingInv = (HostNetworkBondingInventory)data.get(HostNetworkBondingConstant.Param.BONDINGINV.toString());

                        if (bondingInv != null) {
                            new SQLBatch() {
                                @Override
                                protected void scripts() {
                                    PciDeviceUtils.updatePciDevicePassThroughStateFromDisabledToAvailable(
                                            bondingInv.getSlaves().stream().map(HostNetworkInterfaceInventory::getUuid).distinct().collect(Collectors.toList()),
                                            bondingInv.getHostUuid());

                                    // update interface first
                                    SQL.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid())
                                            .eq(HostNetworkInterfaceVO_.bondingUuid, bondingInv.getUuid())
                                            .set(HostNetworkInterfaceVO_.interfaceType, NetworkInterfaceType.noMaster.toString())
                                            .set(HostNetworkInterfaceVO_.slaveActive, Boolean.FALSE)
                                            .update();

                                    // then delete bonding
                                    SQL.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.hostUuid, msg.getHostUuid())
                                            .eq(HostNetworkBondingVO_.uuid, bondingInv.getUuid())
                                            .hardDelete();

                                }
                            }.execute();
                        }

                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    @Override
                    public boolean skip(Map data) {
                        return msg.isDbOnly();
                    }

                    String __name__ = "apply-to-backend";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        HostNetworkBondingInventory bondingInv = (HostNetworkBondingInventory)data.get(HostNetworkBondingConstant.Param.BONDINGINV.toString());

                        HostNetworkBondingFactory factory = hostNetworkBondingFactories.get(msg.getType());
                        factory.createBonding(bondingInv, new Completion(trigger) {
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

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        HostNetworkBondingInventory bondingInv = (HostNetworkBondingInventory)data.get(HostNetworkBondingConstant.Param.BONDINGINV.toString());
                        bondingInv.getSlaves().forEach(slave ->
                                SQL.New(HostNetworkInterfaceServiceRefVO.class)
                                        .eq(HostNetworkInterfaceServiceRefVO_.interfaceUuid, slave.getUuid())
                                        .hardDelete()
                        );
                        reply.setInventory(bondingInv);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }

        }).start();
    }

    private void handle(APICreateBondingMsg msg) {
        APICreateBondingEvent event = new APICreateBondingEvent(msg.getId());
        Map<String, List<String>> slavesMap = msg.getSlavesMap();

        List<CreateBondingMsg> msgs = new ArrayList<CreateBondingMsg>();
        for (String hostUuid : msg.getHostUuids()) {
            CreateBondingMsg createBondingMsg = new CreateBondingMsg();
            createBondingMsg.setHostUuid(hostUuid);
            createBondingMsg.setBondingName(msg.getBondingName());
            createBondingMsg.setSlaveUuids(slavesMap.get(hostUuid));
            createBondingMsg.setType(msg.getType());
            createBondingMsg.setMode(msg.getMode());
            createBondingMsg.setXmitHashPolicy(msg.getXmitHashPolicy());
            createBondingMsg.setDescription(msg.getDescription());
            createBondingMsg.setAccountUuid(msg.getSession().getAccountUuid());

            bus.makeLocalServiceId(createBondingMsg, HostNetworkBondingConstant.SERVICE_ID);
            msgs.add(createBondingMsg);
        }

        List<ErrorCode> errors = Collections.synchronizedList(new LinkedList<ErrorCode>());
        List<HostNetworkBondingInventory> inventories = Collections.synchronizedList(new ArrayList<>());
        new While<CreateBondingMsg>(msgs).step((cmsg, whileCompletion) ->
            bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        CreateBondingReply cReply = reply.castReply();
                        inventories.add(cReply.getInventory());
                        whileCompletion.done();
                    } else {
                        errors.add(reply.getError());
                        whileCompletion.allDone();
                    }
                }
            }), 10).run(new WhileDoneCompletion(msg) {
        @Override
        public void done(ErrorCodeList errorCodeList) {
            if (errors.size() > 0) {
                event.setError(errors.get(0));
                bus.publish(event);
                return;
            }
            event.setInventory(inventories);
            bus.publish(event);
        }
    });
}

    @Override
    public HostNetworkBondingFactory getHostNetworkBondingFactory(String type) {
        HostNetworkBondingFactory f = hostNetworkBondingFactories.get(type);
        if (f == null) {
            throw new CloudRuntimeException(String.format("cannot find HostNetworkBondingFactory[type:%s]", type));
        }
        return f;
    }

    private void populateExtensions() {
        for (HostNetworkBondingFactory factory : pluginRgty.getExtensionList(HostNetworkBondingFactory.class)) {
            String type = factory.getType().toString();
            HostNetworkBondingFactory old = hostNetworkBondingFactories.get(type);
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate HostNetworkBondingBackend[%s, %s] for type[%s]",
                        factory.getClass().getName(), old.getClass().getName(), type));
            }
            hostNetworkBondingFactories.put(type, factory);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(HostNetworkBondingConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
