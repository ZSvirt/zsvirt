package org.zstack.network.l2.virtualSwitch;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.L2NetworkCreateExtensionPoint;
import org.zstack.header.network.l2.L2NetworkFactory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.network.l3.*;
import org.zstack.network.l2.virtualSwitch.header.*;
import org.zstack.network.l3.L3BasicNetworkFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.err;

public class PortGroupFactory extends L3BasicNetworkFactory {
    private static final L3NetworkType type = new L3NetworkType.L3NetworkTypeBuilder()
            .typeName(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE)
            .mandatoryIpAllocation(false)
            .build();
    private static final CLogger logger = Utils.getLogger(PortGroupFactory.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected ThreadFacade thdf;

    @Override
    public L3NetworkType getType() {
        return type;
    }

    private String generateL2PortGroupName(String vSwitchUuid, int vlan) {
        if (vlan != 0) {
            return String.format("L2PortGroup-vlan-%s-for-vSwitch-%s", vlan, vSwitchUuid);
        }
        return String.format("L2PortGroup-noVlan-for-vSwitch-%s", vSwitchUuid);
    }

    @Override
    public void createL3Network(L3NetworkVO l3vo, APICreateL3NetworkMsg msg, ReturnValueCompletion<L3NetworkInventory> completion) {
        PortGroupVO pg = new PortGroupVO(l3vo);
        APICreatePortGroupMsg cmsg = (APICreatePortGroupMsg) msg;
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                doCreatePortGroup(pg, cmsg, new ReturnValueCompletion<L3NetworkInventory>(msg) {
                    @Override
                    public void success(L3NetworkInventory returnValue) {
                        completion.success(returnValue);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format(VirtualSwitchConstant.PORT_GROUP_SYNC_ID_FORMAT, cmsg.getvSwitchUuid(), cmsg.getVlan());
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });



    }

    private void doCreatePortGroup(PortGroupVO pg, APICreatePortGroupMsg msg, ReturnValueCompletion<L3NetworkInventory> completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("create-port-group-on-vSwitch-%s-vlan-%s", msg.getvSwitchUuid(), msg.getVlan()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "get-l2-port-group-if-exist";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (msg.getL2NetworkUuid() != null) {
                    trigger.next();
                    return;
                }

                String zoneUuid = Q.New(L2VirtualSwitchNetworkVO.class)
                        .eq(L2VirtualSwitchNetworkVO_.uuid, msg.getvSwitchUuid())
                        .select(L2VirtualSwitchNetworkVO_.zoneUuid)
                        .findValue();
                pg.setZoneUuid(zoneUuid);

                Q q = Q.New(L2PortGroupNetworkVO.class)
                        .eq(L2PortGroupNetworkVO_.vSwitchUuid, msg.getvSwitchUuid())
                        .eq(L2PortGroupNetworkVO_.vlanId, msg.getVlan())
                        .eq(L2PortGroupNetworkVO_.vlanMode, PortGroupVlanMode.valueOf(msg.getVlanMode()))
                        .select(L2PortGroupNetworkVO_.uuid);

                String l2Uuid;
                if (msg.getVlanRanges() == null) {
                    l2Uuid = q.isNull(L2PortGroupNetworkVO_.vlanRanges).findValue();
                } else {
                    l2Uuid = q.eq(L2PortGroupNetworkVO_.vlanRanges, msg.getVlanRanges()).findValue();
                }

                pg.setL2NetworkUuid(l2Uuid);
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "create-l2-port-group-if-not-exist";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (pg.getL2NetworkUuid() != null) {
                    trigger.next();
                    return;
                }

                // fake API message, only use for factory to create L2PortGroupNetworkVO
                APICreateL2PortGroupMsg l2Msg = new APICreateL2PortGroupMsg();
                l2Msg.setvSwitchUuid(msg.getvSwitchUuid());
                l2Msg.setVlan(msg.getVlan());
                l2Msg.setVlanMode(msg.getVlanMode());
                l2Msg.setVlanRanges(msg.getVlanRanges());

                List<L2NetworkCreateExtensionPoint> createExtensions = pluginRgty.getExtensionList(L2NetworkCreateExtensionPoint.class);
                for (L2NetworkCreateExtensionPoint extp : createExtensions) {
                    try {
                        extp.beforeCreateL2Network(l2Msg);
                    } catch (NetworkException e) {
                        ErrorCode err = err(SysErrors.CREATE_RESOURCE_ERROR, "unable to create l2network[name:%s, type:%s], %s", msg.getName(), msg.getType(), e.getMessage());
                        logger.warn(err.getDetails(), e);
                        trigger.fail(err);
                        return;
                    }
                }

                L2PortGroupNetworkFactory factory = null;
                for (L2NetworkFactory f : pluginRgty.getExtensionList(L2NetworkFactory.class)) {
                    if (f.getType().equals(L2NetworkType.valueOf(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE))) {
                        factory = (L2PortGroupNetworkFactory) f;
                    }
                }

                if (factory == null) {
                    trigger.fail(Platform.inerr("there is no L2PortGroupNetworkFactory, please check!"));
                    return;
                }

                L2PortGroupNetworkVO vo = new L2PortGroupNetworkVO();
                vo.setUuid(Platform.getUuid());
                vo.setName(generateL2PortGroupName(msg.getvSwitchUuid(), msg.getVlan()));
                vo.setType(type.toString());
                vo.setZoneUuid(pg.getZoneUuid());
                vo.setAccountUuid(pg.getAccountUuid());

                factory.createL2Network(vo, l2Msg, new ReturnValueCompletion<L2NetworkInventory>(msg) {
                    @Override
                    public void success(L2NetworkInventory returnValue) {
                        for (L2NetworkCreateExtensionPoint extp : createExtensions) {
                            try {
                                extp.afterCreateL2Network(returnValue);
                            } catch (Exception e) {
                                logger.warn(String.format("unhandled exception happened when calling %s", extp.getClass().getName()), e);
                            }
                        }
                        pg.setL2NetworkUuid(returnValue.getUuid());
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                pg.setvSwitchUuid(msg.getvSwitchUuid());
                pg.setVlanId(msg.getVlan());
                pg.setVlanMode(PortGroupVlanMode.valueOf(msg.getVlanMode()));
                pg.setVlanRanges(msg.getVlanRanges());
                pg.setType(getType().toString());
                dbf.persistAndRefresh(pg);
                completion.success(PortGroupInventory.valueOf(pg));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public L3Network getL3Network(L3NetworkVO vo) {
        return new PortGroup(vo);
    }
}
