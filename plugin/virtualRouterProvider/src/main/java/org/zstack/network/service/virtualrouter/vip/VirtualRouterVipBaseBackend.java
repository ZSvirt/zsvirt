package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.VirtualRouterHaTask;
import org.zstack.header.vm.*;
import org.zstack.network.service.vip.AfterAcquireVipExtensionPoint;
import org.zstack.network.service.vip.VipBaseBackend;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.ha.VirtualRouterHaBackend;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/11/20.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterVipBaseBackend extends VipBaseBackend {
    private static final CLogger logger = Utils.getLogger(VirtualRouterVipBaseBackend.class);

    @Autowired
    protected VirtualRouterManager vrMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private VipConfigProxy proxy;
    @Autowired
    private VirtualRouterHaBackend haBackend;

    public VirtualRouterVipBaseBackend(VipVO self) {
        super(self);
    }

    protected void releaseVipOnHaHaRouter(VirtualRouterVmInventory vrInv, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(VirtualRouterVipBackend.RELEASE_VIP_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(getSelfInventory()));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    protected void acquireVipOnHaBackend(VirtualRouterVmInventory vrInv, Completion completion) {
        VirtualRouterHaTask task = new VirtualRouterHaTask();
        task.setTaskName(VirtualRouterVipBackend.APPLY_VIP_TASK);
        task.setOriginRouterUuid(vrInv.getUuid());
        task.setJsonData(JSONObjectUtil.toJsonString(getSelfInventory()));
        haBackend.submitVirtualRouterHaTask(task, completion);
    }

    @Override
    protected void releaseVipOnBackend(Completion completion) {
        List<String> vrs = proxy.getVrUuidsByNetworkService(VipVO.class.getSimpleName(), self.getUuid());
        if (vrs == null || vrs.isEmpty()) {
            completion.success();
            return;
        }

        String vrUuid = vrs.get(0);
        final VirtualRouterVmVO vrvo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vrvo);
        if (vrvo.getState() != VmInstanceState.Running) {
            // vr will sync when becomes Running
            proxy.detachNetworkService(vrUuid, VipVO.class.getSimpleName(), asList(self.getUuid()));
            releaseVipOnHaHaRouter(vrInv, completion);
            return;
        }

        releaseVipOnVirtualRouterVm(vrInv, asList(getSelfInventory()), new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully released vip[uuid:%s, name:%s, ip:%s] on virtual router vm[uuid:%s]",
                        self.getUuid(), self.getName(), self.getIp(), vrvo.getUuid()));
                proxy.detachNetworkService(vrUuid, VipVO.class.getSimpleName(), asList(self.getUuid()));
                releaseVipOnHaHaRouter(vrInv, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(errorCode.getReadableDetails());
                // NOTE: we don't know if the failure happens before or after the VIP deleted on the real device,
                // for example, a message timeout failure happens after the VIP gets really deleted, but an internal error
                // may happen before so. In both cases, we delete the database reference here so next time the backend
                // will try to apply the VIP again. It's virtualrouter/vyos's responsibility to succeed if a VIP is applied
                // while it exists
                proxy.detachNetworkService(vrUuid, VipVO.class.getSimpleName(), asList(self.getUuid()));
                releaseVipOnHaHaRouter(vrInv, new NopeCompletion());
                completion.fail(errorCode);
            }
        });
    }

    private String getOwnerMac(VirtualRouterVmInventory vr, VipInventory vip) {
        for (VmNicInventory nic : vr.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(vip.getL3NetworkUuid())) {
                return nic.getMac();
            }
        }

        return null;
    }

    private void releaseVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, List<VipInventory> vips, final Completion completion) {
        final List<VirtualRouterCommands.VipTO> tos = new ArrayList<VirtualRouterCommands.VipTO>(vips.size());
        for (VipInventory vip : vips) {
            String mac = getOwnerMac(vr, vip);
            if (mac == null) {
                /*the nic will be detached during the vip network deleted */
                continue;
            }
            VirtualRouterCommands.VipTO to = VirtualRouterCommands.VipTO.valueOf(vip, mac);
            tos.add(to);
        }

        if (tos.isEmpty()) {
            completion.success();
            return;
        }

        VirtualRouterCommands.RemoveVipCmd cmd = new VirtualRouterCommands.RemoveVipCmd();
        cmd.setVips(tos);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setPath(VirtualRouterConstant.VR_REMOVE_VIP);
        msg.setCommand(cmd);
        msg.setVmInstanceUuid(vr.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.RemoveVipRsp ret = re.toResponse(VirtualRouterCommands.RemoveVipRsp.class);
                if (ret.isSuccess()) {
                    completion.success();
                } else {
                    ErrorCode err = operr("failed to remove vip%s, because %s", tos, ret.getError());
                    completion.fail(err);
                }
            }
        });
    }

    public void createVipOnVirtualRouterVm(final VirtualRouterVmInventory vr, List<VipInventory> vips, final Completion completion) {
        final List<VirtualRouterCommands.VipTO> tos = new ArrayList<VirtualRouterCommands.VipTO>(vips.size());
        List<VipInventory> systemVip = vips.stream().filter(VipInventory::isSystem).collect(Collectors.toList());
        List<VipInventory> notSystemVip = vips.stream().filter(v -> !v.isSystem()).collect(Collectors.toList());
        List<VipInventory> vipss = new ArrayList<>();
        vipss.addAll(systemVip);
        vipss.addAll(notSystemVip);

        for (VipInventory vip : vipss) {
            String mac = getOwnerMac(vr, vip);
            if (mac == null) {
                throw new CloudRuntimeException(String.format("virtual router vm[uuid:%s] has no nic on l3Network[uuid:%s] for vip[uuid:%s, ip:%s]",
                            vr.getUuid(), vip.getL3NetworkUuid(), vip.getUuid(), vip.getIp()));
            }
            VirtualRouterCommands.VipTO to = VirtualRouterCommands.VipTO.valueOf(vip, mac);
            tos.add(to);
        }

        VirtualRouterCommands.CreateVipCmd cmd = new VirtualRouterCommands.CreateVipCmd();
        cmd.setVips(tos);

        VirtualRouterAsyncHttpCallMsg msg = new VirtualRouterAsyncHttpCallMsg();
        msg.setVmInstanceUuid(vr.getUuid());
        msg.setCommand(cmd);
        msg.setPath(VirtualRouterConstant.VR_CREATE_VIP);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vr.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                VirtualRouterAsyncHttpCallReply re = reply.castReply();
                VirtualRouterCommands.CreateVipRsp ret = re.toResponse(VirtualRouterCommands.CreateVipRsp.class);
                if (!ret.isSuccess()) {
                    ErrorCode err = operr("failed to create vip%s on virtual router[uuid:%s], because %s", tos, vr.getUuid(), ret.getError());
                    completion.fail(err);
                } else {
                    completion.success();
                }
            }
        });
    }

    @Override
    protected void acquireVipOnBackend(Completion completion) {
        refresh();

        List<String> vrs = proxy.getVrUuidsByNetworkService(VipVO.class.getSimpleName(), self.getUuid());
        if (vrs != null && !vrs.isEmpty()) {
            String vrUuid = vrs.get(0);

            logger.debug(String.format("vip already attached to virtual router [uuid:%s]", vrUuid));
            VmInstanceState vrState = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.state)
                    .eq(VmInstanceVO_.uuid, vrUuid)
                    .findValue();

            if (VmInstanceState.Running != vrState) {
                completion.fail(operr("virtual router[uuid:%s, state:%s] is not running",
                        vrUuid, vrState));
            } else {
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterAcquireVipExtensionPoint.class),
                        new ForEachFunction<AfterAcquireVipExtensionPoint>() {
                            @Override
                            public void run(AfterAcquireVipExtensionPoint ext) {
                                logger.debug(String.format("execute after acquire vip extension point %s", ext));
                                ext.afterAcquireVip(VipInventory.valueOf(getSelf()));
                            }
                        });

                completion.success();
            }

            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-vr-for-vip-%s-%s", self.getUuid(), self.getIp()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, final Map data) {
                DebugUtils.Assert(self.getPeerL3NetworkUuids() != null, "peerL3NetworkUuid cannot be null");

                VirtualRouterStruct s = new VirtualRouterStruct();
                s.setL3Network(L3NetworkInventory.valueOf(dbf.findByUuid(self.getPeerL3NetworkUuids().iterator().next(), L3NetworkVO.class)));

                String appVmType;
                if (self.getServiceProvider().equals(VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE)) {
                    appVmType = VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE;
                } else if (self.getServiceProvider().equals(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE)) {
                    appVmType = VyosConstants.VYOS_VM_TYPE;
                } else {
                    throw new CloudRuntimeException(String.format("unknown network service provider type[%s]", self.getServiceProvider()));
                }

                s.setApplianceVmType(appVmType);
                s.setProviderType(self.getServiceProvider());
                s.setOfferingValidator(offering -> {
                    if (!offering.getPublicNetworkUuid().equals(self.getL3NetworkUuid())) {
                        throw new OperationFailureException(operr("found a virtual router offering[uuid:%s] for L3Network[uuid:%s] in zone[uuid:%s]; however, the network's public network[uuid:%s] is not the same to VIP[uuid:%s]'s; you may need to use system tag" +
                                        " guestL3Network::l3NetworkUuid to specify a particular virtual router offering for the L3Network",
                                offering.getUuid(), s.getL3Network().getUuid(), s.getL3Network().getZoneUuid(),
                                self.getL3NetworkUuid(), self.getUuid()));
                    }
                });

                vrMgr.acquireVirtualRouterVm(s, new ReturnValueCompletion<VirtualRouterVmInventory>(trigger){
                    @Override
                    public void success(VirtualRouterVmInventory returnValue) {
                        data.put(VirtualRouterConstant.Param.VR.toString(), returnValue);
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
                final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
                createVipOnVirtualRouterVm(vr, Arrays.asList(getSelfInventory()), new Completion(trigger) {
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
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
                proxy.attachNetworkService(vr.getUuid(), VipVO.class.getSimpleName(), asList(self.getUuid()));
                CollectionUtils.safeForEach(pluginRgty.getExtensionList(AfterAcquireVipExtensionPoint.class),
                        new ForEachFunction<AfterAcquireVipExtensionPoint>() {
                            @Override
                            public void run(AfterAcquireVipExtensionPoint ext) {
                                logger.debug(String.format("execute after acquire vip extension point %s", ext));
                                ext.afterAcquireVip(VipInventory.valueOf(getSelf()));
                            }
                        });

                acquireVipOnHaBackend(vr, completion);
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    protected void handleBackendSpecificMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }
}
