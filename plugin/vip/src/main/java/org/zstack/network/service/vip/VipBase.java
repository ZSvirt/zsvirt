package org.zstack.network.service.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.ReturnIpMsg;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.network.service.VirtualRouterHaGroupExtensionPoint;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/11/19.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VipBase {
    protected static final CLogger logger = Utils.getLogger(VipBase.class);

    protected VipVO self;

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected VipManager vipMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected EventFacade evtf;

    public String getThreadSyncSignature() {
        return String.format("vip-%s-%s", self.getName(), self.getUuid());
    }

    protected VipVO getSelf() {
        return self;
    }

    protected VipInventory getSelfInventory() {
        return VipInventory.valueOf(getSelf());
    }

    public VipBase(VipVO self) {
        this.self = self;
    }

    protected void refresh() {
        VipVO vo = dbf.reload(self);
        if (vo == null) {
            throw new OperationFailureException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find the vip[name:%s, uuid:%s, ip:%s], it may have been deleted",
                    self.getName(), self.getUuid(), self.getIp()
            ));
        }

        self = vo;
    }

    private void cleanInDB() {
        clearPeerL3Network();
        self.setServiceProvider(null);
        dbf.update(self);
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    protected void passToBackend(Message msg) {
        if (self.getServiceProvider() == null) {
            bus.dealWithUnknownMessage(msg);
            return;
        }

        VipFactory f = vipMgr.getVipFactory(self.getServiceProvider());
        VipBaseBackend bkd = f.getVip(getSelf());
        bkd.handleBackendSpecificMessage(msg);
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof VipDeletionMsg) {
            handle((VipDeletionMsg) msg);
        } else if (msg instanceof AcquireVipMsg) {
            handle((AcquireVipMsg) msg);
        } else if (msg instanceof ReleaseVipMsg) {
            handle((ReleaseVipMsg) msg);
        } else if (msg instanceof StopVipMsg) {
            handle((StopVipMsg) msg);
        } else {
            passToBackend(msg);
        }
    }

    protected boolean acquireCheckModifyVipAttributeStruct(ModifyVipAttributesStruct s) {
        if (s.isServiceProvider()) {
            if (self.getServiceProvider() != null && s.getServiceProvider() != null
                    && !s.getServiceProvider().equals(self.getServiceProvider())) {
                throw new OperationFailureException(operr("service provider of the vip[uuid:%s, name:%s, ip: %s] has been set to %s",
                        self.getUuid(), self.getName(), self.getIp(), self.getServiceProvider()));
            }
            self.setServiceProvider(s.getServiceProvider());
        }

        if (s.isPeerL3NetworkUuid()) {
            try {
                if (s.isServiceProvider()) {
                    s.getPeerL3NetworkUuids().forEach(this::addPeerL3NetworkUuid);
                }
            } catch (CloudRuntimeException e) {
                throw new OperationFailureException(operr(e.getMessage()));
            }
        }

        self = dbf.updateAndRefresh(self);

        /* system Vip don't need to be bound to backend */
        if (self.isSystem()) {
            return false;
        }

        return s.isPeerL3NetworkUuid() && s.isServiceProvider();
    }

    protected boolean CheckModifyVipAttributeStructWithoutReleaseService( ModifyVipAttributesStruct s, List<String> services) {
        if (services == null || services.isEmpty()){
            /*there are no any services using this vip*/
            return false;
        }

        if (s.isUserFor() && !services.contains(s.getUseFor())) {
            /*the service using this vip has been deleted. don't delete repeat*/
            return false;
        }

        if (self.isSystem()){
            /* system Vip don't need to be remove from backend */
            return false;
        }

        long activeNetworks = 0;
        long activeServices = 0;
        for (VipGetServiceReferencePoint ext : pluginRgty.getExtensionList(VipGetServiceReferencePoint.class)) {
            VipGetServiceReferencePoint.ServiceReference service = ext.getServiceReference(self.getUuid());
            activeServices += service.serviceUids.size();
            activeNetworks += service.count;
        }

        if (activeServices > 1) {
            return false;
        }

        /*the vip is active in this service, in another word, there are at least one nic/l3network to attach
             * with the vip */
        int deleting = 0;
        if (s.getPeerL3NetworkUuids() != null) {
            deleting = s.getPeerL3NetworkUuids().size();
        }
        return activeNetworks <= deleting;
    }

    protected boolean releaseCheckModifyVipAttributeStruct( ModifyVipAttributesStruct s, List<String> services) {
        if (services == null || services.isEmpty()){
            /*there are no any services using this vip*/
            return false;
        }

        if (s.isUserFor() && !services.contains(s.getUseFor())) {
            /*the service using this vip has been deleted. don't delete repeat*/
            return false;
        }

        if (services.size() == 1) {
            if (self.isSystem()) {
                /* snat is bound to router public interface, it is created automatically,
                 * so it should be deleted automatically, but don't need to remove from backend */
                return false;
            }

            return true;
        }


        if (self.isSystem()){
            /* system Vip don't need to be remove from backend */
            return false;
        }

        long activeServices = 0;
        for (VipGetServiceReferencePoint ext : pluginRgty.getExtensionList(VipGetServiceReferencePoint.class)) {
            VipGetServiceReferencePoint.ServiceReference service = ext.getServiceReference(self.getUuid());
            if (!service.useFor.equals(s.getUseFor())) {
                activeServices += service.serviceUids.size();
            } else if (service.serviceUids.contains(s.getServiceUuid())) {
                /* remove the service that will be deleted */
                activeServices += (service.serviceUids.size() - 1);
            } else {
                /* this case means, to be released service is inactive */
                activeServices += service.serviceUids.size();
            }
        }

        /*
        it doesn't support to release multi Services concurrently, so for release a service, we just check
        the active service count, if just one service ( that just delete one) or less, it means the vip can be release at backend.
        And at same time clear all the peer L3 record. ZSTAC-20506
        * */
        return (activeServices <= 0);
    }

    private void handle(ReleaseVipMsg msg) {
        ReleaseVipReply reply = new ReleaseVipReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {

                releaseVip(msg.getStruct(), true, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        VipInventory vip = VipInventory.valueOf(self);
                        reply.setInventory(vip);
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
                return "release-vip";
            }
        });
    }

    private void handle(StopVipMsg msg) {
        StopVipReply reply = new StopVipReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {

                releaseVip(msg.getStruct(), false, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        VipInventory vip = VipInventory.valueOf(self);
                        reply.setInventory(vip);
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
                return "release-vip";
            }
        });
    }

    protected void releaseVip(ModifyVipAttributesStruct s, Boolean releaseServices, Completion completion) {

        refresh();

        if (self.getServicesRefs() == null || self.getServicesRefs().isEmpty()){
            /*there are no any services using this vip*/
            /* no need to remove vip from backend */
            completion.success();
            return;
        }

        List<String> services = self.getServicesRefs().stream().map(VipNetworkServicesRefVO::getServiceType).collect(Collectors.toList());
        /* s == null is called from VipDeleteMsg, all service has been released */
        if ((s != null) && !releaseServices && (!CheckModifyVipAttributeStructWithoutReleaseService(s, services)) ||
                (s != null) && releaseServices && (!releaseCheckModifyVipAttributeStruct(s, services))) {
            try {
                if (s.getUseFor() != null && releaseServices) {
                    delServicesRef(s.getServiceUuid(),s.getUseFor());
                }
                if (s.isPeerL3NetworkUuid() && s.isServiceProvider() && !self.isSystem()) {
                    s.getPeerL3NetworkUuids().forEach(this::deletePeerL3Network);
                }
            } catch (CloudRuntimeException e) {
                throw new OperationFailureException(operr(e.getMessage()));
            }
            /* no need to remove vip from backend */
            completion.success();
            return;
        }

        if (self.getServiceProvider() == null) {
            logger.debug(String.format("the serviceProvider field is null, the vip[uuid:%s, name:%s, ip:%s] has been released" +
                    " by other service", self.getUuid(), self.getName(), self.getIp()));
            for (VipCleanupExtensionPoint ext : pluginRgty.getExtensionList(VipCleanupExtensionPoint.class)) {
                ext.cleanupVip(self.getUuid());
            }
            if (s != null && s.getUseFor() != null && releaseServices) {
                delServicesRef(s.getServiceUuid(),s.getUseFor());
            }
            completion.success();
            return;
        }

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(BeforeReleaseVipExtensionPoint.class),
                new ForEachFunction<BeforeReleaseVipExtensionPoint>() {
                    @Override
                    public void run(BeforeReleaseVipExtensionPoint ext) {
                        logger.debug(String.format("execute before release vip extension point %s", ext));
                        ext.beforeReleaseVip(VipInventory.valueOf(getSelf()));
                    }
                });

        VipFactory f = vipMgr.getVipFactory(self.getServiceProvider());
        VipBaseBackend vip = f.getVip(getSelf());
        vip.releaseVipOnBackend(new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully released vip[uuid:%s, name:%s, ip:%s] on service[%s]",
                        self.getUuid(), self.getName(), self.getIp(), self.getServiceProvider()));
                if (s != null && s.getUseFor() != null && releaseServices) {
                    delServicesRef(s.getServiceUuid(),s.getUseFor());
                }
                cleanInDB();
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to release vip[uuid:%s, name:%s, ip:%s] on service[%s], its garbage collector should" +
                        " handle this", self.getUuid(), self.getName(), self.getIp(), self.getServiceProvider()));
                completion.fail(errorCode);
            }
        });
    }

    protected void handle(AcquireVipMsg msg) {
        AcquireVipReply reply = new AcquireVipReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {

                acquireVip(msg.getStruct(), new Completion(msg, chain) {
                    @Override
                    public void success() {
                        VipInventory vip = VipInventory.valueOf(self);
                        reply.setVip(vip);
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
                return "acquire-vip";
            }
        });
    }

    protected void acquireVip(ModifyVipAttributesStruct s, Completion completion) {

        refresh();

        if (s.isUserFor() && s.getServiceUuid() != null) {
            addServicesRef(s.getServiceUuid(),s.getUseFor());
        }

        if (!acquireCheckModifyVipAttributeStruct(s)) {
            /* no need to install vip to backend */
            completion.success();
            return;
        }

        VipFactory f = vipMgr.getVipFactory(self.getServiceProvider());
        VipBaseBackend vip = f.getVip(getSelf());
        vip.acquireVipOnBackend(new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully acquired vip[uuid:%s, name:%s, ip:%s] on service[%s]",
                        self.getUuid(), self.getName(), self.getIp(), s.getServiceProvider()));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (s.isUserFor() && s.getServiceUuid() != null) {
                    delServicesRef(s.getServiceUuid(),s.getUseFor());
                }

                s.getPeerL3NetworkUuids().forEach(peer -> deletePeerL3Network(peer));
                completion.fail(errorCode);
            }
        });
    }

    protected void handle(VipDeletionMsg msg) {
        VipDeletionReply reply = new VipDeletionReply();
        VipInventory inventory = VipInventory.valueOf(self);
        String accountUuid = ResourceHelper.findResourceOwner(msg.getVipUuid());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                deleteVip(msg, new Completion(msg, chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);

                        VipCanonicalEvents.VipEventData vipEventData = new VipCanonicalEvents.VipEventData();
                        vipEventData.setVipUuid(msg.getVipUuid());
                        vipEventData.setCurrentStatus(VipCanonicalEvents.VIP_STATUS_DELETED);
                        vipEventData.setInventory(inventory);
                        vipEventData.setDate(new Date());
                        vipEventData.setAccountUuid(accountUuid);
                        evtf.fire(VipCanonicalEvents.VIP_DELETED_PATH, vipEventData);

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
                return "delete-vip";
            }
        });
    }

    protected void returnVip(Completion completion) {
        ReturnIpMsg msg = new ReturnIpMsg();
        msg.setL3NetworkUuid(self.getL3NetworkUuid());
        msg.setUsedIpUuid(self.getUsedIpUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, self.getL3NetworkUuid());
        bus.send(msg, new CloudBusCallBack(completion){
            @Override
            public void run(MessageReply reply) {
                completion.success();
            }
        });
    }

    private void releaseServicesOnVip(Iterator<String> it, FlowTrigger trigger){
        if(!it.hasNext()){
            trigger.next();
            return;
        }

        String useFor = it.next();
        if (useFor.equals(VipUseForList.SNAT_NETWORK_SERVICE_TYPE)){
            releaseServicesOnVip(it, trigger);
        } else if (!useFor.equals("null")) {
            VipReleaseExtensionPoint ext = vipMgr.getVipReleaseExtensionPoint(useFor);
            ext.releaseServicesOnVip(getSelfInventory(), new Completion(trigger) {
                @Override
                public void success() {
                    releaseServicesOnVip(it, trigger);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    releaseServicesOnVip(it, trigger);
                }
            });
        } else {
            releaseServicesOnVip(it, trigger);
        }
    }

    protected void deleteVip(VipDeletionMsg msg, Completion completion) {
        refresh();
        final Set<String> services = self.getServicesTypes();
        
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-vip-uuid-%s-ip-%s-name-%s", self.getUuid(), self.getIp(), self.getName()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "pre-release-services-on-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if(!services.isEmpty()){
                            trigger.next();
                            return;
                        }

                        /* this flow only work for the virtual public vip without service */
                        List<PreVipReleaseExtensionPoint> exts = pluginRgty.getExtensionList(PreVipReleaseExtensionPoint.class);
                        if(exts.isEmpty()){
                            trigger.next();
                            return;
                        }

                        new While<>(exts).each((ext, compl) -> {
                            ext.preReleaseServicesOnVip(VipInventory.valueOf(self), new Completion(compl) {
                                @Override
                                public void success() {
                                    compl.done();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    compl.done();
                                }
                            });
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "release-services-on-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if(services.isEmpty()){
                            trigger.next();
                            return;
                        }

                        List<String> types = new ArrayList<>(services);
                        Iterator<String> it = types.iterator();
                        releaseServicesOnVip(it, trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-vip";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if(services.isEmpty()){
                            trigger.next();
                            return;
                        }

                        releaseVip(null, true, new Completion(trigger) {
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

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        refresh();
                        dbf.remove(self);
                        if (msg.isReturnIp()) {
                            returnVip(completion);
                        } else {
                            completion.success();
                        }
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

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeVipStateMsg) {
            handle((APIChangeVipStateMsg) msg);
        } else if (msg instanceof APIDeleteVipMsg) {
            handle((APIDeleteVipMsg) msg);
        } else if (msg instanceof APIUpdateVipMsg)  {
            handle((APIUpdateVipMsg) msg);
        } else {
            passToBackend(msg);
        }
    }

    protected void handle(APIUpdateVipMsg msg) {
        VipVO vo = dbf.findByUuid(msg.getUuid(), VipVO.class);
        boolean update = false;
        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }
        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        APIUpdateVipEvent evt = new APIUpdateVipEvent(msg.getId());
        evt.setInventory(VipInventory.valueOf(vo));
        bus.publish(evt);
    }

    protected void handle(APIDeleteVipMsg msg) {
        final APIDeleteVipEvent evt = new APIDeleteVipEvent(msg.getId());
        refresh();

        final String issuer = VipVO.class.getSimpleName();
        final List<VipInventory> ctx = asList(VipInventory.valueOf(self));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-vip-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vip-permissive-check";

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
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vip-permissive-delete";

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
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-vip-force-delete";

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

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
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

    protected void handle(APIChangeVipStateMsg msg) {
        VipVO vip = dbf.findByUuid(msg.getUuid(), VipVO.class);
        VipStateEvent sevt = VipStateEvent.valueOf(msg.getStateEvent());
        vip.setState(vip.getState().nextState(sevt));
        vip = dbf.updateAndRefresh(vip);

        APIChangeVipStateEvent evt = new APIChangeVipStateEvent(msg.getId());
        evt.setInventory(VipInventory.valueOf(vip));
        bus.publish(evt);
    }

    private Boolean checkNicInSameVmInstance(VmNicVO enic, VmNicVO nnic, String peerL3NetworkUuid) {
        if (enic.getVmInstanceUuid().equals(nnic.getVmInstanceUuid())) {
            return true;
        }

        Boolean sameHaGroup = false;
        for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
            sameHaGroup = ext.isVirtualRouterInSameHaPair(asList(nnic.getVmInstanceUuid(), enic.getVmInstanceUuid()));
            if (sameHaGroup) {
                break;
            }
        }
        if (sameHaGroup) {
            return true;
        }

        throw new CloudRuntimeException(String.format("the request to add peer l3[uuid:%s] with vip[uuid:%s] has " +
                        "attched vr[uuid:%s] and is not vr[uuid:%s] which exists peer l3 attached", peerL3NetworkUuid, self.getUuid(),
                nnic.getVmInstanceUuid(), enic.getVmInstanceUuid()));
    }

    public Boolean checkPeerL3Additive(String peerL3NetworkUuid) {
        if (peerL3NetworkUuid == null) {
            return false;
        }

        VmNicVO vipNic = null;
        if (self.getPeerL3NetworkRefs() == null || self.getPeerL3NetworkRefs().isEmpty()) {
            vipNic = Q.New(VmNicVO.class).eq(VmNicVO_.usedIpUuid, self.getUsedIpUuid()).limit(1).find();
            if (vipNic == null) {
                return true;
            }
        } else if (self.getPeerL3NetworkRefs().stream()
                .anyMatch(ref -> ref.getL3NetworkUuid().equals(peerL3NetworkUuid))) {
            logger.debug(String.format("peer l3 [uuid:%s] has already add to vip[uuid:%s], skip to add",
                    peerL3NetworkUuid, self.getUuid()));
            return false;
        }

        VmNicVO nnic = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, peerL3NetworkUuid)
                .notNull(VmNicVO_.metaData).limit(1).find();
        if (nnic == null) {
            logger.debug(String.format("add peer l3[uuid:%s] to vip[uuid:%s], the l3 has no vr attached now",
                    peerL3NetworkUuid, self.getUuid()));
            return true;
        }

        if (vipNic != null) {
            return checkNicInSameVmInstance(vipNic, nnic, peerL3NetworkUuid);
        } else {
            for (VipPeerL3NetworkRefVO ref : self.getPeerL3NetworkRefs()) {
                VmNicVO enic = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, ref.getL3NetworkUuid())
                        .notNull(VmNicVO_.metaData).limit(1).find();
                if (enic == null) {
                    continue;
                }

                if (checkNicInSameVmInstance(enic, nnic, peerL3NetworkUuid)){
                    continue;
                }
            }
        }
        return true;
    }

    private Boolean checkPeerL3Deleteable(String peerL3NetworkUuid) {
        refresh();

        if (self.getPeerL3NetworkRefs() == null || self.getPeerL3NetworkRefs().isEmpty()) {
            return false;
        }

        if (self.getPeerL3NetworkRefs().stream()
                .anyMatch(ref -> ref.getL3NetworkUuid().equals(peerL3NetworkUuid))) {
            /*
            * check if there are services to use the l3
            * */
            int useCount = 0;
            int uuidCount = 0;
            for (VipGetServiceReferencePoint ext : pluginRgty.getExtensionList(VipGetServiceReferencePoint.class)) {
                VipGetServiceReferencePoint.ServiceReference service = ext.getServicePeerL3Reference(self.getUuid(), peerL3NetworkUuid);
                uuidCount += service.serviceUids.size();
                useCount += service.count ;
            }

            if ( uuidCount <= 1) {
                return useCount <= 1;
            }
        }

        return false;
    }

    public void addPeerL3NetworkUuid(String peerL3NetworkUuid) {
        if (!checkPeerL3Additive(peerL3NetworkUuid)) {
            logger.debug(String.format("can not add peer l3[uuid:%s] to vip[uuid:%s]",
                    peerL3NetworkUuid, self.getUuid()));
            return;
        }

        if (Q.New(VipPeerL3NetworkRefVO.class)
                .eq(VipPeerL3NetworkRefVO_.vipUuid, self.getUuid())
                .eq(VipPeerL3NetworkRefVO_.l3NetworkUuid, peerL3NetworkUuid)
                .find() != null) {
            logger.debug(String.format("peer l3 [uuid:%s] has already add to vip[uuid:%s], skip to add",
                    peerL3NetworkUuid, self.getUuid()));
            return;
        }
        VipPeerL3NetworkRefVO vo = new VipPeerL3NetworkRefVO();
        vo.setVipUuid(self.getUuid());
        vo.setL3NetworkUuid(peerL3NetworkUuid);
        dbf.persistAndRefresh(vo);
        logger.debug(String.format("added peer l3[uuid:%s] to vip[uuid:%s]",
                peerL3NetworkUuid, self.getUuid()));
    }

    private void deletePeerL3Network(String peerL3NetworkUuid) {
        if (checkPeerL3Deleteable(peerL3NetworkUuid)) {
            deletePeerL3NetworkUuid(peerL3NetworkUuid);
        }
    }

    public void deletePeerL3NetworkUuid(String peerL3NetworkUuid) {
        refresh();
        VipPeerL3NetworkRefVO vo = Q.New(VipPeerL3NetworkRefVO.class)
                .eq(VipPeerL3NetworkRefVO_.vipUuid, self.getUuid())
                .eq(VipPeerL3NetworkRefVO_.l3NetworkUuid, peerL3NetworkUuid)
                .find();
        if (vo != null) {
            dbf.remove(vo);
        }
        logger.debug(String.format("deleted peer l3[uuid:%s] from vip[uuid:%s]",
                peerL3NetworkUuid, self.getUuid()));
    }

    public void clearPeerL3Network() {
        List<VipPeerL3NetworkRefVO> vos = Q.New(VipPeerL3NetworkRefVO.class)
                .eq(VipPeerL3NetworkRefVO_.vipUuid, self.getUuid())
                .list();
        if (vos != null && !vos.isEmpty()) {
            dbf.removeCollection(vos, VipPeerL3NetworkRefVO.class);
        }
        self.setPeerL3NetworkRefs(null);
        refresh();
    }

    private void addServicesRef(String uuid, String type) {
        VipNetworkServicesRefVO vipRef = new VipNetworkServicesRefVO();

        if (Q.New(VipNetworkServicesRefVO.class)
                .eq(VipNetworkServicesRefVO_.uuid, uuid)
                .eq(VipNetworkServicesRefVO_.vipUuid, self.getUuid())
                .eq(VipNetworkServicesRefVO_.serviceType, type).isExists()) {
            logger.debug(String.format("repeat to add the servicesRef [type:%s:uuid:%s] with vip[uuid:%s]",
                    type, uuid, self.getUuid()));
            return;
        }
        vipRef.setUuid(uuid);
        vipRef.setServiceType(type);
        vipRef.setVipUuid(self.getUuid());
        dbf.persist(vipRef);
        Set<String> types = self.getServicesTypes();
        if (  types == null ) {
            self.setUseFor(type);
            self = dbf.updateAndRefresh(self);
        } else if (!types.contains(type)) {
            types.add(type);
            self.setUseFor(new VipUseForList(types).toString());
            self = dbf.updateAndRefresh(self);
        }

        logger.debug(String.format("add the servicesRef [type:%s:uuid:%s] with vip[uuid:%s]",
                type, uuid, self.getUuid()));
    }

    private void delServicesRef(String uuid, String type) {
        DebugUtils.Assert((uuid != null) && (type != null), "the parameter can't be null");
        VipNetworkServicesRefVO vipRef = Q.New(VipNetworkServicesRefVO.class)
                .eq(VipNetworkServicesRefVO_.uuid, uuid)
                .eq(VipNetworkServicesRefVO_.serviceType, type)
                .eq(VipNetworkServicesRefVO_.vipUuid, self.getUuid()).find();
        if ( vipRef == null ) {
            logger.error(String.format("the servicesRef [type:%s:uuid:%s] with vip[uuid:%s] doesn't exist",
                    type, uuid, self.getUuid()));
            return;
        }

        dbf.remove(vipRef);
        refresh();
        Set<String> types = self.getServicesTypes();
        if (  types == null ) {
            self.setUseFor(null);
            self = dbf.updateAndRefresh(self);
        } else if (!types.contains(type)) {
            self.setUseFor(new VipUseForList(types).toString());
            self = dbf.updateAndRefresh(self);
        }

        logger.debug(String.format("delete the servicesRef [type:%s:uuid:%s] with vip[uuid:%s]",
                type, uuid, self.getUuid()));
    }

    public void delSystemServiceRef() {
        self.setSystem(false);
        dbf.update(self);
        if (self.getServicesTypes().contains(NetworkServiceType.SNAT.toString())) {
            VipNetworkServicesRefVO vipRef = Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, self.getUuid())
                    .eq(VipNetworkServicesRefVO_.serviceType, NetworkServiceType.SNAT.toString()).find();
            if(vipRef != null) {
                delServicesRef(vipRef.getUuid(), vipRef.getServiceType());
            }
        }
    }
}
