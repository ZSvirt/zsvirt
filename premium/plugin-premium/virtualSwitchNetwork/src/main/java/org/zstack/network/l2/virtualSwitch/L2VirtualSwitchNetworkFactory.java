package org.zstack.network.l2.virtualSwitch;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.compute.bonding.HostNetworkBondingUtils;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.bonding.BondingDeletionMsg;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.service.NetworkServiceHostExtensionPoint;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.l2.L2NetworkHostHelper;
import org.zstack.network.l2.L2NetworkHostUtils;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.network.l2.virtualSwitch.header.*;
import org.zstack.pciDevice.PciDeviceUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L2VirtualSwitchNetworkFactory extends AbstractService implements L2NetworkFactory, NetworkServiceHostExtensionPoint,
        L2NetworkGetInterfaceExtensionPoint, L2NetworkOwnedL3ExtensionPoint {
    private static L2NetworkType type = new L2NetworkType.L2NetworkTypeBuilder()
            .typeName(VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE)
            .attachToAllHosts(false)
            .build();
    private static final CLogger logger = Utils.getLogger(L2VirtualSwitchNetworkFactory.class);
    private static final L2NetworkHostHelper l2NetworkHostHelper = new L2NetworkHostHelper();
    private static final VirtualSwitchHelper virtualSwitchHelper = new VirtualSwitchHelper();

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected L2NetworkManager l2Mgr;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public void createL2Network(L2NetworkVO ovo, APICreateL2NetworkMsg msg, ReturnValueCompletion<L2NetworkInventory> completion) {
        APICreateL2VirtualSwitchMsg amsg = (APICreateL2VirtualSwitchMsg) msg;
        L2VirtualSwitchNetworkVO vo = new L2VirtualSwitchNetworkVO(ovo);
        if (vo.getPhysicalInterface() == null) {
            vo.setPhysicalInterface(L2NetworkConstant.PHYSICAL_INTERFACE_EMPTY);
        }
        vo.setDistributed(amsg.getDistributed());
        vo.setVSwitchIndex(VirtualSwitchUtils.getVirtualSwitchIndexOfZone(amsg.getZoneUuid()));
        VirtualSwitchUtils.increaseVirtualSwitchIndexOfZone(amsg.getZoneUuid());
        vo = dbf.persistAndRefresh(vo);
        L2VirtualSwitchNetworkInventory inv = L2VirtualSwitchNetworkInventory.valueOf(vo);
        String info = String.format("successfully create virtualSwitch, %s", JSONObjectUtil.toJsonString(inv));
        logger.debug(info);
        completion.success(inv);
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new L2VirtualSwitchNetwork(vo);
    }

    @Override
    public void handleMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID);
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
    public String getNetworkServiceName() {
        return VirtualSwitchConstant.VIRTUAL_SWITCH_NETWORK_TYPE;
    }

    @Override
    public void afterHostConnected(HostInventory host, Completion completion) {
        List<Tuple> tuples = SQL.New("select l2, ref.l2ProviderType from L2VirtualSwitchNetworkVO l2, L2NetworkClusterRefVO ref" +
                        " where l2.uuid = ref.l2NetworkUuid" +
                        " and ref.clusterUuid = :clusterUuid", Tuple.class)
                .param("clusterUuid", host.getClusterUuid())
                .list();

        if (tuples.isEmpty()) {
            completion.success();
            return;
        }

        List<L2VirtualSwitchNetworkVO> vSwitches = new ArrayList<>();
        Map<String, String> l2ProviderTypeMap = new HashMap<>();
        for (Tuple t : tuples) {
            L2VirtualSwitchNetworkVO vSwitch = t.get(0, L2VirtualSwitchNetworkVO.class);
            String l2ProviderType = t.get(1, String.class);

            if (VirtualSwitchUtils.isUpLinkBondingExist(vSwitch.getUuid(), host.getUuid(), vSwitch.getPhysicalInterface()) ||
                    VirtualSwitchUtils.isUplinkGroupExist(vSwitch.getUuid(), host.getUuid())) {
                vSwitches.add(vSwitch);
                l2ProviderTypeMap.putIfAbsent(vSwitch.getUuid(), l2ProviderType);
            }
        }

        Map<String, List<L2PortGroupNetworkInventory>> portGroupMap = new HashMap<>();
        List<L2PortGroupNetworkVO> attachPortGroups = Q.New(L2PortGroupNetworkVO.class)
                .in(L2PortGroupNetworkVO_.vSwitchUuid, vSwitches.stream().map(L2NetworkVO::getUuid).collect(Collectors.toList()))
                .list();

        for (L2PortGroupNetworkVO l2 : attachPortGroups) {
            portGroupMap.computeIfAbsent(l2.getvSwitchUuid(), k -> new ArrayList<>()).add(L2PortGroupNetworkInventory.valueOf(l2));
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("attach-virtual-switch-for-kvm-%s-connect", host.getUuid()));

        chain.then(new NoRollbackFlow() {
            String __name__ = "init-uplink-group";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                virtualSwitchHelper.initUplinkGroup(vSwitches, host.getUuid(), l2ProviderTypeMap);
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "update-pci-device-passThrough-state";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<UplinkGroupVO> ugs = VirtualSwitchUtils.getUplinkGroups(vSwitches.stream().map(L2NetworkVO::getUuid).collect(Collectors.toList()),
                        host.getUuid(), UplinkGroupType.PhysicalInterface);
                for (UplinkGroupVO ug : ugs) {
                    PciDeviceUtils.updatePciDevicePassThroughStateFromAvailableToDisabled(ug.getInterfaceName(), ug.getHostUuid());
                }
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "set-L2NetworkHostRefVO";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (L2VirtualSwitchNetworkVO vSwitch : vSwitches) {
                    String l2ProviderType = l2ProviderTypeMap.get(vSwitch.getUuid());

                    List<String> portGroupUuids = new ArrayList<>();
                    Map<String, String> bridgeNameMap = new HashMap<>();
                    portGroupMap.getOrDefault(vSwitch.getUuid(), new ArrayList<>()).forEach(pg -> {
                        portGroupUuids.add(pg.getUuid());
                        bridgeNameMap.putIfAbsent(pg.getUuid(), VirtualSwitchUtils.makeBridgeName(pg.getvSwitchUuid(),
                                pg.getUuid(), pg.getVlanId()));
                    });

                    l2NetworkHostHelper.initL2NetworkHostRef(portGroupUuids, host.getUuid(), l2ProviderType, bridgeNameMap);
                }

                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "realize-virtual-switch";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(vSwitches).step((vSwitch, wcomp) -> {
                    L2NetworkType l2Type = L2NetworkType.valueOf(vSwitch.getType());
                    L2ProviderType l2ProviderType = L2ProviderType.valueOf(l2ProviderTypeMap.get(vSwitch.getUuid()));

                    L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, l2ProviderType);
                    ext.realize(L2NetworkInventory.valueOf(vSwitch), host.getUuid(), true, new Completion(wcomp) {
                        @Override
                        public void success() {
                            wcomp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("realize virtual switch[uuid:%s] to host[uuid:%s] failed:%s",
                                    vSwitch.getUuid(), host.getUuid(), errorCode));
                            wcomp.done();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "realize-port-group-on-virtual-switch";

            final L2NetworkType l2Type = L2NetworkType.valueOf(VirtualSwitchConstant.PORT_GROUP_NETWORK_TYPE);

            @Override
            public void run(FlowTrigger trigger, Map data) {
                realizePortGroup(vSwitches.iterator(), trigger);
            }

            private void realizePortGroup(final Iterator<L2VirtualSwitchNetworkVO> it, final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    trigger.next();
                    return;
                }

                L2VirtualSwitchNetworkVO vSwitch = it.next();
                L2ProviderType l2ProviderType = L2ProviderType.valueOf(l2ProviderTypeMap.get(vSwitch.getUuid()));

                List<L2PortGroupNetworkInventory> portGroups = portGroupMap.getOrDefault(vSwitch.getUuid(), new ArrayList<>());
                if (portGroups.isEmpty()) {
                    logger.info(String.format("There is no port group on virtual switch[uuid:%s]", vSwitch.getUuid()));
                    realizePortGroup(it, trigger);
                    return;
                }

                List<L2PortGroupNetworkInventory> attachedPortGroups = Collections.synchronizedList(new ArrayList<>());
                L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, l2ProviderType);

                // first realize no vlan port group to set interface mtu
                L2PortGroupNetworkInventory noVlanL2 = portGroups.stream().filter(l2 -> l2.getVirtualNetworkId() == 0).findFirst().orElse(null);
                if (noVlanL2 != null) {
                    FutureCompletion fcomp = new FutureCompletion(null);
                    ext.realize(noVlanL2, host.getUuid(), true, new Completion(fcomp) {
                        @Override
                        public void success() {
                            attachedPortGroups.add(noVlanL2);
                            fcomp.success();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("realize port group[uuid:%s] to host[uuid:%s] failed, %s, now rollback",
                                    noVlanL2.getUuid(), host.getUuid(), errorCode));
                            fcomp.fail(errorCode);
                        }
                    });
                    fcomp.await(TimeUnit.SECONDS.toMillis(60));
                    if (!fcomp.isSuccess()) {
                        rollback(attachedPortGroups, L2VirtualSwitchNetworkInventory.valueOf(vSwitch), l2ProviderType);
                        realizePortGroup(it, trigger);
                        return;
                    }
                }

                List<L2PortGroupNetworkInventory> vlanPortGroups = portGroups.stream().filter(l2 -> l2.getVirtualNetworkId() != 0).collect(Collectors.toList());
                new While<>(vlanPortGroups).step((l2, wcomp) -> {
                    ext.realize(l2, host.getUuid(), true, new Completion(wcomp) {
                        @Override
                        public void success() {
                            attachedPortGroups.add(l2);
                            wcomp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("realize port group[uuid:%s] to host[uuid:%s] failed:%s",
                                    l2.getUuid(), host.getUuid(), errorCode));
                            wcomp.addError(errorCode);
                            wcomp.allDone();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            logger.warn(String.format("realize port group on virtual switch[uuid:%s] to host[uuid:%s] failed, %s, now rollback",
                                    vSwitch.getUuid(), host.getUuid(), errorCodeList.getCauses().get(0)));
                            rollback(attachedPortGroups, L2VirtualSwitchNetworkInventory.valueOf(vSwitch), l2ProviderType);
                        }

                        realizePortGroup(it, trigger);
                    }
                });
            }

            private void rollback(List<L2PortGroupNetworkInventory> l2s,
                                  L2VirtualSwitchNetworkInventory vSwitch,
                                  L2ProviderType providerType) {
                L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, providerType);

                new While<>(l2s).step((l2, wcomp) -> {
                    ext.delete(l2, host.getUuid(), new Completion(wcomp) {
                        @Override
                        public void success() {
                            wcomp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("delete port group[uuid:%s] from host[uuid:%s] failed:%s",
                                    l2.getUuid(), host.getUuid(), errorCode));
                            wcomp.done();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(null) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(L2NetworkType.valueOf(vSwitch.getType()), providerType);
                        ext.delete(vSwitch, host.getUuid(), new Completion(null) {
                            @Override
                            public void success() {
                                List<String> portGroupUuids = portGroupMap.getOrDefault(vSwitch.getUuid(), new ArrayList<>())
                                        .stream().map(L2PortGroupNetworkInventory::getUuid).collect(Collectors.toList());
                                L2NetworkHostUtils.deleteL2NetworkHostRef(portGroupUuids, host.getUuid());
                                UplinkGroupVO ug = VirtualSwitchUtils.getUplinkGroup(vSwitch.getUuid(), host.getUuid(), UplinkGroupType.PhysicalInterface);
                                if (ug != null) {
                                    PciDeviceUtils.updatePciDevicePassThroughStateFromDisabledToAvailable(ug.getInterfaceName(), ug.getHostUuid());
                                }
                                VirtualSwitchUtils.deleteUplinkGroup(vSwitch.getUuid(), host.getUuid());
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.warn(String.format("delete virtual switch[uuid:%s] from host[uuid:%s] failed:%s",
                                        vSwitch.getUuid(), host.getUuid(), errorCode));
                            }
                        });
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "refresh-host-kernel-interface";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                RefreshHostKernelInterfaceOnHostMsg rmsg = new RefreshHostKernelInterfaceOnHostMsg();
                rmsg.setHostUuid(host.getUuid());
                bus.makeTargetServiceIdByResourceUuid(rmsg, VirtualSwitchConstant.VIRTUAL_SWITCH_SERVICE_ID, host.getUuid());
                bus.send(rmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to refresh host kernel interface on host[uuid:%s]", host.getUuid()));
                        }

                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void beforeDeleteHost(HostInventory host, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("detach-virtual-switch-for-kvm-%s-delete", host.getUuid()));

        List<Tuple> tuples = SQL.New("select l2.uuid, l2.physicalInterface from L2VirtualSwitchNetworkVO l2, L2NetworkHostRefVO ref" +
                        " where l2.uuid = ref.l2NetworkUuid" +
                        " and ref.hostUuid = :hostUuid", Tuple.class)
                .param("hostUuid", host.getUuid())
                .list();

        if (tuples.isEmpty()) {
            completion.success();
            return;
        }

        List<String> vSwitchUuids = new ArrayList<>();
        List<String> interfaceNames = new ArrayList<>();
        for (Tuple t : tuples) {
            String vSwitchUuid = t.get(0, String.class);
            vSwitchUuids.add(vSwitchUuid);
            if (!VirtualSwitchSystemTags.VIRTUAL_SWITCH_DEFAULT.hasTag(vSwitchUuid)) {
                interfaceNames.add(t.get(1, String.class));
            }
        }

        List<String> portGroupUuids = Q.New(L2PortGroupNetworkVO.class)
                .select(L2PortGroupNetworkVO_.uuid)
                .in(L2PortGroupNetworkVO_.vSwitchUuid, vSwitchUuids).listValues();

        chain.then(new NoRollbackFlow() {
            String __name__ = "detach-port-group-from-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (portGroupUuids.isEmpty()) {
                    logger.info(String.format("there is no port group attach to host %s", host.getUuid()));
                    trigger.next();
                    return;
                }

                new While<>(portGroupUuids).step((uuid, wcomp) -> {
                    L2NetworkDetachFromHostMsg dmsg = new L2NetworkDetachFromHostMsg();
                    dmsg.setL2NetworkUuid(uuid);
                    dmsg.setHostUuid(host.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                    bus.send(dmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("detach port group[uuid:%s] from host[uuid:%s] failed: %s",
                                        uuid, host.getUuid(), reply.getError().toString()));
                            }
                            wcomp.done();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(null) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "detach-virtual-switch-from-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(vSwitchUuids).step((uuid, wcomp) -> {
                    L2NetworkDetachFromHostMsg dmsg = new L2NetworkDetachFromHostMsg();
                    dmsg.setL2NetworkUuid(uuid);
                    dmsg.setHostUuid(host.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, L2NetworkConstant.SERVICE_ID, dmsg.getL2NetworkUuid());
                    bus.send(dmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("detach virtual switch[uuid:%s] from host[uuid:%s] failed: %s",
                                        uuid, host.getUuid(), reply.getError().toString()));
                            }
                            wcomp.done();
                        }
                    });
                }, 10).run(new WhileDoneCompletion(null) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-bonding";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (interfaceNames.isEmpty()) {
                    logger.info(String.format("there is no bonding on host[uuid:%s]", host.getUuid()));
                    trigger.next();
                    return;
                }

                List<HostNetworkBondingVO> bondingVOs = Q.New(HostNetworkBondingVO.class)
                        .in(HostNetworkBondingVO_.bondingName, interfaceNames)
                        .eq(HostNetworkBondingVO_.hostUuid, host.getUuid())
                        .list();

                if (bondingVOs.isEmpty()) {
                    logger.warn(String.format("there is no bonding %s on host[uuid:%s]",
                            interfaceNames, host.getUuid()));
                    trigger.next();
                    return;
                }

                new While<>(bondingVOs).each((vo, wcomp) -> {
                    ErrorCode err = HostNetworkBondingUtils.validateDeleteBonding(vo);
                    if (err != null) {
                        logger.warn(String.format("delete bonding[uuid:%s] failed:%s", vo.getUuid(), err));
                        wcomp.done();
                        return;
                    }

                    BondingDeletionMsg dmsg = new BondingDeletionMsg();
                    dmsg.setBondingUuid(vo.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(dmsg, HostNetworkBondingConstant.SERVICE_ID, vo.getUuid());
                    bus.send(dmsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.warn(String.format("delete bonding[uuid:%s] failed:%s", vo.getUuid(), reply.getError().toString()));
                            }
                            wcomp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                /* delete host will delete L2NetworkHostRefVO by db cascade */
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public boolean isPhysicalInterfaceOccupied(String interfaceName, String hostUuid) {
        return VirtualSwitchUtils.getVSwitchUuidOfUplinkGroup(interfaceName, hostUuid) != null;
    }

    @Override
    public List<String> getOwnedL3NetworkUuids(String l2NetworkUuid) {
        return Q.New(PortGroupVO.class)
                .select(PortGroupVO_.uuid)
                .eq(PortGroupVO_.vSwitchUuid, l2NetworkUuid)
                .listValues();
    }
}
