package org.zstack.pciDevice.virtual.vfio_mdev;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.compute.host.PostHostConnectExtensionPoint;
import org.zstack.compute.vm.VmCapabilitiesExtensionPoint;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.ha.HaHostDeviceExtensionPoint;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AddtionalResourceTypeExtensionPoint;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmAddonExtensionPoint;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.specification.mdev.*;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmMsg;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVolumeMsg;
import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;

/**
 * Created by GuoYi on 2019-05-07.
 */
public class MdevDeviceManagerImpl extends AbstractService implements
        MdevDeviceManager,
        HaHostDeviceExtensionPoint,
        GlobalApiMessageInterceptor,
        PostHostConnectExtensionPoint,
        VmCapabilitiesExtensionPoint,
        KVMStartVmAddonExtensionPoint,
        VmInstanceMigrateExtensionPoint,
        VmReleaseResourceExtensionPoint,
        VmAbnormalLifeCycleExtensionPoint,
        AddtionalResourceTypeExtensionPoint,
        PreVmInstantiateResourceExtensionPoint,
        PostVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MdevDeviceManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private EventFacade evf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private PciDeviceManagerInterface pciMgr;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APIGetMdevDeviceCandidatesMsg) {
            handle((APIGetMdevDeviceCandidatesMsg) msg);
        } else if (msg instanceof MdevDeviceMessage) {
            passThrough((MdevDeviceMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetMdevDeviceCandidatesMsg msg) {
        APIGetMdevDeviceCandidatesReply reply = new APIGetMdevDeviceCandidatesReply();
        List<String> hostUuids = new ArrayList<>();

        if (StringUtils.isNotBlank(msg.getVmInstanceUuid())) {
            VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
            String attachedMdevHost = Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .select(MdevDeviceVO_.hostUuid)
                    .limit(1)
                    .findValue();
            if (StringUtils.isNotBlank(attachedMdevHost)) {
                hostUuids.add(attachedMdevHost);
            } else {
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
                            bus.reply(msg, reply);
                        } else {
                            GetVmStartingCandidateClustersHostsReply grly = (GetVmStartingCandidateClustersHostsReply) rly;
                            hostUuids.addAll(grly.getHostInventories().stream().map(HostInventory::getUuid).collect(Collectors.toList()));
                            reply.setInventories(getAccountCanAccessMdevDevices(
                                    getAttachableMdevDevicesFromHosts(hostUuids, msg.getTypes()), msg.getSession()));
                            bus.reply(msg, reply);
                        }
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

        reply.setInventories(getAccountCanAccessMdevDevices(
                getAttachableMdevDevicesFromHosts(hostUuids, msg.getTypes()), msg.getSession()));
        bus.reply(msg, reply);
    }

    private List<MdevDeviceInventory> getAccountCanAccessMdevDevices(List<MdevDeviceInventory> mdevs, SessionInventory session) {
        if (mdevs == null || mdevs.isEmpty()) {
            return mdevs;
        } else if (acntMgr.isAdmin(session)) {
            return mdevs;
        }

        final String accountUuid = session.getAccountUuid();
        List<String> canAccessMdevUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, MdevDeviceVO.class);
        if (canAccessMdevUuids == null || canAccessMdevUuids.isEmpty()) {
            return Collections.emptyList();
        }

        return mdevs.stream().filter(i -> canAccessMdevUuids.contains(i.getUuid())).collect(Collectors.toList());
    }

    private List<String> getIommuEnabledAndActiveHosts(List<String> hostUuids) {
        HostIommuGetter getter = new HostIommuGetter();
        return hostUuids.stream()
                .filter(hostUuid -> getter.getState(hostUuid).equals(HostIommuStateType.Enabled))
                .filter(hostUuid -> getter.getStatus(hostUuid).equals(HostIommuStatusType.Active))
                .collect(Collectors.toList());
    }

    private List<MdevDeviceInventory> getAttachableMdevDevicesFromHosts(List<String> hostUuids, List<String> types) {
        hostUuids = getIommuEnabledAndActiveHosts(hostUuids);
        if (hostUuids == null || hostUuids.isEmpty()) {
            return Collections.emptyList();
        }

        List<MdevDeviceType> devTypes;
        if (types != null && !types.isEmpty()) {
            devTypes = types.stream().map(MdevDeviceType::valueOf).collect(Collectors.toList());
        } else {
            devTypes = asList(MdevDeviceType.values());
        }

        List<MdevDeviceVO> mdevs = Q.New(MdevDeviceVO.class)
                .in(MdevDeviceVO_.hostUuid, hostUuids)
                .eq(MdevDeviceVO_.state, MdevDeviceState.Enabled)
                .in(MdevDeviceVO_.status, MdevDeviceStatus.attachableMdevDeviceStatus)
                .in(MdevDeviceVO_.type, devTypes)
                .list();
        return MdevDeviceInventory.valueOf(mdevs);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CheckAndReserveMdevDeviceBySpecMsg) {
            handle((CheckAndReserveMdevDeviceBySpecMsg) msg);
        } else if (msg instanceof MdevDeviceMessage) {
            passThrough((MdevDeviceMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }
    private void handle(CheckAndReserveMdevDeviceBySpecMsg msg) {
        CheckAndReserveMdevDeviceBySpecReply reply = new CheckAndReserveMdevDeviceBySpecReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return MdevDeviceConstants.CHECK_AND_RESERVE_MDEV_DEVICE_FOR_VM;
            }

            @Override
            public void run(SyncTaskChain chain) {
                String hostUuid = msg.getHostUuid();
                String vmUuid = msg.getVmUuid();

                String accountUuid = acntMgr.getOwnerAccountUuidOfResource(vmUuid);
                List<String> accessibleMdevUuids = new ArrayList<>();
                if (!accountUuid.equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
                    // QuotaChecker cannot handle the scenario of normal account batch creating vms with pci/mdev specs.
                    // New created vms allocate host one by one, so we can check quota again here.
                    // The request info is transform from system tags into database, just `getUsedPci` from db and check.
                    Map<String, Quota.QuotaPair> pairs = new QuotaUtil().makeQuotaPairs(accountUuid);
                    for (PciDeviceType deviceType : PciDeviceType.leagalPciDeviceCandidateTypes) {
                        try {
                            PciDeviceQuotaUtils.check(accountUuid, deviceType.toString(), 0L, pairs);
                        } catch (RuntimeException e) {
                            reply.setError(operr(e.getMessage()));
                            bus.reply(msg, reply);
                            chain.next();
                            return;
                        }
                    }

                    accessibleMdevUuids = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, MdevDeviceVO.class);
                    if (CollectionUtils.isEmpty(accessibleMdevUuids)) {
                        accessibleMdevUuids = Collections.singletonList(Platform.FAKE_UUID);
                    }
                }

                Map<String, List<String>> specDevMap = new HashMap<>();
                Map<String, Integer> specMap = MdevDeviceUtils.getVmMdevSpecUuids(vmUuid);
                for (Map.Entry<String, Integer> entry : specMap.entrySet()) {
                    String specUuid = entry.getKey();
                    int deviceNum = entry.getValue();
                    long attachedNum = Q.New(MdevDeviceVO.class)
                            .eq(MdevDeviceVO_.vmInstanceUuid, vmUuid)
                            .eq(MdevDeviceVO_.mdevSpecUuid, specUuid)
                            .eq(MdevDeviceVO_.chooser, MdevDeviceChooser.Spec)
                            .count();
                    if (deviceNum > attachedNum) {
                        logger.error(String.format("more than %d mdev devices related to spec[uuid:%s] are attached to vm[uuid:%s]",
                                deviceNum, specUuid, vmUuid));
                    }

                    // still need `deviceNum` mdevs of `specUuid` for vm
                    deviceNum -= attachedNum;
                    if (deviceNum == 0) {
                        continue;
                    }

                    // find all available mdevs in dest host
                    Q query = Q.New(MdevDeviceVO.class)
                            .eq(MdevDeviceVO_.hostUuid, hostUuid)
                            .eq(MdevDeviceVO_.mdevSpecUuid, specUuid)
                            .eq(MdevDeviceVO_.state, MdevDeviceState.Enabled)
                            .in(MdevDeviceVO_.status, MdevDeviceStatus.attachableMdevDeviceStatus)
                            .select(MdevDeviceVO_.uuid);

                    if (!accountUuid.equals(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)) {
                        query = query.in(MdevDeviceVO_.uuid, accessibleMdevUuids);
                    }

                    List<String> mdevsOneSpec = query.listValues();

                    if (mdevsOneSpec.size() < deviceNum) {
                        reply.setError(Platform.operr("failed to find enough mdev device of spec[uuid:%s] in dest host[uuid:%s] for vm[uuid:%s]",
                                specUuid, hostUuid, vmUuid));
                        bus.reply(msg, reply);
                        chain.next();
                        return;
                    }

                    if (!mdevsOneSpec.isEmpty()) {
                        specDevMap.put(specUuid, mdevsOneSpec.stream().limit(deviceNum).collect(Collectors.toList()));
                    }
                }

                // destHost has enough spec related mdev devices for vm
                List<String> reservedMdevDevices = new ArrayList<>();
                if (msg.isDryRun()) {
                    logger.debug(String.format("host[uuid:%s] has enough spec related mdev devices for vm[uuid:%s], " +
                            "they are not reserved because dry run", hostUuid, vmUuid));
                    reply.setReservedMdevDevices(reservedMdevDevices);
                    bus.reply(msg, reply);
                    chain.next();
                    return;
                }

                for (Map.Entry<String, List<String>> entry : specDevMap.entrySet()) {
                    MdevDeviceUtils.reserveMdevDeviceInDB(entry.getValue(), vmUuid, MdevDeviceChooser.Spec);
                    reservedMdevDevices.addAll(entry.getValue());
                }

                logger.debug(String.format("reserved mdev device[uuid:%s] on host[uuid:%s] for vm[uuid:%s]", reservedMdevDevices, hostUuid, vmUuid));
                reply.setReservedMdevDevices(reservedMdevDevices);
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void passThrough(MdevDeviceMessage msg) {
        MdevDeviceVO vo = dbf.findByUuid(msg.getMdevDeviceUuid(), MdevDeviceVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr(
                    "cannot find mdev device[uuid:%s], it may have been deleted", msg.getMdevDeviceUuid()));
        }

        new MdevDeviceBase(vo).handleMessage((Message) msg);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(MdevDeviceConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        setupCanonicalEvents();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void setupCanonicalEvents() {
        // make sure mdev devices are automatically detached (if need to) when vm is stopped bypass zstack
        evf.on(VmCanonicalEvents.VM_LIBVIRT_REPORT_SHUTDOWN, new EventCallback<Object>() {
            @Override
            protected void run(Map tokens, Object data) {
                String vmUuid = (String) data;
                VmInstanceState state = Q.New(VmInstanceVO.class)
                        .select(VmInstanceVO_.state)
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .findValue();
                if (state == VmInstanceState.Rebooting) {
                    return;
                }

                detachMdevDevicesFromVm(vmUuid, false,
                        MdevDeviceUtils.releaseSpecReleatedVirtualMdevDevicesWhenStop(vmUuid),
                        new Completion(null) {
                            @Override
                            public void success() {
                                logger.debug(String.format("auto detached mdev devices from vm[uuid:%s] because it's been stopped bypass zstack", vmUuid));
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.error(String.format("failed to auto detach mdev devices from vm[uuid:%s] because it's been stopped bypass zstack", vmUuid));
                            }
                        });
            }
        });
    }

    /**
     * release different kinds of mdev devices from vm
     * @param vmUuid  virtual machine uuid
     * @param releaseNonSpec  release non-spec-releated mdev devices
     * @param releaseVirtualSpec release virtual-spec-releated mdev devices
     */
    private void detachMdevDevicesFromVm(String vmUuid, boolean releaseNonSpec, boolean releaseVirtualSpec, Completion completion) {
        MdevDeviceUtils.detachMdevDeviceForVmInDB(vmUuid, null, MdevDeviceStatus.Reserved, null);

        List<String> mdevUuids = getNeedToDetachMdevDevices(vmUuid, releaseNonSpec, releaseVirtualSpec);
        if (mdevUuids.isEmpty()) {
            completion.success();
            return;
        }

        logger.debug(String.format("going to detach mdev devices from vm[uuid:%s], releaseNonSpec[%s], releaseVirtualSpec[%s]",
                vmUuid, releaseNonSpec, releaseVirtualSpec));

        // call DetachMdevDeviceMsg on above mdev devices
        new While<>(mdevUuids).each((mdevUuid, comp) -> {
            DetachMdevDeviceMsg dmsg = new DetachMdevDeviceMsg();
            dmsg.setVmInstanceUuid(vmUuid);
            dmsg.setMdevDeviceUuid(mdevUuid);
            bus.makeLocalServiceId(dmsg, MdevDeviceConstants.SERVICE_ID);
            bus.send(dmsg, new CloudBusCallBack(comp) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        logger.debug(String.format("detached mdev device[uuid:%s] from vm[uuid:%s]", mdevUuid, vmUuid));
                    } else {
                        logger.error(String.format("failed to detach mdev device[uuid:%s] from vm[uuid:%s], %s", mdevUuid, vmUuid, reply.getError()));
                    }
                    comp.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    @Transactional(readOnly = true)
    public List<String> getNeedToDetachMdevDevices(String vmUuid, boolean releaseNonSpec, boolean releaseVirtualSpec) {
        // get uuids of mdev devices that are going to be released
        List<String> mdevUuids = new ArrayList<>();

        if (!releaseNonSpec && !releaseVirtualSpec) {
            return mdevUuids;
        }

        // get uuids of all mdev devices attached to vmUuid
        List<String> allMdevs = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.vmInstanceUuid, vmUuid)
                .select(MdevDeviceVO_.uuid)
                .listValues();
        if (allMdevs.isEmpty()) {
            return mdevUuids;
        }

        // get uuids of non-spec-released mdev devices
        List<String> specMdevs = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.vmInstanceUuid, vmUuid)
                .eq(MdevDeviceVO_.chooser, MdevDeviceChooser.Spec)
                .select(MdevDeviceVO_.uuid)
                .listValues();
        allMdevs.removeAll(specMdevs);
        if (releaseNonSpec && !allMdevs.isEmpty()) {
            mdevUuids.addAll(allMdevs);
        }

        // get uuids of spec-related mdev devices
        List<String> specUuids = Q.New(VmInstanceMdevDeviceSpecRefVO.class)
                .eq(VmInstanceMdevDeviceSpecRefVO_.vmInstanceUuid, vmUuid)
                .select(VmInstanceMdevDeviceSpecRefVO_.mdevSpecUuid)
                .listValues();
        if (releaseVirtualSpec && !specUuids.isEmpty()) {
            mdevUuids.addAll(Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.vmInstanceUuid, vmUuid)
                    .eq(MdevDeviceVO_.chooser, MdevDeviceChooser.Spec)
                    .in(MdevDeviceVO_.mdevSpecUuid, specUuids)
                    .select(MdevDeviceVO_.uuid)
                    .listValues());
        }

        return mdevUuids;
    }

    @Transactional
    public void detachMdevDeviceFromVmInDb(List<String> mdevUuids) {
        for (String mdevUuid : mdevUuids) {
            MdevDeviceVO mdev = dbf.findByUuid(mdevUuid, MdevDeviceVO.class);
            String vmInstanceUuid = mdev.getVmInstanceUuid();
            if (vmInstanceUuid == null) {
                continue;
            }

            // detach mdev device from vm
            mdev.setVmInstanceUuid(null);
            mdev.setChooser(MdevDeviceChooser.None);
            mdev.setStatus(MdevDeviceStatus.Active);
            dbf.updateAndRefresh(mdev);
        }
    }

    @Override
    public void checkVmCapability(VmInstanceInventory inv, VmCapabilities capabilities) {
        // can not do live migration if vm has mdev devices attached to it
        if (!capabilities.isSupportLiveMigration()) {
            return;
        }

        if (Q.New(MdevDeviceVO.class).eq(MdevDeviceVO_.vmInstanceUuid, inv.getUuid()).isExists()) {
            capabilities.setSupportLiveMigration(false);
        }
    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        // if vm reboot, do nothing
        // if vm stopped, detach spec releated devices according to release configurations
        // if vm destroyed, detach spec releated devices by cascade, no matter what release configurations
        String vmUuid = spec.getVmInventory().getUuid();
        if (spec.getCurrentVmOperation() != VmInstanceConstant.VmOperation.Reboot) {
            detachMdevDevicesFromVm(vmUuid, false,
                    MdevDeviceUtils.releaseSpecReleatedVirtualMdevDevicesWhenStop(vmUuid), completion);
        } else {
            completion.success();
        }
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {

    }

    private void attachMdevDeviceToVmInstance(String vmUuid, Completion completion) {
        List<MdevDeviceVO> vos = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.status, MdevDeviceStatus.Reserved)
                .eq(MdevDeviceVO_.vmInstanceUuid, vmUuid)
                .list();
        if (vos.isEmpty()) {
            completion.success();
            return;
        }

        for (MdevDeviceVO vo : vos) {
            if (!MdevDeviceState.Enabled.equals(vo.getState())) {
                throw new OperationFailureException(operr(
                        "mdev device[uuid:%s] doesn't exist or is disabled for vm[uuid:%s]", vo.getUuid(), vmUuid));
            }
            vo.setStatus(MdevDeviceStatus.Attached);
        }

        dbf.updateCollection(vos);
        completion.success();
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        attachMdevDeviceToVmInstance(spec.getVmInventory().getUuid(), completion);
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        String vmUuid = spec.getVmInventory().getUuid();
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
            // release all mdev devices if new created vm failed to start
            detachMdevDevicesFromVm(spec.getVmInventory().getUuid(), true, true, completion);
        } else {
            // release spec related mdev devices if already stopped vm failed to start
            detachMdevDevicesFromVm(vmUuid, false,
                    MdevDeviceUtils.releaseSpecReleatedVirtualMdevDevicesWhenStop(vmUuid),
                    completion);
        }
    }

    @Override
    public void postBeforeInstantiateVmResource(VmInstanceSpec spec) {

    }

    private void cleanUpMdevDeviceTagsOnVmInstance(String vmUuid) {
        // Reserved => Active
        MdevDeviceUtils.detachMdevDeviceForVmInDB(vmUuid, null, MdevDeviceStatus.Reserved, null);

        // delete mdevDeviceSpec tag after vm is started
        if (MdevDeviceSystemTags.MDEV_DEVICE_SPEC.hasTag(vmUuid)) {
            MdevDeviceSystemTags.MDEV_DEVICE_SPEC.delete(vmUuid);
        }
    }

    @Override
    public void postInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        String vmUuid = spec.getVmInventory().getUuid();
        cleanUpMdevDeviceTagsOnVmInstance(vmUuid);
        completion.success();
    }

    @Override
    public void postReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APIMigrateVmMsg.class,
                APIPrimaryStorageMigrateVmMsg.class,
                APIPrimaryStorageMigrateVolumeMsg.class,
                APILocalStorageMigrateVolumeMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIMigrateVmMsg) {
            validate((APIMigrateVmMsg) msg);
        } else if (msg instanceof APILocalStorageMigrateVolumeMsg) {
            validate((APILocalStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVolumeMsg) {
            validate((APIPrimaryStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVmMsg) {
            validate((APIPrimaryStorageMigrateVmMsg) msg);
        }

        return msg;
    }

    private void validate(APIMigrateVmMsg msg) {
        List<MdevDeviceVO> mdevDeviceVOS = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .list();
        if (mdevDeviceVOS != null && !mdevDeviceVOS.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("can not migrate vm[uuid:%s] since mdev device attached",
                    msg.getVmInstanceUuid()));
        }
    }

    private void validate(APILocalStorageMigrateVolumeMsg msg) {
        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (volume != null && volume.getType() == VolumeType.Root) {
            boolean mdevAttached = Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.vmInstanceUuid, volume.getVmInstanceUuid())
                    .isExists();

            if (mdevAttached) {
                throw new ApiMessageInterceptionException(operr(
                        "cannot migrate root volume[uuid:%s] because there are mdev devices attached",
                        msg.getVolumeUuid()
                ));
            }
        }
    }

    private void validate(APIPrimaryStorageMigrateVolumeMsg msg) {
        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (volume != null && volume.getType() == VolumeType.Root) {
            boolean mdevAttached = Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.vmInstanceUuid, volume.getVmInstanceUuid())
                    .isExists();

            if (mdevAttached) {
                throw new ApiMessageInterceptionException(operr(
                        "cannot migrate root volume[uuid:%s] because there are mdev devices attached",
                        msg.getVolumeUuid()
                ));
            }
        }
    }

    private void validate(APIPrimaryStorageMigrateVmMsg msg) {
        if (msg.getVmInstanceUuid() != null) {
            boolean mdevAttached = Q.New(MdevDeviceVO.class)
                    .eq(MdevDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .isExists();

            if (mdevAttached) {
                throw new ApiMessageInterceptionException(operr(
                        "cannot migrate vm[uuid:%s] because there are mdev devices attached",
                        msg.getVmInstanceUuid()
                ));
            }
        }
    }

    @Override
    public List<String> getAddtionalResourceType() {
        return Collections.singletonList(MdevDeviceVO.class.getName());
    }

    @Override
    public VmInstanceType getVmTypeForAddonExtension() {
        return VmInstanceType.valueOf(VmInstanceConstant.USER_VM_TYPE);
    }

    @Override
    public void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        String vmUuid = spec.getVmInventory().getUuid();
        if (spec.getVmInventory().getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)) {
            return;
        }

        List<MdevDeviceVO> vos = Q.New(MdevDeviceVO.class).eq(MdevDeviceVO_.vmInstanceUuid, vmUuid)
                .eq(MdevDeviceVO_.status, MdevDeviceStatus.Attached).list();
        if (vos == null || vos.isEmpty()) {
            return;
        }

        List<String> mdevUuids = vos.stream().map(MdevDeviceVO::getUuid).collect(Collectors.toList());
        cmd.getAddons().put(MdevDeviceConstants.SERVICE_ID, mdevUuids);
        logger.debug(String.format("put mdev devices %s to vm instance[uuid:%s]", mdevUuids, vmUuid));
    }

    @Override
    public Flow createPostHostConnectFlow(HostInventory host) {
        // try to re-splite pci devices in host because mdev devices cannot persist when the host is rebooted
        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<PciDeviceVO> pcis = Q.New(PciDeviceVO.class)
                        .eq(PciDeviceVO_.hostUuid, host.getUuid())
                        .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED)
                        .list();
                if (pcis.isEmpty()) {
                    trigger.next();
                    return;
                }

                logger.debug(String.format("try to re-splite pci devices[uuid:%s] into mdev devices",
                        pcis.stream().map(PciDeviceVO::getUuid).collect(Collectors.toList())));
                new While<>(pcis).each((pci, comp) -> {
                    PciDeviceBackend bkd = pciMgr.getPciDeviceBackendByHostUuid(host.getUuid());
                    MdevDeviceSpecVO mdevSpec = SQL.New("select spec from MdevDeviceSpecVO spec, PciDeviceMdevSpecRefVO ref " +
                            "where ref.pciDeviceUuid = :puuid and ref.mdevSpecUuid = spec.uuid and ref.effective = true")
                            .param("puuid", pci.getUuid())
                            .find();
                    logger.debug(String.format("mdev spec uuid:%s name:%s", mdevSpec.getUuid(), mdevSpec.getName()));
                    List<String> mdevUuids = Q.New(MdevDeviceVO.class)
                            .eq(MdevDeviceVO_.parentUuid, pci.getUuid())
                            .eq(MdevDeviceVO_.mdevSpecUuid, mdevSpec.getUuid())
                            .select(MdevDeviceVO_.uuid)
                            .listValues();
                    bkd.generateVfioMdevDevices(pci.getHostUuid(), pci.toInventory(), mdevSpec.toInventory(), mdevUuids, new Completion(comp) {
                        @Override
                        public void success() {
                            logger.debug(String.format("tried to re-splited pci device[uuid:%s] into mdev devices", pci.getUuid()));
                            comp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.error(String.format("failed to re-splited pci device[uuid:%s] into mdev devices", pci.getUuid()));
                            comp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        };
    }

    @Override
    @Transactional
    public boolean canDoVmHa(String vmUuid) {
        // release mdev devices before check
        VmInstanceState state = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).select(VmInstanceVO_.state).findValue();
        if (state == VmInstanceState.Stopped || state == VmInstanceState.Unknown) {
            detachMdevDeviceFromVmInDb(getNeedToDetachMdevDevices(vmUuid,
                    false, MdevDeviceUtils.releaseSpecReleatedVirtualMdevDevicesWhenStop(vmUuid)));
        }

        // do the check
        String hostUuid = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.status, MdevDeviceStatus.Attached)
                .eq(MdevDeviceVO_.vmInstanceUuid, vmUuid)
                .select(MdevDeviceVO_.hostUuid)
                .limit(1)
                .findValue();
        if (StringUtils.isEmpty(hostUuid)) {
            return true;
        }

        // the vm can only start in the host where the pci device on
        return Q.New(HostVO.class)
                .eq(HostVO_.uuid, hostUuid)
                .eq(HostVO_.state, HostState.Enabled)
                .eq(HostVO_.status, HostStatus.Connected)
                .isExists();
    }

    @Override
    public void beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        attachMdevDeviceToVmInstance(inv.getUuid(), new Completion(null) {
            @Override
            public void success() {
                logger.debug(String.format("attached mdev device to vm[uuid:%s] after migration", inv.getUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to attach mdev device to vm[uuid:%s] after migration", inv.getUuid()));
            }
        });
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        cleanUpMdevDeviceTagsOnVmInstance(inv.getUuid());
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        // release mdev devices if vm failed to migrate
        detachMdevDevicesFromVm(inv.getUuid(), true, true, new Completion(null) {
            @Override
            public void success() {
                logger.debug(String.format("auto detached mdev devices from vm[uuid:%s] because it failed to migrate", inv.getUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to auto detach mdev devices from vm[uuid:%s] because it's failed to migrate", inv.getUuid()));
            }
        });
    }

    @Override
    public Flow createVmAbnormalLifeCycleHandlingFlow(VmAbnormalLifeCycleStruct struct) {
        return new Flow() {
            String __name__ = "auto-release-attached-mdev-devices";

            VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation operation = struct.getOperation();
            VmInstanceInventory vm = struct.getVmInstance();

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (operation == VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation.VmStoppedOnTheSameHost ||
                        operation == VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation.VmStoppedFromPausedStateHostNotChanged) {
                    vmStoppedOnTheSameHost(trigger);
                } else {
                    trigger.next();
                }
            }

            private void vmStoppedOnTheSameHost(final FlowTrigger trigger) {
                String vmUuid = vm.getUuid();
                detachMdevDevicesFromVm(vmUuid, false,
                        MdevDeviceUtils.releaseSpecReleatedVirtualMdevDevicesWhenStop(vmUuid),
                        new Completion(null) {
                            @Override
                            public void success() {
                                logger.debug(String.format("auto detached mdev devices from vm[uuid:%s] because it's been stopped bypass zstack", vmUuid));
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                logger.error(String.format("failed to auto detach mdev devices from vm[uuid:%s] because it's been stopped bypass zstack", vmUuid));
                                trigger.next();
                            }
                        });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        };
    }
}
