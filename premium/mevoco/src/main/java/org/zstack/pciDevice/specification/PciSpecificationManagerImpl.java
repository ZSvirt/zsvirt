package org.zstack.pciDevice.specification;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.*;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.*;
import org.zstack.header.identity.AddtionalResourceTypeExtensionPoint;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.GetVmStartingCandidateClustersHostsMsg;
import org.zstack.header.vm.GetVmStartingCandidateClustersHostsReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.identity.AccountManager;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.specification.mdev.*;
import org.zstack.pciDevice.specification.pci.*;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.pciDevice.virtual.vfio_mdev.*;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

/**
 * Created by GuoYi on 2019-05-06.
 */
public class PciSpecificationManagerImpl extends AbstractService implements
        PciSpecificationManager,
        AddtionalResourceTypeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(PciSpecificationManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    protected PciDeviceManager pciMgr;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof GetPciDeviceSpecCandidatesMsg) {
            handle((GetPciDeviceSpecCandidatesMsg) msg);
        } else if (msg instanceof GetMdevDeviceSpecCandidatesMsg) {
            handle((GetMdevDeviceSpecCandidatesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(GetPciDeviceSpecCandidatesMsg msg) {
        GetPciDeviceSpecCandidatesReply reply = new GetPciDeviceSpecCandidatesReply();
        List<String> hostUuids = new ArrayList<>();

        if (StringUtils.isNotBlank(msg.getVmInstanceUuid())) {
            String attachedPciHost = Q.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .select(PciDeviceVO_.hostUuid)
                    .limit(1)
                    .findValue();
            if (StringUtils.isNotBlank(attachedPciHost)) {
                hostUuids.add(attachedPciHost);
            } else {
                // vm needs to be stopped
                GetVmStartingCandidateClustersHostsMsg gmsg = new GetVmStartingCandidateClustersHostsMsg();
                gmsg.setUuid(msg.getVmInstanceUuid());
                bus.makeLocalServiceId(gmsg, VmInstanceConstant.SERVICE_ID);
                bus.send(gmsg, new CloudBusCallBack(reply) {
                    @Override
                    public void run(MessageReply rly) {
                        if (!rly.isSuccess()) {
                            reply.setError(Platform.operr("failed to get candidate hosts to start vm[uuid:%s], %s",
                                    msg.getVmInstanceUuid(), rly.getError()));
                        } else {
                            GetVmStartingCandidateClustersHostsReply grly = (GetVmStartingCandidateClustersHostsReply) rly;
                            if (!grly.isSuccess()) {
                                reply.setError(grly.getError());
                            } else {
                                hostUuids.addAll(grly.getHostInventories().stream().map(HostInventory::getUuid).collect(Collectors.toList()));
                                reply.setInventories(getAccountCanAccessPciSpecs(
                                        getAttachablePciDeviceSpecsFromHosts(hostUuids, msg.getTypes(), msg.getSession()), msg.getSession()));
                            }
                        }
                        bus.reply(msg, reply);
                    }
                });
                return;
            }
        }

        if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
            hostUuids.addAll(Q.New(HostVO.class)
                    .in(HostVO_.clusterUuid, msg.getClusterUuids())
                    .eq(HostVO_.state, HostState.Enabled)
                    .eq(HostVO_.status, HostStatus.Connected)
                    .select(HostVO_.uuid)
                    .listValues());
        }

        if (StringUtils.isNotBlank(msg.getHostUuid())) {
            hostUuids.add(msg.getHostUuid());
        }

        if (hostUuids.isEmpty()) {
            reply.setInventories(Collections.emptyList());
            bus.reply(msg, reply);
            return;
        }

        reply.setInventories(getAccountCanAccessPciSpecs(
                getAttachablePciDeviceSpecsFromHosts(hostUuids, msg.getTypes(), msg.getSession()), msg.getSession()));
        bus.reply(msg, reply);
    }

    private void handle(GetMdevDeviceSpecCandidatesMsg msg) {
        GetMdevDeviceSpecCandidatesReply reply = new GetMdevDeviceSpecCandidatesReply();
        List<String> hostUuids = new ArrayList<>();

        if (StringUtils.isNotBlank(msg.getVmInstanceUuid())) {
            String attachedMdevHost = Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .select(MdevDeviceVO_.hostUuid)
                    .limit(1)
                    .findValue();
            if (StringUtils.isNotBlank(attachedMdevHost)) {
                hostUuids.add(attachedMdevHost);
            } else {
                // vm needs to be stopped
                GetVmStartingCandidateClustersHostsMsg gmsg = new GetVmStartingCandidateClustersHostsMsg();
                gmsg.setUuid(msg.getVmInstanceUuid());
                bus.makeLocalServiceId(gmsg, VmInstanceConstant.SERVICE_ID);
                bus.send(gmsg, new CloudBusCallBack(reply) {
                    @Override
                    public void run(MessageReply rly) {
                        if (!rly.isSuccess()) {
                            reply.setError(Platform.operr(
                                    "failed to get candidate hosts to start vm[uuid:%s], %s",
                                    msg.getVmInstanceUuid(), rly.getError()));
                        } else {
                            GetVmStartingCandidateClustersHostsReply grly = (GetVmStartingCandidateClustersHostsReply) rly;
                            if (!grly.isSuccess()) {
                                reply.setError(grly.getError());
                            } else {
                                hostUuids.addAll(grly.getHostInventories().stream().map(HostInventory::getUuid).collect(Collectors.toList()));
                                reply.setInventories(getAccountCanAccessMdevSpecs(
                                        getAttachableMdevDeviceSpecsFromHosts(hostUuids, msg.getTypes(), msg.getSession()), msg.getSession()));
                            }
                        }
                        bus.reply(msg, reply);
                    }
                });
                return;
            }
        }

        if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
            hostUuids.addAll(Q.New(HostVO.class)
                    .in(HostVO_.clusterUuid, msg.getClusterUuids())
                    .eq(HostVO_.state, HostState.Enabled)
                    .eq(HostVO_.status, HostStatus.Connected)
                    .select(HostVO_.uuid)
                    .listValues());
        }

        if (StringUtils.isNotBlank(msg.getHostUuid())) {
            hostUuids.add(msg.getHostUuid());
        }

        if (hostUuids.isEmpty()) {
            reply.setInventories(Collections.emptyList());
            bus.reply(msg, reply);
            return;
        }

        reply.setInventories(getAccountCanAccessMdevSpecs(
                getAttachableMdevDeviceSpecsFromHosts(hostUuids, msg.getTypes(), msg.getSession()), msg.getSession()));
        bus.reply(msg, reply);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdatePciDeviceSpecMsg) {
            handle((APIUpdatePciDeviceSpecMsg) msg);
        } else if (msg instanceof APIUpdateMdevDeviceSpecMsg) {
            handle((APIUpdateMdevDeviceSpecMsg) msg);
        } else if (msg instanceof APIGetPciDeviceSpecCandidatesMsg) {
            handle((APIGetPciDeviceSpecCandidatesMsg) msg);
        } else if (msg instanceof APIAddPciDeviceSpecToVmInstanceMsg) {
            handle((APIAddPciDeviceSpecToVmInstanceMsg) msg);
        } else if (msg instanceof APIRemovePciDeviceSpecFromVmInstanceMsg) {
            handle((APIRemovePciDeviceSpecFromVmInstanceMsg) msg);
        } else if (msg instanceof APIGetMdevDeviceSpecCandidatesMsg) {
            handle((APIGetMdevDeviceSpecCandidatesMsg) msg);
        } else if (msg instanceof APIAddMdevDeviceSpecToVmInstanceMsg) {
            handle((APIAddMdevDeviceSpecToVmInstanceMsg) msg);
        } else if (msg instanceof APIRemoveMdevDeviceSpecFromVmInstanceMsg) {
            handle((APIRemoveMdevDeviceSpecFromVmInstanceMsg) msg);
        }
    }

    private void handle(APIUpdatePciDeviceSpecMsg msg) {
        APIUpdatePciDeviceSpecEvent evt = new APIUpdatePciDeviceSpecEvent(msg.getId());
        PciDeviceSpecVO spec = dbf.findByUuid(msg.getUuid(), PciDeviceSpecVO.class);
        if (StringUtils.isNotBlank(msg.getName())) {
            spec.setName(msg.getName());
        }
        if (StringUtils.isNotBlank(msg.getDescription())) {
            spec.setDescription(msg.getDescription());
        }
        if (StringUtils.isNotBlank(msg.getState())) {
            spec.setState(PciDeviceSpecState.valueOf(msg.getState()));
        }
        if (StringUtils.isNotBlank(msg.getRomContent())) {
            spec.setRomContent(msg.getRomContent());
            spec.setRomMd5sum(StringDSL.getMd5Sum(msg.getRomContent()));
        }
        if (StringUtils.isNotBlank(msg.getRomVersion())) {
            spec.setRomVersion(msg.getRomVersion());
        }
        if (msg.isAbandonSpecRom()) {
            spec.setRomContent(null);
            spec.setRomVersion(null);
            spec.setRomMd5sum(null);
        }
        spec = dbf.updateAndRefresh(spec);
        logger.debug(String.format("updated pci device spec[uuid:%s], name: %s, state: %s",
                msg.getUuid(), spec.getName(), spec.getState()));
        evt.setInventory(spec.toInventory());
        bus.publish(evt);
    }

    private void handle(APIUpdateMdevDeviceSpecMsg msg) {
        APIUpdateMdevDeviceSpecEvent evt = new APIUpdateMdevDeviceSpecEvent(msg.getId());
        MdevDeviceSpecVO spec = dbf.findByUuid(msg.getUuid(), MdevDeviceSpecVO.class);
        if (StringUtils.isNotBlank(msg.getName())) {
            spec.setName(msg.getName());
        }
        if (StringUtils.isNotBlank(msg.getDescription())) {
            spec.setDescription(msg.getDescription());
        }
        if (StringUtils.isNotBlank(msg.getState())) {
            spec.setState(MdevDeviceSpecState.valueOf(msg.getState()));
        }
        spec = dbf.updateAndRefresh(spec);
        logger.debug(String.format("updated mdev device spec[uuid:%s], name: %s, state: %s",
                msg.getUuid(), spec.getName(), spec.getState()));
        evt.setInventory(spec.toInventory());
        bus.publish(evt);
    }

    private void handle(APIGetPciDeviceSpecCandidatesMsg msg) {
        APIGetPciDeviceSpecCandidatesReply reply = new APIGetPciDeviceSpecCandidatesReply();
        if (CollectionUtils.isNotEmpty(msg.getVmInstanceUuids())) {
            List<ErrorCode> errors = new ArrayList<>();
            List<List<String>> candidateSpecs = new ArrayList<>();
            new While<>(msg.getVmInstanceUuids()).all((vmUuid, completion) -> {
                GetPciDeviceSpecCandidatesMsg gmsg = new GetPciDeviceSpecCandidatesMsg();
                gmsg.setVmInstanceUuid(vmUuid);
                gmsg.setTypes(msg.getTypes());
                gmsg.setSession(msg.getSession());
                bus.makeLocalServiceId(gmsg, PciSpecificationConstants.SERVICE_ID);
                bus.send(gmsg, new CloudBusCallBack(reply) {
                    @Override
                    public void run(MessageReply rly) {
                        if (!rly.isSuccess()) {
                            errors.add(rly.getError());
                            completion.allDone();
                            return;
                        } else {
                            GetPciDeviceSpecCandidatesReply grly = rly.castReply();
                            candidateSpecs.add(grly.getInventories().stream().map(PciDeviceSpecInventory::getUuid).collect(Collectors.toList()));
                        }

                        completion.done();
                    }
                });
            }).run(new WhileDoneCompletion(msg) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (!errors.isEmpty()) {
                        reply.setError(errors.get(0));
                        bus.reply(msg, reply);
                        return;
                    }

                    if (candidateSpecs.isEmpty()) {
                        bus.reply(msg, reply);
                        return;
                    }

                    // get common spec candidates for msg.getVmInstanceUuids
                    Set<String> candidates = new HashSet<>(candidateSpecs.get(0));
                    for (int i = 1; i < candidateSpecs.size(); i++) {
                        candidates.retainAll(new HashSet<>(candidateSpecs.get(i)));
                    }
                    if (candidates.isEmpty()) {
                        reply.setInventories(new ArrayList<>());
                    } else {
                        reply.setInventories(PciDeviceSpecInventory.valueOf(dbf.listByPrimaryKeys(candidates, PciDeviceSpecVO.class)));
                    }
                    bus.reply(msg, reply);
                }
            });
        } else {
            GetPciDeviceSpecCandidatesMsg gmsg = new GetPciDeviceSpecCandidatesMsg();
            gmsg.setClusterUuids(msg.getClusterUuids());
            gmsg.setHostUuid(msg.getHostUuid());
            gmsg.setTypes(msg.getTypes());
            gmsg.setSession(msg.getSession());
            bus.makeLocalServiceId(gmsg, PciSpecificationConstants.SERVICE_ID);
            bus.send(gmsg, new CloudBusCallBack(reply) {
                @Override
                public void run(MessageReply rly) {
                    if (!rly.isSuccess()) {
                        reply.setError(rly.getError());
                    } else {
                        GetPciDeviceSpecCandidatesReply grly = rly.castReply();
                        reply.setInventories(grly.getInventories());
                    }
                    bus.reply(msg, reply);
                }
            });
        }
    }

    private void handle(APIAddPciDeviceSpecToVmInstanceMsg msg) {
        APIAddPciDeviceSpecToVmInstanceEvent evt = new APIAddPciDeviceSpecToVmInstanceEvent(msg.getId());
        GetPciDeviceSpecCandidatesMsg gmsg = new GetPciDeviceSpecCandidatesMsg();
        gmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        gmsg.setSession(msg.getSession());
        bus.makeLocalServiceId(gmsg, PciSpecificationConstants.SERVICE_ID);
        bus.send(gmsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    evt.setError(Platform.operr("failed to get pci device spec available for vm[uuid:%s]: %s", msg.getVmInstanceUuid(), rly.getError()));
                } else {
                    GetPciDeviceSpecCandidatesReply grly = rly.castReply();
                    List<PciDeviceSpecInventory> inventories = grly.getInventories();
                    if (inventories == null || inventories.isEmpty()) {
                        evt.setError(Platform.operr("no pci device spec available for vm[uuid:%s]", msg.getVmInstanceUuid()));
                    } else if (!inventories.stream().map(PciDeviceSpecInventory::getUuid).collect(Collectors.toList()).contains(msg.getPciSpecUuid())) {
                        evt.setError(Platform.operr("pci device spec[uuid:%s] is not available for vm[uuid:%s]", msg.getPciSpecUuid(), msg.getVmInstanceUuid()));
                    } else {
                        VmInstancePciDeviceSpecRefVO ref = new VmInstancePciDeviceSpecRefVO();
                        ref.setVmInstanceUuid(msg.getVmInstanceUuid());
                        ref.setPciSpecUuid(msg.getPciSpecUuid());
                        ref.setPciDeviceNumber(msg.getPciDeviceNumber());
                        ref = dbf.persistAndRefresh(ref);
                        evt.setInventory(ref.toInventory());
                        logger.debug(String.format("added pci device spec[uuid:%s, devnum:%d] to vm[uuid:%s]",
                                msg.getPciSpecUuid(), msg.getPciDeviceNumber(), msg.getVmInstanceUuid()));
                    }
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIRemovePciDeviceSpecFromVmInstanceMsg msg) {
        APIRemovePciDeviceSpecFromVmInstanceEvent evt = new APIRemovePciDeviceSpecFromVmInstanceEvent(msg.getId());
        List<String> specPciUuids = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .eq(PciDeviceVO_.pciSpecUuid, msg.getPciSpecUuid())
                .eq(PciDeviceVO_.chooser, PciDeviceChooser.Spec)
                .select(PciDeviceVO_.uuid)
                .listValues();
        if (specPciUuids.isEmpty()) {
            SQL.New(VmInstancePciDeviceSpecRefVO.class)
                    .eq(VmInstancePciDeviceSpecRefVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .eq(VmInstancePciDeviceSpecRefVO_.pciSpecUuid, msg.getPciSpecUuid())
                    .hardDelete();
            logger.debug(String.format("removed pci device spec[uuid:%s] from vm[uuid:%s]",
                    msg.getPciSpecUuid(), msg.getVmInstanceUuid()));
            bus.publish(evt);
            return;
        }

        // expand to all pci devices in same iommu group
        List<String> pciUuids = new ArrayList<>();
        for (String specPciUuid : specPciUuids) {
            pciUuids.addAll(PciDeviceUtils.getPciDeviceUuidsFromSameIommuGroup(specPciUuid));
        }
    
        // find dirty data in PciDeviceVO : msg.vmInstanceUuid != PciDeviceVO.vmInstanceUuid
        List<PciDeviceVO> pcis = Q.New(PciDeviceVO.class).in(PciDeviceVO_.uuid, pciUuids).list();
        Map<String, PciDeviceVO> pcisNotMatch = pcis.stream()
                .filter(pci -> !pci.getVmInstanceUuid().equals(msg.getVmInstanceUuid()))
                .collect(Collectors.toMap(PciDeviceVO::getUuid, Function.identity()));
        if (!pcisNotMatch.isEmpty()) {
            pciUuids.removeAll(pcisNotMatch.keySet());
        }

        // auto detach spec releated devices when spec is removed
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("detach-pci-devices-when-remove-spec-from-vm");
        chain.allowEmptyFlow();
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (!pciUuids.isEmpty()) {
                    flow(pciMgr.createDetachPciDeviceFromVmFlow(pciUuids));
                    flow(pciMgr.createAttachPciDeviceToHostFlow(pciUuids));
                    //flow(pciMgr.createSyncPciDeviceInfoFlow(pciUuids));
                }

                done(new FlowDoneHandler(evt) {
                    @Override
                    public void handle(Map data) {
                        pciMgr.detachPciDeviceFromVmInDb(pciUuids);

                        // remove pci device spec from vm
                        SQL.New(VmInstancePciDeviceSpecRefVO.class)
                                .eq(VmInstancePciDeviceSpecRefVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                                .eq(VmInstancePciDeviceSpecRefVO_.pciSpecUuid, msg.getPciSpecUuid())
                                .hardDelete();
                        logger.debug(String.format("removed pci device spec[uuid:%s] from vm[uuid:%s]",
                                msg.getPciSpecUuid(), msg.getVmInstanceUuid()));
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(evt) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(errCode);
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    private void handle(APIGetMdevDeviceSpecCandidatesMsg msg) {
        APIGetMdevDeviceSpecCandidatesReply reply = new APIGetMdevDeviceSpecCandidatesReply();
        if (CollectionUtils.isNotEmpty(msg.getVmInstanceUuids())) {
            List<ErrorCode> errors = new ArrayList<>();
            List<List<String>> candidateSpecs = new ArrayList<>();
            new While<>(msg.getVmInstanceUuids()).all((vmUuid, completion) -> {
                GetMdevDeviceSpecCandidatesMsg gmsg = new GetMdevDeviceSpecCandidatesMsg();
                gmsg.setVmInstanceUuid(vmUuid);
                gmsg.setTypes(msg.getTypes());
                gmsg.setSession(msg.getSession());
                bus.makeLocalServiceId(gmsg, PciSpecificationConstants.SERVICE_ID);
                bus.send(gmsg, new CloudBusCallBack(reply) {
                    @Override
                    public void run(MessageReply rly) {
                        if (!rly.isSuccess()) {
                            errors.add(rly.getError());
                            completion.allDone();
                            return;
                        } else {
                            GetMdevDeviceSpecCandidatesReply grly = rly.castReply();
                            candidateSpecs.add(grly.getInventories().stream().map(MdevDeviceSpecInventory::getUuid).collect(Collectors.toList()));
                        }

                        completion.done();
                    }
                });
            }).run(new WhileDoneCompletion(msg) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    if (!errors.isEmpty()) {
                        reply.setError(errors.get(0));
                        bus.reply(msg, reply);
                        return;
                    }

                    if (candidateSpecs.isEmpty()) {
                        bus.reply(msg, reply);
                        return;
                    }

                    // get common spec candidates for msg.getVmInstanceUuids
                    Set<String> candidates = new HashSet<>(candidateSpecs.get(0));
                    for (int i = 1; i < candidateSpecs.size(); i++) {
                        candidates.retainAll(new HashSet<>(candidateSpecs.get(i)));
                    }
                    if (candidates.isEmpty()) {
                        reply.setInventories(new ArrayList<>());
                    } else {
                        reply.setInventories(MdevDeviceSpecInventory.valueOf(dbf.listByPrimaryKeys(candidates, MdevDeviceSpecVO.class)));
                    }
                    bus.reply(msg, reply);
                }
            });
        } else {
            GetMdevDeviceSpecCandidatesMsg gmsg = new GetMdevDeviceSpecCandidatesMsg();
            gmsg.setClusterUuids(msg.getClusterUuids());
            gmsg.setHostUuid(msg.getHostUuid());
            gmsg.setTypes(msg.getTypes());
            gmsg.setSession(msg.getSession());
            bus.makeLocalServiceId(gmsg, PciSpecificationConstants.SERVICE_ID);
            bus.send(gmsg, new CloudBusCallBack(reply) {
                @Override
                public void run(MessageReply rly) {
                    if (!rly.isSuccess()) {
                        reply.setError(rly.getError());
                    } else {
                        GetMdevDeviceSpecCandidatesReply grly = rly.castReply();
                        reply.setInventories(grly.getInventories());
                    }
                    bus.reply(msg, reply);
                }
            });
        }
    }

    private void handle(APIAddMdevDeviceSpecToVmInstanceMsg msg) {
        APIAddMdevDeviceSpecToVmInstanceEvent evt = new APIAddMdevDeviceSpecToVmInstanceEvent(msg.getId());
        GetMdevDeviceSpecCandidatesMsg gmsg = new GetMdevDeviceSpecCandidatesMsg();
        gmsg.setVmInstanceUuid(msg.getVmInstanceUuid());
        gmsg.setSession(msg.getSession());
        bus.makeLocalServiceId(gmsg, PciSpecificationConstants.SERVICE_ID);
        bus.send(gmsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply rly) {
                if (!rly.isSuccess()) {
                    evt.setError(Platform.operr("no mdev device spec available for vm[uuid:%s]",msg.getVmInstanceUuid()));
                } else {
                    GetMdevDeviceSpecCandidatesReply grly = rly.castReply();
                    List<MdevDeviceSpecInventory> inventories = grly.getInventories();
                    if (inventories == null || inventories.isEmpty()) {
                        evt.setError(Platform.operr("no mdev device spec available for vm[uuid:%s]", msg.getVmInstanceUuid()));
                    } else if (!inventories.stream().map(MdevDeviceSpecInventory::getUuid).collect(Collectors.toList()).contains(msg.getMdevSpecUuid())) {
                        evt.setError(Platform.operr("mdev device spec[uuid:%s] is not available for vm[uuid:%s]", msg.getMdevSpecUuid(), msg.getVmInstanceUuid()));
                    } else {
                        VmInstanceMdevDeviceSpecRefVO ref = new VmInstanceMdevDeviceSpecRefVO();
                        ref.setVmInstanceUuid(msg.getVmInstanceUuid());
                        ref.setMdevSpecUuid(msg.getMdevSpecUuid());
                        ref.setMdevDeviceNumber(msg.getMdevDeviceNumber());
                        ref = dbf.persistAndRefresh(ref);
                        evt.setInventory(ref.toInventory());
                        logger.debug(String.format("added mdev device spec[uuid:%s, devnum:%d] to vm[uuid:%s]",
                                msg.getMdevSpecUuid(), msg.getMdevDeviceNumber(), msg.getVmInstanceUuid()));
                    }
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIRemoveMdevDeviceSpecFromVmInstanceMsg msg) {
        APIRemoveMdevDeviceSpecFromVmInstanceEvent evt = new APIRemoveMdevDeviceSpecFromVmInstanceEvent(msg.getId());
        // auto detach spec releated devices when spec is removed
        MdevDeviceUtils.detachMdevDeviceForVmInDB(msg.getVmInstanceUuid(), msg.getMdevSpecUuid(), null, MdevDeviceChooser.Spec);

        // remove mdev device spec from vm
        SQL.New(VmInstanceMdevDeviceSpecRefVO.class)
                .eq(VmInstanceMdevDeviceSpecRefVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .eq(VmInstanceMdevDeviceSpecRefVO_.mdevSpecUuid, msg.getMdevSpecUuid())
                .hardDelete();
        logger.debug(String.format("removed mdev device spec[uuid:%s] from vm[uuid:%s]",
                msg.getMdevSpecUuid(), msg.getVmInstanceUuid()));
        bus.publish(evt);
    }

    private List<PciDeviceSpecInventory> getAccountCanAccessPciSpecs(List<PciDeviceSpecInventory> specs, SessionInventory session) {
        if (specs == null || specs.isEmpty()) {
            return Collections.emptyList();
        } else if (acntMgr.isAdmin(session)) {
            return specs;
        }

        final String accountUuid = session.getAccountUuid();
        List<String> canAccessPciSpecs = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, PciDeviceSpecVO.class);
        if (canAccessPciSpecs == null || canAccessPciSpecs.isEmpty()) {
            return Collections.emptyList();
        }

        return specs.stream().filter(i -> canAccessPciSpecs.contains(i.getUuid())).collect(Collectors.toList());
    }

    private List<MdevDeviceSpecInventory> getAccountCanAccessMdevSpecs(List<MdevDeviceSpecInventory> specs, SessionInventory session) {
        if (specs == null || specs.isEmpty()) {
            return Collections.emptyList();
        } else if (acntMgr.isAdmin(session)) {
            return specs;
        }

        final String accountUuid = session.getAccountUuid();
        List<String> canAccessMdevSpecs = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, MdevDeviceSpecVO.class);
        if (canAccessMdevSpecs == null || canAccessMdevSpecs.isEmpty()) {
            return Collections.emptyList();
        }

        return specs.stream().filter(i -> canAccessMdevSpecs.contains(i.getUuid())).collect(Collectors.toList());
    }

    private List<String> getIommuEnabledAndActiveHosts(List<String> hostUuids) {
        HostIommuGetter getter = new HostIommuGetter();
        return hostUuids.stream()
                .filter(hostUuid -> getter.getState(hostUuid).equals(HostIommuStateType.Enabled))
                .filter(hostUuid -> getter.getStatus(hostUuid).equals(HostIommuStatusType.Active))
                .collect(Collectors.toList());
    }

    private List<PciDeviceSpecInventory> getAttachablePciDeviceSpecsFromHosts(List<String> hostUuids, List<String> types, SessionInventory session) {
        List<String> iommuEnabledAndActiveHosts = getIommuEnabledAndActiveHosts(hostUuids);
        if (iommuEnabledAndActiveHosts == null || iommuEnabledAndActiveHosts.isEmpty()) {
            return Collections.emptyList();
        }

        List<PciDeviceType> devTypes;
        if (types != null && !types.isEmpty()) {
            devTypes = types.stream().map(PciDeviceType::valueOf).collect(Collectors.toList());
        } else {
            devTypes = asList(PciDeviceType.values());
        }

        List<String> canAccessPcis;
        if (acntMgr.isAdmin(session)) {
            canAccessPcis = Q.New(PciDeviceVO.class)
                    .in(PciDeviceVO_.hostUuid, iommuEnabledAndActiveHosts)
                    .select(PciDeviceVO_.uuid)
                    .listValues();
        } else {
            canAccessPcis = acntMgr.getResourceUuidsCanAccessByAccount(session.getAccountUuid(), PciDeviceVO.class);
        }
        if (canAccessPcis == null || canAccessPcis.isEmpty()) {
            return Collections.emptyList();
        }

        String sql = "select pciSpecUuid from PciDeviceVO where " +
                "uuid in :pciUuids and hostUuid in :hostUuids and state='Enabled' and status in :status " +
                "and virtStatus in :virtStatus and type in :type group by pciSpecUuid";

        List<String> specUuids = SQL.New(sql).param("pciUuids", canAccessPcis)
                .param("hostUuids", iommuEnabledAndActiveHosts)
                .param("status", PciDeviceStatus.attachablePciDeviceStatus)
                .param("virtStatus", PciDeviceVirtStatus.attachablePciDeviceVirtStatus)
                .param("type", devTypes)
                .list();

        if (specUuids == null || specUuids.isEmpty()) {
            return Collections.emptyList();
        }

        List<PciDeviceSpecVO> specs = Q.New(PciDeviceSpecVO.class)
                .in(PciDeviceSpecVO_.uuid, specUuids)
                .eq(PciDeviceSpecVO_.state, PciDeviceSpecState.Enabled)
                .list();
        return PciDeviceSpecInventory.valueOf(specs);
    }

    private List<MdevDeviceSpecInventory> getAttachableMdevDeviceSpecsFromHosts(List<String> hostUuids, List<String> types, SessionInventory session) {
        List<String> iommuEnabledAndActiveHosts = getIommuEnabledAndActiveHosts(hostUuids);
        if (iommuEnabledAndActiveHosts == null || iommuEnabledAndActiveHosts.isEmpty()) {
            return Collections.emptyList();
        }

        List<MdevDeviceType> devTypes;
        if (types != null && !types.isEmpty()) {
            devTypes = types.stream().map(MdevDeviceType::valueOf).collect(Collectors.toList());
        } else {
            devTypes = asList(MdevDeviceType.values());
        }

        List<String> canAccessMdevs;
        if (acntMgr.isAdmin(session)) {
            canAccessMdevs = Q.New(MdevDeviceVO.class)
                    .in(MdevDeviceVO_.hostUuid, iommuEnabledAndActiveHosts)
                    .select(MdevDeviceVO_.uuid)
                    .listValues();
        } else {
            canAccessMdevs = acntMgr.getResourceUuidsCanAccessByAccount(session.getAccountUuid(), MdevDeviceVO.class);
        }
        if (canAccessMdevs == null || canAccessMdevs.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> specUuids = Q.New(MdevDeviceVO.class)
                .in(MdevDeviceVO_.uuid, canAccessMdevs)
                .in(MdevDeviceVO_.hostUuid, iommuEnabledAndActiveHosts)
                .eq(MdevDeviceVO_.state, MdevDeviceState.Enabled)
                .in(MdevDeviceVO_.status, MdevDeviceStatus.attachableMdevDeviceStatus)
                .in(MdevDeviceVO_.type, devTypes)
                .select(MdevDeviceVO_.mdevSpecUuid)
                .groupBy(MdevDeviceVO_.mdevSpecUuid)
                .listValues();
        if (specUuids == null || specUuids.isEmpty()) {
            return Collections.emptyList();
        }

        List<MdevDeviceSpecVO> specs = Q.New(MdevDeviceSpecVO.class)
                .in(MdevDeviceSpecVO_.uuid, specUuids)
                .eq(MdevDeviceSpecVO_.state, MdevDeviceSpecState.Enabled)
                .list();
        return MdevDeviceSpecInventory.valueOf(specs);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PciSpecificationConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        installSystemTagValidator();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void installSystemTagValidator() {
        installPciDeviceSpecValidator();
        installMdevDeviceSpecValidator();
    }

    private void installPciDeviceSpecValidator() {
        PciDeviceSystemTags.PCI_DEVICE_SPEC.installValidator((resourceUuid, resourceType, systemTag) -> {
            String specUuid = PciDeviceSystemTags.PCI_DEVICE_SPEC.getTokenByTag(systemTag,
                    PciDeviceSystemTags.PCI_DEVICE_SPEC_UUID_TOKEN);

            if (StringUtils.isBlank(specUuid) ||
                    !Q.New(PciDeviceSpecVO.class).eq(PciDeviceSpecVO_.uuid, specUuid).isExists()) {
                throw new ApiMessageInterceptionException(argerr(
                        "pci device spec[uuid:%s] doesn't exist", specUuid, systemTag));
            }
        });
    }

    private void installMdevDeviceSpecValidator() {
        MdevDeviceSystemTags.MDEV_DEVICE_SPEC.installValidator((resourceUuid, resourceType, systemTag) -> {
            String specUuid = MdevDeviceSystemTags.MDEV_DEVICE_SPEC.getTokenByTag(systemTag,
                    MdevDeviceSystemTags.MDEV_DEVICE_SPEC_UUID_TOKEN);

            if (StringUtils.isBlank(specUuid) ||
                    !Q.New(MdevDeviceSpecVO.class).eq(MdevDeviceSpecVO_.uuid, specUuid).isExists()) {
                throw new ApiMessageInterceptionException(argerr(
                        "mdev device spec[uuid:%s] doesn't exist", specUuid, systemTag));
            }
        });
    }

    @Override
    public List<String> getAddtionalResourceType() {
        return asList(PciDeviceSpecVO.class.getName(), MdevDeviceSpecVO.class.getName());
    }
}
