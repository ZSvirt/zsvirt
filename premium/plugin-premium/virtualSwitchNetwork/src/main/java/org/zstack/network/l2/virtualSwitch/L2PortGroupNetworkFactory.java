package org.zstack.network.l2.virtualSwitch;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.sriov.SriovSystemTags;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.L2NetworkDefaultMtu;
import org.zstack.network.l2.virtualSwitch.header.*;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.multiErr;
import static org.zstack.header.network.l2.L2NetworkType.L2NetworkTypeBuilder;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L2PortGroupNetworkFactory extends AbstractService implements L2NetworkFactory, L2NetworkDefaultMtu,
        L2NetworkGetInterfaceExtensionPoint {
    private static L2NetworkType type = new L2NetworkTypeBuilder()
            .typeName(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE)
            .sriovSupported(true)
            .attachToAllHosts(false)
            .build();
    private static final CLogger logger = Utils.getLogger(L2PortGroupNetworkFactory.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private TagManager tagMgr;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public void createL2Network(L2NetworkVO ovo, APICreateL2NetworkMsg msg, ReturnValueCompletion<L2NetworkInventory> completion) {
        class Context {
            L2PortGroupNetworkVO vo;
            final List<String> attachedClusterUuids = Collections.synchronizedList(new ArrayList<>());
        }
        final Context ctx = new Context();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("create-port-group-%s", ovo.getName()));
        chain.then(new Flow() {
            String __name__ = "port-group-write-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                APICreateL2PortGroupMsg amsg = (APICreateL2PortGroupMsg) msg;
                L2VirtualSwitchNetworkVO vSwitchVO = dbf.findByUuid(amsg.getvSwitchUuid(), L2VirtualSwitchNetworkVO.class);

                L2PortGroupNetworkVO vo = new L2PortGroupNetworkVO(ovo);
                vo.setvSwitchType(vSwitchVO.getvSwitchType());
                vo.setvSwitchUuid(amsg.getvSwitchUuid());
                vo.setPhysicalInterface(vSwitchVO.getPhysicalInterface());
                vo.setZoneUuid(vSwitchVO.getZoneUuid());
                vo.setVlanId(amsg.getVlan());
                vo.setVirtualNetworkId(amsg.getVlan());
                vo.setVlanMode(PortGroupVlanMode.valueOf(amsg.getVlanMode()));
                vo.setVlanRanges(amsg.getVlanRanges());
                vo = dbf.persistAndRefresh(vo);
                L2PortGroupNetworkInventory inv = L2PortGroupNetworkInventory.valueOf(vo);
                String info = String.format("successfully create port group, %s", JSONObjectUtil.toJsonString(inv));
                logger.debug(info);

                ctx.vo = vo;
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (ctx.vo != null) {
                    dbf.removeByPrimaryKey(ctx.vo.getUuid(), L2PortGroupNetworkVO.class);
                }

                trigger.rollback();
            }
        }).then(new Flow() {
            String __name__ = "port-group-apply-cluster";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                L2VirtualSwitchNetworkVO vSwitchVO = dbf.findByUuid(ctx.vo.getvSwitchUuid(), L2VirtualSwitchNetworkVO.class);

                new While<>(vSwitchVO.getAttachedClusterRefs()).each((ref, whileCompletion) -> {
                    AttachL2NetworkToClusterMsg msg = new AttachL2NetworkToClusterMsg();
                    msg.setL2NetworkUuid(ctx.vo.getUuid());
                    msg.setClusterUuid(ref.getClusterUuid());
                    msg.setL2ProviderType(ref.getL2ProviderType());

                    bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, ctx.vo.getUuid());
                    bus.send(msg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                ctx.attachedClusterUuids.add(ref.getClusterUuid());
                                whileCompletion.done();
                            } else {
                                logger.error(String.format("attach port group[name:%s, uuid:%s] to cluster[uuid:%s] failed:%s",
                                        ctx.vo.getName(), ctx.vo.getUuid(), ref.getClusterUuid(), reply.getError().getDetails()));
                                whileCompletion.addError(reply.getError());
                                whileCompletion.allDone();
                            }
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.hasError()) {
                            trigger.fail(multiErr(errorCodeList));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (ctx.vo == null) {
                    trigger.rollback();
                    return;
                }

                new While<>(ctx.attachedClusterUuids).each((uuid, whileCompletion) -> {
                    L2NetworkDetachFromClusterMsg msg = new L2NetworkDetachFromClusterMsg();
                    msg.setL2NetworkUuid(ctx.vo.getUuid());
                    msg.setClusterUuid(uuid);

                    bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, ctx.vo.getUuid());
                    bus.send(msg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.error(String.format("detach port group[name:%s, uuid:%s] to cluster[uuid:%s] failed:%s",
                                        ctx.vo.getName(), ctx.vo.getUuid(), uuid, reply.getError().getDetails()));
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "enable-sriov-by-default";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                tagMgr.createNonInherentSystemTag(ctx.vo.getUuid(), SriovSystemTags.L2_ENABLE_SRIOV.getTagFormat(), L2NetworkVO.class.getSimpleName());
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                L2PortGroupNetworkInventory inv = L2PortGroupNetworkInventory.valueOf(
                        dbf.findByUuid(ctx.vo.getUuid(), L2PortGroupNetworkVO.class));
                completion.success(inv);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new L2PortGroupNetwork(vo);
    }

    @Override
    public void handleMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VirtualSwitchConstant.PORT_GROUP_SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getL2NetworkType() {
        return type.toString();
    }

    @Override
    public Integer getDefaultMtu(L2NetworkInventory inv) {
        if (inv.getVirtualNetworkId() != 0) {
            return rcf.getResourceConfigValue(NetworkServiceGlobalConfig.DHCP_MTU_VLAN, inv.getUuid(), Integer.class);
        } else {
            return rcf.getResourceConfigValue(NetworkServiceGlobalConfig.DHCP_MTU_NO_VLAN, inv.getUuid(), Integer.class);
        }
    }

    @Override
    public List<String> getHostNetworkInterfaceNames(String l2NetworkUuid, String hostUuid) {
        return VirtualSwitchUtils.getPhysicalInterfaceNamesOfL2PortGroupOnHost(l2NetworkUuid, hostUuid);
    }

    @Override
    public String getPhysicalInterfaceName(String l2NetworkUuid, String hostUuid) {
        L2NetworkVO vo = dbf.findByUuid(l2NetworkUuid, L2NetworkVO.class);
        return VirtualSwitchUtils.getInterfaceNameOfL2PortGroupOnHost(L2NetworkInventory.valueOf(vo), hostUuid);
    }
}
