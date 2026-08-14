package org.zstack.compute.vm;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.compute.vm.quota.*;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigBeforeUpdateExtensionPoint;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.thread.*;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.directory.ResourceDirectoryRefVO;
import org.zstack.header.AbstractService;
import org.zstack.header.allocator.AllocateHostDryRunReply;
import org.zstack.header.allocator.DesignatedAllocateHostMsg;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.*;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudConfigureFailException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.AfterChangeHostStatusExtensionPoint;
import org.zstack.header.host.CpuArchitecture;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.identity.*;
import org.zstack.header.identity.Quota.QuotaPair;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageType;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.tag.SystemTagCreateMessageValidator;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.header.volume.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.identity.Account;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.resourceconfig.*;
import org.zstack.tag.*;
import org.zstack.utils.*;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.PersistenceException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.*;

public class VmInstanceManagerImpl extends AbstractService implements
        VmInstanceManager,
        ReportQuotaExtensionPoint,
        ManagementNodeReadyExtensionPoint,
        L3NetworkDeleteExtensionPoint,
        ResourceOwnerAfterChangeExtensionPoint,
        GlobalApiMessageInterceptor,
        AfterChangeHostStatusExtensionPoint,
        VmInstanceMigrateExtensionPoint,
        VmInstanceBeforeStartExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmInstanceManagerImpl.class);
    private List<String> createVmWorkFlowElements;
    private List<String> stopVmWorkFlowElements;
    private List<String> rebootVmWorkFlowElements;
    private List<String> startVmWorkFlowElements;
    private List<String> destroyVmWorkFlowElements;
    private List<String> migrateVmWorkFlowElements;
    private List<String> attachIsoWorkFlowElements;
    private List<String> detachIsoWorkFlowElements;
    private List<String> attachVolumeWorkFlowElements;
    private List<String> expungeVmWorkFlowElements;
    private List<String> pauseVmWorkFlowElements;
    private List<String> resumeVmWorkFlowElements;
    private FlowChainBuilder createVmFlowBuilder;
    private FlowChainBuilder stopVmFlowBuilder;
    private FlowChainBuilder rebootVmFlowBuilder;
    private FlowChainBuilder startVmFlowBuilder;
    private FlowChainBuilder destroyVmFlowBuilder;
    private FlowChainBuilder migrateVmFlowBuilder;
    private FlowChainBuilder attachVolumeFlowBuilder;
    private FlowChainBuilder attachIsoFlowBuilder;
    private FlowChainBuilder detachIsoFlowBuilder;
    private FlowChainBuilder expungeVmFlowBuilder;
    private FlowChainBuilder pauseVmFlowBuilder;
    private FlowChainBuilder resumeVmFlowBuilder;
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<>();
    private Future<Void> expungeVmTask;

    static {
        allowedMessageAfterSoftDeletion.add(VmInstanceDeletionMsg.class);
    }

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private VmInstanceDeletionPolicyManager deletionPolicyMgr;
    @Autowired
    private HostAllocatorManager hostAllocatorMgr;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;
    @Autowired
    protected L3NetworkManager l3nm;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private VmFactoryManager vmFactoryManager;
    @Autowired
    protected EventFacade evtf;

    private List<VmInstanceExtensionManager> vmExtensionManagers = new ArrayList<>();
    private final static VmConfigSyncHelper vmConfigSyncHelper = new VmConfigSyncHelper();
    private final static StaticIpOperator ipOperator = new StaticIpOperator();

    @Override
    public void handleMessage(Message msg) {
        VmInstanceExtensionManager extensionManager = vmExtensionManagers.stream().filter(it -> it.getMessageClasses()
                .stream().anyMatch(clz -> clz.isAssignableFrom(msg.getClass()))).findFirst().orElse(null);
        if (extensionManager != null) {
            extensionManager.handleMessage(msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    void passThrough(VmInstanceMessage msg) {
        VmInstanceVO vo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            VmInstanceEO eo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceEO.class);
            vo = ObjectUtils.newAndCopy(eo, VmInstanceVO.class);
        }

        if (vo == null) {
            String err = String.format("Cannot find VmInstance[uuid:%s], it may have been deleted", msg.getVmInstanceUuid());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }

        VmInstanceFactory factory = getVmInstanceFactory(VmInstanceType.valueOf(vo.getType()));
        VmInstance vm = factory.getVmInstance(vo);
        vm.handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateVmInstanceMsg) {
            handle((CreateVmInstanceMsg) msg);
        } else if (msg instanceof VmInstanceMessage) {
            passThrough((VmInstanceMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateVmInstanceMsg) {
            handle((APICreateVmInstanceMsg) msg);
        } else if(msg instanceof APICreateVmNicMsg) {
            handle((APICreateVmNicMsg) msg);
        } else if (msg instanceof APIGetVmNicAttachedNetworkServiceMsg) {
            handle((APIGetVmNicAttachedNetworkServiceMsg) msg);
        } else if (msg instanceof APIDeleteVmNicMsg) {
            handle((APIDeleteVmNicMsg) msg);
        } else if (msg instanceof APIGetCandidateZonesClustersHostsForCreatingVmMsg) {
            handle((APIGetCandidateZonesClustersHostsForCreatingVmMsg) msg);
        } else if (msg instanceof APIGetCandidatePrimaryStoragesForCreatingVmMsg) {
            handle((APIGetCandidatePrimaryStoragesForCreatingVmMsg) msg);
        } else if (msg instanceof APIGetInterdependentL3NetworksImagesMsg) {
            handle((APIGetInterdependentL3NetworksImagesMsg) msg);
        } else if (msg instanceof APIGetInterdependentL3NetworksBackupStoragesMsg) {
            handle((APIGetInterdependentL3NetworksBackupStoragesMsg) msg);
        } else if (msg instanceof APIGetCandidateVmForAttachingIsoMsg) {
            handle((APIGetCandidateVmForAttachingIsoMsg) msg);
        } else if (msg instanceof APIUpdatePriorityConfigMsg) {
            handle((APIUpdatePriorityConfigMsg) msg);
        } else if (msg instanceof APIGetSpiceCertificatesMsg) {
            handle((APIGetSpiceCertificatesMsg) msg);
        } else if (msg instanceof APIGetVmsCapabilitiesMsg) {
            handle((APIGetVmsCapabilitiesMsg) msg);
        } else if (msg instanceof VmInstanceMessage) {
            passThrough((VmInstanceMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetInterdependentL3NetworksBackupStoragesMsg msg) {
        final String accountUuid = msg.getSession().getAccountUuid();
        APIGetInterdependentL3NetworksBackupStoragesReply reply =
                new APIGetInterdependentL3NetworksBackupStoragesReply();
        if (msg.getBackupStorageUuid() != null) {
            BackupStorageVO bsvo = Q.New(BackupStorageVO.class)
                    .eq(BackupStorageVO_.uuid, msg.getBackupStorageUuid())
                    .find();
            if (bsvo == null) {
                reply.setInventories(new ArrayList<>());
            } else {
                reply.setInventories(getInterdependentL3NetworksByBackupStorageUuids(Collections.singletonList(bsvo),
                        msg.getZoneUuid(), accountUuid, false));
            }
        } else {
            reply.setInventories(getInterdependentBackupStoragesByL3NetworkUuids(msg.getL3NetworkUuids()));
        }

        bus.reply(msg, reply);
    }

    private void handle(final APIGetVmsCapabilitiesMsg msg) {
        APIGetVmsCapabilitiesEvent evt = new APIGetVmsCapabilitiesEvent(msg.getId());
        ErrorCodeList err = new ErrorCodeList();
        Map<String, VmCapabilities> vmsCaps = Maps.newConcurrentMap();
        msg.getVmUuids()
                .parallelStream()
                .forEach(v -> {
                    vmsCaps.put(v, new VmCapabilitiesJudger().judge(v));
                });

        evt.setVmsCaps(vmsCaps);
        bus.publish(evt);
    }

    private void handle(final APIUpdatePriorityConfigMsg msg) {
        final APIUpdatePriorityConfigEvent evt = new APIUpdatePriorityConfigEvent(msg.getId());

        VmPriorityOperator.PriorityStruct struct = new VmPriorityOperator.PriorityStruct();
        struct.setCpuShares(msg.getCpuShares());
        struct.setOomScoreAdj(msg.getOomScoreAdj());
        VmPriorityConfigVO vmPriorityConfigVO = new VmPriorityOperator().updatePriorityConfig(msg.getUuid(), struct);
        if (vmPriorityConfigVO != null) {
            for (UpdatePriorityConfigExtensionPoint exp : pluginRgty.getExtensionList(UpdatePriorityConfigExtensionPoint.class)) {
                exp.afterUpdatePriorityConfig(vmPriorityConfigVO);
            }
        }
        bus.publish(evt);
    }

    private void handle(APIGetSpiceCertificatesMsg msg) {
        APIGetSpiceCertificatesReply reply = new APIGetSpiceCertificatesReply();
        String certificateStr = new JsonLabel().get("spiceCA", String.class);
        if (StringUtils.isNotEmpty(certificateStr)) {
            reply.setCertificateStr(certificateStr);
        } else {
            reply.setError(operr("Spice certificate does not exist, Please check if spice tls is enabled"));
        }
        bus.reply(msg, reply);
    }

    @Transactional(readOnly = true)
    private void handle(APIGetCandidateVmForAttachingIsoMsg msg) {
        APIGetCandidateVmForAttachingIsoReply reply = new APIGetCandidateVmForAttachingIsoReply();

        String sql = "select bs" +
                " from BackupStorageVO bs, ImageBackupStorageRefVO ref" +
                " where ref.imageUuid = :isoUuid" +
                " and bs.uuid = ref.backupStorageUuid";
        TypedQuery<BackupStorageVO> q = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
        q.setParameter("isoUuid", msg.getIsoUuid());
        List<BackupStorageVO> bss = q.getResultList();
        if (bss.isEmpty()) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        List<String> psUuids = new ArrayList<>();
        List<String> psTypes = new ArrayList<>();
        for (BackupStorageVO bs : bss) {
            BackupStorageType bsType = BackupStorageType.valueOf(bs.getType());
            List<String> lst = bsType.findRelatedPrimaryStorage(bs.getUuid());
            if (lst != null) {
                psUuids.addAll(lst);
            } else {
                psTypes.addAll(hostAllocatorMgr.getPrimaryStorageTypesByBackupStorageTypeFromMetrics(bs.getType()));
            }
        }

        List<VmInstanceVO> vms = new ArrayList<>();
        if (!psUuids.isEmpty()) {
            sql = "select vm" +
                    " from VmInstanceVO vm, VolumeVO vol" +
                    " where vol.type = :volType" +
                    " and vol.vmInstanceUuid = vm.uuid" +
                    " and vm.state in (:vmStates)" +
                    " and vol.primaryStorageUuid in (:psUuids)";
            TypedQuery<VmInstanceVO> vmq = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            vmq.setParameter("volType", VolumeType.Root);
            vmq.setParameter("vmStates", asList(VmInstanceState.Running, VmInstanceState.Stopped));
            vmq.setParameter("psUuids", psUuids);
            vms.addAll(vmq.getResultList());
        }

        if (!psTypes.isEmpty()) {
            sql = "select vm" +
                    " from VmInstanceVO vm, VolumeVO vol, PrimaryStorageVO ps" +
                    " where vol.type = :volType" +
                    " and vol.vmInstanceUuid = vm.uuid" +
                    " and vm.state in (:vmStates)" +
                    " and vol.primaryStorageUuid = ps.uuid" +
                    " and ps.type in (:psTypes)";
            TypedQuery<VmInstanceVO> vmq = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
            vmq.setParameter("volType", VolumeType.Root);
            vmq.setParameter("vmStates", asList(VmInstanceState.Running, VmInstanceState.Stopped));
            vmq.setParameter("psTypes", psTypes);
            vms.addAll(vmq.getResultList());
        }

        List<VmInstanceInventory> result = VmInstanceInventory.valueOf(vms);

        for (VmAttachIsoExtensionPoint ext : pluginRgty.getExtensionList(VmAttachIsoExtensionPoint.class)) {
            ext.filtCandidateVms(msg.getIsoUuid(), result);
        }
        reply.setInventories(result);
        bus.reply(msg, reply);
    }

    private void handle(APIGetInterdependentL3NetworksImagesMsg msg) {
        final String accountUuid = msg.getSession().getAccountUuid();
        if (msg.getImageUuid() != null) {
                thdf.singleFlightSubmit(new SingleFlightTask(msg)
                        .setSyncSignature(String.format("get-interdependent-l3-by-image-%s-in-zone-%s",
                                msg.getImageUuid(),
                                msg.getZoneUuid()))
                        .run((completion) -> completion.success(getInterdependentL3NetworksByImageUuid(msg, accountUuid)))
                        .done(((result) -> {
                            APIGetInterdependentL3NetworkImageReply reply = new APIGetInterdependentL3NetworkImageReply();
                            if (!result.isSuccess()) {
                                reply.setError(result.getErrorCode());
                            } else {
                                reply.setInventories((List<L3NetworkInventory>) result.getResult());
                            }

                            bus.reply(msg, reply);
                        })));
        } else if (msg.getL3NetworkUuids() != null) {
            getInterdependentImagesByL3NetworkUuids(msg);
        } else {
            thdf.singleFlightSubmit(new SingleFlightTask(msg)
                    .setSyncSignature(String.format("get-interdependent-l3-by-zone-%s",
                            msg.getZoneUuid()))
                    .run((completion) -> completion.success(getInterdependentL3NetworksByImageUuid(msg, accountUuid)))
                    .done(((result) -> {
                        APIGetInterdependentL3NetworkImageReply reply = new APIGetInterdependentL3NetworkImageReply();
                        if (!result.isSuccess()) {
                            reply.setError(result.getErrorCode());
                        } else {
                            reply.setInventories((List<L3NetworkInventory>) result.getResult());
                        }

                        bus.reply(msg, reply);
                    })));
        }
    }

    private List<BackupStorageVO> listIntersection(List<BackupStorageVO> a, List<BackupStorageVO> b) {
        List<BackupStorageVO> ret = new ArrayList<>();
        for (BackupStorageVO s : a) {
            if (b.stream().filter(it -> it.getUuid().equals(s.getUuid())).findAny().isPresent()) {
                ret.add(s);
            }
        }

        return ret;
    }

    @Transactional(readOnly = true)
    private List<BackupStorageInventory> getInterdependentBackupStoragesByL3NetworkUuids(List<String> l3s) {
        List<List<BackupStorageVO>> bss = new ArrayList<>();
        for (String l3uuid : l3s) {
            String sql = "select ps" +
                    " from PrimaryStorageVO ps, L2NetworkClusterRefVO l2ref," +
                    " L3NetworkVO l3, PrimaryStorageClusterRefVO psref" +
                    " where ps.uuid = psref.primaryStorageUuid" +
                    " and psref.clusterUuid = l2ref.clusterUuid" +
                    " and l2ref.l2NetworkUuid = l3.l2NetworkUuid" +
                    " and l3.uuid = :l3uuid";
            TypedQuery<PrimaryStorageVO> psq = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
            psq.setParameter("l3uuid", l3uuid);
            List<PrimaryStorageVO> pss = psq.getResultList();

            List<BackupStorageVO> lst = new ArrayList<>();
            for (PrimaryStorageVO ps : pss) {
                PrimaryStorageType psType = PrimaryStorageType.valueOf(ps.getType());
                List<String> bsUuids = psType.findBackupStorage(ps.getUuid());

                if (!bsUuids.isEmpty()) {
                    // the primary storage has bound backup storage, e.g. ceph
                    sql = "select bs from BackupStorageVO bs where bs.uuid in (:uuids)";
                    TypedQuery<BackupStorageVO> bq = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
                    bq.setParameter("uuids", bsUuids);
                    lst.addAll(bq.getResultList());
                } else {
                    logger.warn(String.format("the primary storage[uuid:%s, type:%s] needs a bound backup storage," +
                            " but seems it's not added", ps.getUuid(), ps.getType()));
                }
            }

            bss.add(lst);
        }

        List<BackupStorageVO> selectedBss = new ArrayList<>();
        for (List<BackupStorageVO> lst : bss) {
            selectedBss.addAll(lst);
        }

        for (List<BackupStorageVO> l : bss) {
            selectedBss = listIntersection(selectedBss, l);
        }

        return BackupStorageInventory.valueOf(selectedBss);
    }

    @Transactional(readOnly = true)
    private void getInterdependentImagesByL3NetworkUuids(APIGetInterdependentL3NetworksImagesMsg msg) {
        APIGetInterdependentL3NetworkImageReply reply = new APIGetInterdependentL3NetworkImageReply();

        List<BackupStorageInventory> bss = getInterdependentBackupStoragesByL3NetworkUuids(msg.getL3NetworkUuids());

        if (bss.isEmpty()) {
            reply.setInventories(new ArrayList<>());
            bus.reply(msg, reply);
            return;
        }

        List<String> bsUuids = bss.stream().map(BackupStorageInventory::getUuid).collect(Collectors.toList());
        String sql = "select img" +
                " from ImageVO img, ImageBackupStorageRefVO iref, BackupStorageZoneRefVO zref, BackupStorageVO bs" +
                " where img.uuid = iref.imageUuid" +
                " and iref.backupStorageUuid = zref.backupStorageUuid" +
                " and bs.uuid = zref.backupStorageUuid" +
                " and bs.uuid in (:uuids)" +
                " and zref.zoneUuid = :zoneUuid" +
                " group by img.uuid";
        TypedQuery<ImageVO> iq = dbf.getEntityManager().createQuery(sql, ImageVO.class);
        iq.setParameter("uuids", bsUuids);
        iq.setParameter("zoneUuid", msg.getZoneUuid());
        List<ImageVO> vos = iq.getResultList();
        reply.setInventories(ImageInventory.valueOf(vos));
        bus.reply(msg, reply);
    }

    @Transactional(readOnly = true)
    private List<L3NetworkInventory> getInterdependentL3NetworksByImageUuid(APIGetInterdependentL3NetworksImagesMsg msg, String accountUuid) {
        List<BackupStorageVO> bss = null;
        if (msg.getImageUuid() != null) {
            String sql = "select bs" +
                    " from BackupStorageVO bs, ImageBackupStorageRefVO ref, BackupStorageZoneRefVO zref" +
                    " where bs.uuid = ref.backupStorageUuid" +
                    " and ref.imageUuid = :imgUuid" +
                    " and ref.backupStorageUuid = zref.backupStorageUuid" +
                    " and zref.zoneUuid = :zoneUuid";
            TypedQuery<BackupStorageVO> bsq = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
            bsq.setParameter("imgUuid", msg.getImageUuid());
            bsq.setParameter("zoneUuid", msg.getZoneUuid());
            bss = bsq.getResultList();
        } else {
            String sql = "select bs" +
                    " from BackupStorageVO bs, BackupStorageZoneRefVO zref" +
                    " where bs.uuid = zref.backupStorageUuid" +
                    " and zref.zoneUuid = :zoneUuid";
            TypedQuery<BackupStorageVO> bsq = dbf.getEntityManager().createQuery(sql, BackupStorageVO.class);
            bsq.setParameter("zoneUuid", msg.getZoneUuid());
            bss = bsq.getResultList();
        }

        if (bss.isEmpty()) {
            throw new OperationFailureException(argerr("the image[uuid:%s] is not on any backup storage that has been attached to the zone[uuid:%s]",
                            msg.getImageUuid(), msg.getZoneUuid()));
        }

        List<L3NetworkInventory> l3s = getInterdependentL3NetworksByBackupStorageUuids(bss, msg.getZoneUuid(), accountUuid, msg.getRaiseException());

        List<String> bsUuids = bss.stream().map(BackupStorageVO::getUuid).collect(Collectors.toList());
        for (GetInterdependentL3NetworksExtensionPoint ext : pluginRgty.getExtensionList(GetInterdependentL3NetworksExtensionPoint.class)) {
            l3s = ext.afterFilterByImage(l3s, bsUuids, msg.getImageUuid());
        }

        return l3s;
    }

    @Transactional(readOnly = true)
    private List<L3NetworkInventory> getInterdependentL3NetworksByBackupStorageUuids(List<BackupStorageVO> bss, String zoneUuid, String accountUuid, boolean raiseException) {
        List<String> psUuids = new ArrayList<>();
        List<L3NetworkVO> l3s = new ArrayList<>();
        for (BackupStorageVO bs : bss) {
            BackupStorageType bsType = BackupStorageType.valueOf(bs.getType());
            List<String> relatedPrimaryStorageUuids = bsType.findRelatedPrimaryStorage(bs.getUuid());
            if (relatedPrimaryStorageUuids == null) {
                // the backup storage has no strongly-bound primary storage
                List<String> psTypes = hostAllocatorMgr.getPrimaryStorageTypesByBackupStorageTypeFromMetrics(bs.getType());
                psUuids.addAll(Q.New(PrimaryStorageVO.class)
                        .select(PrimaryStorageVO_.uuid)
                        .in(PrimaryStorageVO_.type, psTypes)
                        .eq(PrimaryStorageVO_.zoneUuid, zoneUuid)
                        .listValues());
                l3s.addAll(SQL.New("select l3" +
                                " from L3NetworkVO l3, L2NetworkClusterRefVO l2ref," +
                                " PrimaryStorageClusterRefVO psref, PrimaryStorageVO ps" +
                                " where l3.l2NetworkUuid = l2ref.l2NetworkUuid" +
                                " and l2ref.clusterUuid = psref.clusterUuid" +
                                " and psref.primaryStorageUuid = ps.uuid" +
                                " and ps.type in (:psTypes)" +
                                " and ps.zoneUuid = l3.zoneUuid" +
                                " and l3.zoneUuid = :zoneUuid" +
                                " group by l3.uuid")
                        .param("psTypes", psTypes)
                        .param("zoneUuid", zoneUuid)
                        .list());
            } else if (!relatedPrimaryStorageUuids.isEmpty()) {
                // the backup storage has strongly-bound primary storage, e.g. ceph
                psUuids.addAll(Q.New(PrimaryStorageVO.class)
                        .select(PrimaryStorageVO_.uuid)
                        .in(PrimaryStorageVO_.uuid, relatedPrimaryStorageUuids)
                        .eq(PrimaryStorageVO_.zoneUuid, zoneUuid)
                        .listValues());
                l3s.addAll(SQL.New("select l3" +
                                " from L3NetworkVO l3, L2NetworkClusterRefVO l2ref," +
                                " PrimaryStorageClusterRefVO psref, PrimaryStorageVO ps" +
                                " where l3.l2NetworkUuid = l2ref.l2NetworkUuid" +
                                " and l2ref.clusterUuid = psref.clusterUuid" +
                                " and psref.primaryStorageUuid = ps.uuid" +
                                " and ps.uuid in (:psUuids)" +
                                " and ps.zoneUuid = l3.zoneUuid" +
                                " and l3.zoneUuid = :zoneUuid" +
                                " group by l3.uuid")
                        .param("psUuids", relatedPrimaryStorageUuids)
                        .param("zoneUuid", zoneUuid)
                        .list());
            } else {
                // relatedPrimaryStorageUuids is not null, but size is 0
                logger.warn(String.format("the backup storage[uuid:%s, type: %s] needs a strongly-bound primary storage," +
                        " but seems the primary storage is not added", bs.getUuid(), bs.getType()));
            }
        }

        if (l3s.isEmpty()) {
            if (psUuids.isEmpty()) {
                if (raiseException) {
                    throw new OperationFailureException(argerr("no primary storage accessible to the backup storage[uuid:%s, type:%s] is found",
                            bss.get(0).getUuid(), bss.get(0).getType()));
                }
                logger.warn(String.format("no primary storage accessible to the backup storage[uuid:%s, type:%s] is found",
                        bss.get(0).getUuid(), bss.get(0).getType()));
                return new ArrayList<>();
            }

            Long clusterNum = SQL.New("select count(distinct cl)" +
                            " from ClusterVO cl, PrimaryStorageClusterRefVO psref, PrimaryStorageVO ps" +
                            " where cl.uuid = psref.clusterUuid" +
                            " and psref.primaryStorageUuid in (:psUuids)" +
                            " and ps.zoneUuid = cl.zoneUuid" +
                            " and cl.zoneUuid = :zoneUuid" +
                            " group by cl.uuid", Long.class)
                    .param("psUuids", psUuids)
                    .param("zoneUuid", zoneUuid)
                    .find();

            if (clusterNum == null || clusterNum == 0) {
                if (raiseException) {
                    throw new OperationFailureException(argerr("the primary storages[uuids:%s] has not attached any cluster on the zone[uuid:%s]",
                            psUuids, zoneUuid));
                }
                logger.warn(String.format("the primary storages[uuids:%s] has not attached any cluster on the zone[uuid:%s]", psUuids, zoneUuid));
                return new ArrayList<>();
            }

            Long l2Num = SQL.New("select count(distinct l2)" +
                            " from L2NetworkVO l2, L2NetworkClusterRefVO l2ref, PrimaryStorageClusterRefVO psref, PrimaryStorageVO ps" +
                            " where l2.uuid = l2ref.l2NetworkUuid" +
                            " and psref.primaryStorageUuid in (:psUuids)" +
                            " and l2ref.clusterUuid = psref.clusterUuid" +
                            " and ps.zoneUuid = l2.zoneUuid" +
                            " and l2.zoneUuid = :zoneUuid", Long.class)
                    .param("psUuids", psUuids)
                    .param("zoneUuid", zoneUuid)
                    .find();
            if (l2Num == null || l2Num == 0) {
                if (raiseException) {
                    throw new OperationFailureException(argerr("no l2Networks found in clusters that have attached to primary storages[uuids:%s]",
                            psUuids));
                }
                logger.warn(String.format("no l2Networks found in clusters that have attached to primary storages[uuids:%s]", psUuids));
                return new ArrayList<>();
            }
        }

        List<String> l3UuidListOfCurrentAccount;
        if (!Account.isAdminPermission(accountUuid)) {
            l3UuidListOfCurrentAccount = acntMgr.getResourceUuidsCanAccessByAccount(accountUuid, L3NetworkVO.class);
        } else {
            l3UuidListOfCurrentAccount = null;
        }

        if (l3UuidListOfCurrentAccount == null) {
            return L3NetworkInventory.valueOf(l3s);
        }
        return L3NetworkInventory.valueOf(l3s.stream()
                .filter(vo -> l3UuidListOfCurrentAccount.contains(vo.getUuid()))
                .collect(Collectors.toList()));
    }
    
    private void handle(APIGetCandidateZonesClustersHostsForCreatingVmMsg msg) {
        DesignatedAllocateHostMsg amsg = new DesignatedAllocateHostMsg();

        ImageVO image = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        amsg.setImage(ImageInventory.valueOf(image));
        amsg.setZoneUuid(msg.getZoneUuid());
        amsg.setClusterUuid(msg.getClusterUuid());

        InstanceOfferingVO insvo = null;
        if (msg.getInstanceOfferingUuid() == null) {
            amsg.setCpuCapacity(msg.getCpuNum());
            amsg.setMemoryCapacity(msg.getMemorySize());
        } else {
            insvo = dbf.findByUuid(msg.getInstanceOfferingUuid(), InstanceOfferingVO.class);
            amsg.setCpuCapacity(insvo.getCpuNum());
            amsg.setMemoryCapacity(insvo.getMemorySize());
        }

        long diskSize = 0;
        List<DiskOfferingInventory> diskOfferings = new ArrayList<>();
        if (msg.getDataDiskOfferingUuids() != null) {
            SimpleQuery<DiskOfferingVO> q = dbf.createQuery(DiskOfferingVO.class);
            q.add(DiskOfferingVO_.uuid, Op.IN, msg.getDataDiskOfferingUuids());
            List<DiskOfferingVO> dvos = q.list();
            diskOfferings.addAll(DiskOfferingInventory.valueOf(dvos));
        }

        if (image.getMediaType() == ImageMediaType.ISO) {
            if (msg.getRootDiskOfferingUuid() == null) {
                diskSize = msg.getRootDiskSize();
            } else {
                DiskOfferingVO rootDiskOffering = dbf.findByUuid(msg.getRootDiskOfferingUuid(), DiskOfferingVO.class);
                diskOfferings.add(DiskOfferingInventory.valueOf(rootDiskOffering));
            }
        } else {
            diskSize = image.getSize();
        }

        diskSize += diskOfferings.stream().mapToLong(DiskOfferingInventory::getDiskSize).sum();
        amsg.setDiskSize(diskSize);
        amsg.setL3NetworkUuids(msg.getL3NetworkUuids());
        amsg.setVmOperation(VmOperation.NewCreate.toString());
        amsg.setDryRun(true);
        amsg.setListAllHosts(true);
        amsg.setAllocatorStrategy(HostAllocatorConstant.DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE);

        if (image.getBackupStorageRefs().size() == 1) {
            amsg.setRequiredBackupStorageUuid(image.getBackupStorageRefs().iterator().next().getBackupStorageUuid());
        } else {
            if (msg.getZoneUuid() == null) {
                throw new OperationFailureException(argerr("zoneUuid must be set because the image[name:%s, uuid:%s] is on multiple backup storage",
                                image.getName(), image.getUuid()));
            }

            ImageBackupStorageSelector selector = new ImageBackupStorageSelector();
            selector.setZoneUuid(msg.getZoneUuid());
            selector.setImageUuid(image.getUuid());
            amsg.setRequiredBackupStorageUuid(selector.select());
        }

        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setUuid(Platform.FAKE_UUID);
        vm.setImageUuid(image.getUuid());
        if (insvo == null) {
            vm.setCpuNum(msg.getCpuNum());
            vm.setMemorySize(msg.getMemorySize());
        } else {
            vm.setInstanceOfferingUuid(insvo.getUuid());
            vm.setCpuNum(insvo.getCpuNum());
            vm.setMemorySize(insvo.getMemorySize());
        }
        vm.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid() == null ? msg.getL3NetworkUuids().get(0) : msg.getDefaultL3NetworkUuid());
        vm.setName("for-getting-candidates-zones-clusters-hosts");
        amsg.setVmInstance(vm);
        if (msg.getSystemTags() != null && !msg.getSystemTags().isEmpty()) {
            amsg.setSystemTags(new ArrayList<String>(msg.getSystemTags()));
        }

        APIGetCandidateZonesClustersHostsForCreatingVmReply areply = new APIGetCandidateZonesClustersHostsForCreatingVmReply();
        bus.makeLocalServiceId(amsg, HostAllocatorConstant.SERVICE_ID);
        bus.send(amsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    areply.setError(reply.getError());
                } else {
                    AllocateHostDryRunReply re = reply.castReply();

                    if (!re.getHosts().isEmpty()) {
                        areply.setHosts(re.getHosts());

                        Set<String> clusterUuids = re.getHosts().stream().
                                map(HostInventory::getClusterUuid).collect(Collectors.toSet());
                        List<ClusterInventory> clusters = ClusterInventory.valueOf(dbf.listByPrimaryKeys(clusterUuids, ClusterVO.class));
                        areply.setClusters(clusters);

                        Set<String> zoneUuids = clusters.stream().
                                map(ClusterInventory::getZoneUuid).collect(Collectors.toSet());
                        areply.setZones(ZoneInventory.valueOf(dbf.listByPrimaryKeys(zoneUuids, ZoneVO.class)));
                    } else {
                        areply.setHosts(new ArrayList<>());
                        areply.setClusters(new ArrayList<>());
                        areply.setZones(new ArrayList<>());
                    }
                }

                bus.reply(msg, areply);
            }
        });
    }

    private void handle(APIGetCandidatePrimaryStoragesForCreatingVmMsg msg) {
        APIGetCandidatePrimaryStoragesForCreatingVmReply reply = new APIGetCandidatePrimaryStoragesForCreatingVmReply();
        List<AllocatePrimaryStorageMsg> msgs = new ArrayList<>();

        Set<String> psTypes = new HashSet<>();
        List<String> clusterUuids = new ArrayList<>();
        List<DiskOfferingInventory> dataOfferings = new ArrayList<>();
        ImageInventory imageInv = new SQLBatchWithReturn<ImageInventory>(){

            @Override
            protected ImageInventory scripts() {
                List<String> dataOfferingUuids = msg.getDataDiskOfferingUuids() == null ? new ArrayList<>() :
                        msg.getDataDiskOfferingUuids();

                sql("select bs.type from BackupStorageVO bs, ImageBackupStorageRefVO ref" +
                        " where ref.imageUuid =:imageUuid" +
                        " and bs.uuid = ref.backupStorageUuid", String.class)
                        .param("imageUuid", msg.getImageUuid())
                        .list().forEach(it ->
                        psTypes.addAll(hostAllocatorMgr.getPrimaryStorageTypesByBackupStorageTypeFromMetrics((String)it)
                        ));

                clusterUuids.addAll(sql("select distinct ref.clusterUuid" +
                        " from L2NetworkClusterRefVO ref, L3NetworkVO l3" +
                        " where l3.uuid in (:l3Uuids)" +
                        " and ref.l2NetworkUuid = l3.l2NetworkUuid", String.class)
                        .param("l3Uuids", msg.getL3NetworkUuids())
                        .list());

                for (String diskUuid : dataOfferingUuids){
                    dataOfferings.add(DiskOfferingInventory.valueOf(
                            (DiskOfferingVO) q(DiskOfferingVO.class)
                                    .eq(DiskOfferingVO_.uuid, diskUuid)
                                    .find()
                    ));
                }

                ImageVO imageVO = q(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).find();
                return ImageInventory.valueOf(imageVO);
            }
        }.execute();


        // allocate ps for root volume
        AllocatePrimaryStorageMsg rmsg = new AllocatePrimaryStorageMsg();
        rmsg.setDryRun(true);
        rmsg.setImageUuid(msg.getImageUuid());
        rmsg.setRequiredClusterUuids(clusterUuids);
        if (ImageMediaType.ISO.toString().equals(imageInv.getMediaType())) {
            if (msg.getRootDiskOfferingUuid() == null) {
                rmsg.setSize(msg.getRootDiskSize());
            } else {
                Tuple t = Q.New(DiskOfferingVO.class).eq(DiskOfferingVO_.uuid, msg.getRootDiskOfferingUuid())
                        .select(DiskOfferingVO_.diskSize, DiskOfferingVO_.allocatorStrategy).findTuple();
                rmsg.setSize((long) t.get(0));
                rmsg.setAllocationStrategy((String) t.get(1));
                rmsg.setDiskOfferingUuid(msg.getRootDiskOfferingUuid());
            }
        } else {
            rmsg.setSize(imageInv.getSize());
        }
        rmsg.setPurpose(PrimaryStorageAllocationPurpose.CreateNewVm.toString());
        rmsg.setPossiblePrimaryStorageTypes(new ArrayList<>(psTypes));
        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
        msgs.add(rmsg);

        // allocate ps for data volumes
        for (DiskOfferingInventory dinv : dataOfferings) {
            AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
            amsg.setDryRun(true);
            amsg.setSize(dinv.getDiskSize());
            amsg.setRequiredClusterUuids(clusterUuids);
            amsg.setAllocationStrategy(dinv.getAllocatorStrategy());
            amsg.setDiskOfferingUuid(dinv.getUuid());
            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
            msgs.add(amsg);
        }

        if (msg.getDataDiskSizes() != null) {
            for (Long size : msg.getDataDiskSizes()) {
                AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                amsg.setDryRun(true);
                amsg.setSize(size);
                amsg.setRequiredClusterUuids(clusterUuids);
                bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                msgs.add(amsg);
            }
        }

        new While<>(msgs).all((amsg, completion) ->{
            bus.send(amsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply r) {
                    if (r.isSuccess()){
                        AllocatePrimaryStorageDryRunReply re = r.castReply();
                        if (amsg.getImageUuid() != null){
                            reply.setRootVolumePrimaryStorages(re.getPrimaryStorageInventories());
                        } else if (amsg.getDiskOfferingUuid() != null) {
                            reply.getDataVolumePrimaryStorages().put(amsg.getDiskOfferingUuid(), re.getPrimaryStorageInventories());
                        } else {
                            reply.getDataVolumePrimaryStorages().put(String.valueOf(amsg.getSize()), re.getPrimaryStorageInventories());
                        }
                    }
                    completion.done();
                }
            });

        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APICreateVmNicMsg msg) {
        final APICreateVmNicEvent evt = new APICreateVmNicEvent(msg.getId());
        VmNicInventory nic = new VmNicInventory();
        VmNicVO nicVO = new VmNicVO();
        List<UsedIpInventory> ips = new ArrayList<>();

        FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
        flowChain.setName(String.format("create-nic-on-l3-network-%s", msg.getL3NetworkUuid()));
        flowChain.then(new NoRollbackFlow() {
            String __name__ = "create-nic-and-persist-to-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                int deviceId = 1;
                String mac = NetworkUtils.generateMacWithDeviceId((short) deviceId);
                nic.setUuid(Platform.getUuid());
                nic.setMac(mac);
                nic.setDeviceId(deviceId);
                nic.setType(VmInstanceConstant.VIRTUAL_NIC_TYPE);
                for (NicManageExtensionPoint ext : pluginRgty.getExtensionList(NicManageExtensionPoint.class)) {
                    ext.beforeCreateNic(nic, msg);
                }

                nicVO.setUuid(nic.getUuid());
                nicVO.setDeviceId(deviceId);
                nicVO.setMac(nic.getMac());
                nicVO.setAccountUuid(msg.getSession().getAccountUuid());
                nicVO.setType(nic.getType());

                int tries = 5;
                while (tries-- > 0) {
                    try {
                        new SQLBatch() {
                            @Override
                            protected void scripts() {
                                persist(nicVO);
                            }
                        }.execute();
                        break;
                    } catch (PersistenceException e) {
                        if (ExceptionDSL.isCausedBy(e, SQLIntegrityConstraintViolationException.class, "Duplicate entry")) {
                            logger.debug(String.format("Concurrent mac allocation. Mac[%s] has been allocated, try allocating another one. " +
                                    "The error[Duplicate entry] printed by jdbc.spi.SqlExceptionHelper is no harm, " +
                                    "we will try finding another mac", nicVO.getMac()));
                            logger.trace("", e);
                            nicVO.setMac(NetworkUtils.generateMacWithDeviceId((short) nicVO.getDeviceId()));
                        } else {
                            throw e;
                        }
                    }
                }

                trigger.next();
            }
        }).then(new Flow() {
            String __name__ = "allocate-nic-ip-and-mac";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<ErrorCode> errors = new ArrayList<>();
                L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
                new While<>(l3NetworkVO.getIpVersions()).each((version, wcomp) -> {
                    AllocateIpMsg allocateIpMsg = new AllocateIpMsg();
                    allocateIpMsg.setL3NetworkUuid(msg.getL3NetworkUuid());
                    allocateIpMsg.setRequiredIp(msg.getIp());
                    if (msg.getIp() == null) {
                        allocateIpMsg.setRequiredIp(nic.getIp());
                    }
                    allocateIpMsg.setIpVersion(version);
                    l3nm.updateIpAllocationMsg(allocateIpMsg, nic.getMac());
                    bus.makeTargetServiceIdByResourceUuid(allocateIpMsg, L3NetworkConstant.SERVICE_ID, msg.getL3NetworkUuid());

                    bus.send(allocateIpMsg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errors.add(reply.getError());
                                wcomp.allDone();
                                return;
                            }

                            AllocateIpReply aReply = reply.castReply();
                            UsedIpInventory ipInventory = aReply.getIpInventory();
                            ips.add(ipInventory);
                            for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
                                ext.afterAddIpAddress(nic.getUuid(), ipInventory.getUuid());
                            }

                            if (nic.getL3NetworkUuid() == null) {
                                nic.setL3NetworkUuid(aReply.getIpInventory().getL3NetworkUuid());
                            }
                            if (nic.getUsedIpUuid() == null) {
                                nic.setUsedIpUuid(aReply.getIpInventory().getUuid());
                            }
                            /* TODO, to support nic driver type*/
                            wcomp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errors.size() > 0) {
                            trigger.fail(errors.get(0));
                        } else {
                            trigger.next();
                        }
                    }
                });

            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (!ips.isEmpty()) {
                    List<ReturnIpMsg> rmsgs = new ArrayList<>();
                    for (UsedIpInventory ip : ips) {
                        ReturnIpMsg rmsg = new ReturnIpMsg();
                        rmsg.setL3NetworkUuid(ip.getL3NetworkUuid());
                        rmsg.setUsedIpUuid(ip.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
                        rmsgs.add(rmsg);
                    }

                    new While<>(rmsgs).step((rmsg, wcomp) -> {
                        bus.send(rmsg, new CloudBusCallBack(wcomp) {
                            @Override
                            public void run(MessageReply reply) {
                                wcomp.done();

                            }
                        });
                    }, 2).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            dbf.removeByPrimaryKey(nic.getUuid(), VmNicVO.class);
                            trigger.rollback();
                        }
                    });
                } else {
                    dbf.removeByPrimaryKey(nic.getUuid(), VmNicVO.class);
                    trigger.rollback();
                }
            }
        });

        flowChain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                tagMgr.createTagsFromAPICreateMessage(msg, nic.getUuid(), VmNicVO.class.getSimpleName());
                evt.setInventory(VmNicInventory.valueOf(dbf.reload(nicVO)));
                bus.publish(evt);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                evt.setError(errCode);
                bus.publish(evt);
            }
        }).start();
    }

    private void handle(APIGetVmNicAttachedNetworkServiceMsg msg) {
        APIGetVmNicAttachedNetworkServiceReply reply = new APIGetVmNicAttachedNetworkServiceReply();
        List<String> networkServices = new ArrayList<>();
        VmNicVO nicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).find();
        for (VmNicChangeNetworkExtensionPoint extension : pluginRgty.getExtensionList(VmNicChangeNetworkExtensionPoint.class)) {
            Map<String, String> ret = extension.getVmNicAttachedNetworkService(VmNicInventory.valueOf(nicVO));
            if (ret == null) {
                continue;
            }
            networkServices.addAll(ret.keySet());
        }
        reply.setNetworkServices(networkServices);
        bus.reply(msg, reply);
    }

    private void instantiateTagsForCreateMessage(final CreateVmInstanceMsg msg, final APICreateMessage cmsg, VmInstanceVO finalVo) {
        if (cmsg != null) {
            tagMgr.createTagsFromAPICreateMessage(cmsg, finalVo.getUuid(), VmInstanceVO.class.getSimpleName());
        } else {
            tagMgr.createTags(msg.getSystemTags(), msg.getUserTags(), finalVo.getUuid(), VmInstanceVO.class.getSimpleName());
        }

        Boolean isVirtio = msg.getVirtio();
        if (isVirtio == null && msg.getImageUuid() != null) {
            isVirtio = Q.New(ImageVO.class)
                    .eq(ImageVO_.uuid, msg.getImageUuid())
                    .select(ImageVO_.virtio)
                    .findValue();
        }
        if (isVirtio == null) {
            isVirtio = false;
        }

        if (isVirtio) {
            SystemTagCreator creator = VmSystemTags.VIRTIO.newSystemTagCreator(finalVo.getUuid());
            creator.recreate = true;
            creator.inherent = false;
            creator.tag = VmSystemTags.VIRTIO.getTagFormat();
            creator.create();
        }

        if (finalVo.getInstanceOfferingUuid() != null) {
            tagMgr.copySystemTag(
                    finalVo.getInstanceOfferingUuid(),
                    InstanceOfferingVO.class.getSimpleName(),
                    finalVo.getUuid(),
                    VmInstanceVO.class.getSimpleName(), false);
        }

        if (!msg.hasSystemTag(VmSystemTags.BOOT_MODE::isMatch)) {
        if (msg.getImageUuid() != null) {
            tagMgr.copySystemTag(
                    msg.getImageUuid(),
                    ImageVO.class.getSimpleName(),
                    finalVo.getUuid(),
                    VmInstanceVO.class.getSimpleName(), false);
        }}

        if (ImageArchitecture.aarch64.toString().equals(finalVo.getArchitecture())) {
            SystemTagCreator creator = VmSystemTags.MACHINE_TYPE.newSystemTagCreator(finalVo.getUuid());
            creator.setTagByTokens(map(e(VmSystemTags.MACHINE_TYPE_TOKEN, VmMachineType.virt.toString())));
            creator.recreate = true;
            creator.create();
        }

        vmConfigSyncHelper.setVmSyncPorts(finalVo.getUuid());
    }

    private List<ErrorCode> extEmitterHandleSystemTag(final CreateVmInstanceMsg msg, final APICreateMessage cmsg, VmInstanceVO finalVo) {
        List<ErrorCode> result = Collections.emptyList();
        if (msg == null) {
            result.add(operr("CreateVmInstanceMsg cannot be null"));
            return result;
        } else if (cmsg != null && cmsg.getSystemTags() != null && !cmsg.getSystemTags().isEmpty()) {
            return extEmitter.handleSystemTag(finalVo.getUuid(), cmsg.getSystemTags());
        } else if (cmsg == null && msg.getSystemTags() != null && !msg.getSystemTags().isEmpty()) {
            return extEmitter.handleSystemTag(finalVo.getUuid(), msg.getSystemTags());
        }
        return result;
    }

    private List<ErrorCode> extEmitterHandleSshKeyPair(final CreateVmInstanceMsg msg, final APICreateMessage cmsg, VmInstanceVO finalVo) {
        List<ErrorCode> result = Collections.emptyList();
        if (msg == null) {
            result.add(operr("CreateVmInstanceMsg cannot be null"));
            return result;
        } else if (msg.getSshKeyPairUuids() != null && !msg.getSshKeyPairUuids().isEmpty()) {
            return extEmitter.associateSshKeyPair(finalVo.getUuid(), msg.getSshKeyPairUuids());
        }
        return result;
    }

    protected void doCreateVmInstance(final CreateVmInstanceMsg msg, final APICreateMessage cmsg, ReturnValueCompletion<VmInstanceInventory> completion) {
        boolean uniqueVmName = VmGlobalConfig.UNIQUE_VM_NAME.value(Boolean.class);
        if (uniqueVmName) {
            boolean exists = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.name, msg.getName()).isExists();
            if (exists) {
                throw new ApiMessageInterceptionException(operr("could not create vm, a vm with the name [%s] already exists",
                        msg.getName()));
            }
        }

        pluginRgty.getExtensionList(VmInstanceCreateExtensionPoint.class).forEach(extensionPoint -> {
            extensionPoint.preCreateVmInstance(msg);
        });

        final String instanceOfferingUuid = msg.getInstanceOfferingUuid();
        final ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).find();
        VmInstanceVO vo = new VmInstanceVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setClusterUuid(msg.getClusterUuid());
        vo.setDescription(msg.getDescription());
        vo.setImageUuid(msg.getImageUuid());
        vo.setInstanceOfferingUuid(instanceOfferingUuid);
        vo.setState(VmInstanceState.Created);
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setInternalId(dbf.generateSequenceNumber(VmInstanceSequenceNumberVO.class));
        vo.setDefaultL3NetworkUuid(msg.getDefaultL3NetworkUuid());
        vo.setCpuNum(msg.getCpuNum());
        vo.setCpuSpeed(msg.getCpuSpeed());
        vo.setMemorySize(msg.getMemorySize());
        vo.setReservedMemorySize(msg.getReservedMemorySize());
        vo.setAllocatorStrategy(msg.getAllocatorStrategy());
        vo.setPlatform(msg.getPlatform() != null ? msg.getPlatform() : image.getPlatform().toString());
        vo.setGuestOsType(msg.getGuestOsType() != null ? msg.getGuestOsType() : image.getGuestOsType());
        vo.setArchitecture(msg.getArchitecture() != null ? msg.getArchitecture() : image.getArchitecture());
        String vmType = msg.getType() == null ? VmInstanceConstant.USER_VM_TYPE : msg.getType();
        VmInstanceType type = VmInstanceType.valueOf(vmType);
        VmInstanceFactory factory = getVmInstanceFactory(type);

        VmInstanceVO finalVo = vo;
        vo = new SQLBatchWithReturn<VmInstanceVO>() {
            @Override
            protected VmInstanceVO scripts() {
                finalVo.setAccountUuid(msg.getAccountUuid());
                factory.createVmInstance(finalVo, msg);

                return reload(finalVo);
            }
        }.execute();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("do-create-vmInstance-%s", vo.getUuid()));
        chain.then(new ShareFlow() {
            VmInstanceInventory instantiateVm;
            List<DiskAO> otherDisks = new ArrayList<>();
            boolean attachOtherDisks = false;

            @Override
            public void setup() {
                if (!CollectionUtils.isEmpty(msg.getDataDisks())) {
                    setDiskAOsName(otherDisks = msg.getDataDisks());
                    attachOtherDisks = !otherDisks.isEmpty();
                }

                // VM in other platform do not support to attach data volume when VM is running
                boolean temporaryShutdown = ImagePlatform.Other.name().equals(finalVo.getPlatform()) &&
                        Objects.equals(msg.getStrategy(), VmCreationStrategy.InstantStart.toString()) &&
                        attachOtherDisks;

                flow(new Flow() {
                    String __name__ = "call-after-persist-vm-extensions";
                    List<VmInstanceCreateExtensionPoint> done = new ArrayList<>();

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (VmInstanceCreateExtensionPoint extension : pluginRgty.getExtensionList(VmInstanceCreateExtensionPoint.class)) {
                            done.add(extension);
                            extension.afterPersistVmInstanceVO(finalVo, msg);
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        Collections.reverse(done);
                        CollectionUtils.safeForEach(done,
                                extension -> extension.afterRollbackPersistVmInstanceVO(finalVo, msg));
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    List<ErrorCode> errorCodes = Collections.emptyList();
                    String __name__ = String.format("instantiate-systemTag-for-vm-%s", finalVo.getUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        instantiateTagsForCreateMessage(msg, cmsg, finalVo);
                        errorCodes = extEmitterHandleSystemTag(msg, cmsg, finalVo);
                        if (!errorCodes.isEmpty()) {
                            trigger.fail(multiErr(errorCodes, "handle system tag fail when creating vm"));
                            return;
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, finalVo.getUuid()).isExists()) {
                            dbf.removeByPrimaryKey(finalVo.getUuid(), VmInstanceVO.class);
                        }
                        trigger.rollback();
                    }
                });

                flow(new Flow() {
                    List<ErrorCode> errorCodes = Collections.emptyList();
                    String __name__ = String.format("instantiate-ssh-key-pair-for-vm-%s", finalVo.getUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        errorCodes = extEmitterHandleSshKeyPair(msg, cmsg, finalVo);
                        if (!errorCodes.isEmpty()) {
                            trigger.fail(multiErr(errorCodes, "handle sshkeypair fail when creating vm"));
                            return;
                        }
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, finalVo.getUuid()).isExists()) {
                            dbf.removeByPrimaryKey(finalVo.getUuid(), VmInstanceVO.class);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "instantiate-just-create-vmInstance";

                    @Override
                    public boolean skip(Map data) {
                        return VmCreationStrategy.JustCreate != VmCreationStrategy.valueOf(msg.getStrategy());
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VmInstanceInventory inv = VmInstanceInventory.valueOf(finalVo);
                        createVmButNotStart(msg, inv);
                        instantiateVm = inv;
                        trigger.next();
                    }
                });

                flow(new Flow() {
                    String __name__ = "instantiate-new-created-vmInstance";

                    @Override
                    public boolean skip(Map data) {
                        return VmCreationStrategy.JustCreate == VmCreationStrategy.valueOf(msg.getStrategy());
                    }
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        InstantiateNewCreatedVmInstanceMsg smsg = new InstantiateNewCreatedVmInstanceMsg();
                        smsg.setDisableL3Networks(msg.getDisableL3Networks());
                        smsg.setHostUuid(msg.getHostUuid());
                        smsg.setDataVolumeTemplateUuids(msg.getDataVolumeTemplateUuids());
                        smsg.setDataVolumeFromTemplateSystemTags(msg.getDataVolumeFromTemplateSystemTags());
                        smsg.setL3NetworkUuids(msg.getL3NetworkSpecs());

                        final DiskAO rootDisk = msg.getRootDisk();
                        DiskOfferingVO dvo = null;
                        if (rootDisk != null) {
                            if (rootDisk.getDiskOfferingUuid() != null) {
                                smsg.setRootDiskOfferingUuid(rootDisk.getDiskOfferingUuid());
                            } else if (rootDisk.getSize() > 0) {
                                dvo = new DiskOfferingVO();
                                dvo.setUuid(getUuid());
                                dvo.setAccountUuid(msg.getAccountUuid());
                                dvo.setDiskSize(rootDisk.getSize());
                                dvo.setName("for-create-vm-" + finalVo.getUuid());
                                dvo.setType("TemporaryDiskOfferingType");
                                dvo.setState(DiskOfferingState.Enabled);
                                dvo.setAllocatorStrategy(PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE);
                                dbf.persist(dvo);
                                smsg.setRootDiskOfferingUuid(dvo.getUuid());
                            }
                        }

                        String newCreateDiskOfferingUuid = dvo == null ? null : dvo.getUuid();

                        smsg.setVmInstanceInventory(VmInstanceInventory.valueOf(finalVo));
                        smsg.setCandidatePrimaryStorageUuidsForDataVolume(msg.getCandidatePrimaryStorageUuidsForDataVolume());
                        smsg.setCandidatePrimaryStorageUuidsForRootVolume(msg.getCandidatePrimaryStorageUuidsForRootVolume());

                        if (temporaryShutdown) {
                            logger.debug(String.format(
                                    "VM[uuid=%s] will be temporarily shut down before attaching data volume(s): strategy update to 'CreateStopped'",
                                    finalVo.getUuid()));
                            smsg.setStrategy(VmCreationStrategy.CreateStopped.toString());
                        } else {
                            smsg.setStrategy(msg.getStrategy());
                        }

                        smsg.setArchitecture(msg.getArchitecture() == null ? null :
                                CpuArchitecture.valueOf(msg.getArchitecture()));

                        smsg.setTimeout(msg.getTimeout());
                        smsg.setRootVolumeSystemTags(msg.getRootVolumeSystemTags());
                        smsg.setDataVolumeSystemTags(msg.getDataVolumeSystemTags());
                        smsg.setRootDisk(msg.getRootDisk());
                        smsg.setDataDisks(msg.getDataDisks());
                        smsg.setDeprecatedDataVolumeSpecs(msg.getDeprecatedDataVolumeSpecs());
                        smsg.setVmCustomSpecification(msg.getVmCustomSpecification());
                        smsg.setDevicesSpec(msg.getDevicesSpec());
                        bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, finalVo.getUuid());
                        bus.send(smsg, new CloudBusCallBack(smsg) {
                            @Override
                            public void run(MessageReply reply) {
                                if (newCreateDiskOfferingUuid != null) {
                                    dbf.removeByPrimaryKey(newCreateDiskOfferingUuid, DiskOfferingVO.class);
                                }

                                if (reply.isSuccess()) {
                                    InstantiateNewCreatedVmInstanceReply r = (InstantiateNewCreatedVmInstanceReply) reply;
                                    instantiateVm = r.getVmInventory();
                                    data.put(VmInstanceInventory.class.getSimpleName(), instantiateVm);
                                    trigger.next();
                                    return;
                                }
                                trigger.fail(reply.getError());
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback chain, Map data) {
                        if (instantiateVm == null) {
                            chain.rollback();
                            return;
                        }
                        DestroyVmInstanceMsg dmsg = new DestroyVmInstanceMsg();
                        dmsg.setVmInstanceUuid(finalVo.getUuid());
                        dmsg.setDeletionPolicy(VmInstanceDeletionPolicy.Direct);
                        bus.makeTargetServiceIdByResourceUuid(dmsg, VmInstanceConstant.SERVICE_ID, finalVo.getUuid());
                        bus.send(dmsg, new CloudBusCallBack(null) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("failed to delete vm [%s]", instantiateVm.getUuid()));
                                }
                                chain.rollback();
                            }
                        });
                    }
                });


                if (!Objects.equals(msg.getStrategy(), VmCreationStrategy.JustCreate.toString()) && attachOtherDisks) {
                    otherDisks.forEach(diskAO -> flow(new VmInstantiateOtherDiskFlow(diskAO)));
                }

                if (temporaryShutdown) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "start-vm";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            StartVmInstanceMsg smsg = new StartVmInstanceMsg();
                            smsg.setVmInstanceUuid(instantiateVm.getUuid());
                            smsg.setHostUuid(instantiateVm.getLastHostUuid());
                            bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, finalVo.getUuid());
                            bus.send(smsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                        return;
                                    }
                                    trigger.next();
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        String vmUuid = instantiateVm.getUuid();
                        completion.success(VmInstanceInventory.valueOf(dbf.findByUuid(vmUuid, VmInstanceVO.class)));
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }

            private void setDiskAOsName(List<DiskAO> diskAOs) {
                AtomicInteger count = new AtomicInteger(1);
                diskAOs.stream().filter(diskAO -> diskAO.getSourceUuid() == null).filter(diskAO -> diskAO.getName() == null)
                        .forEach(diskAO -> {
                            diskAO.setName(String.format("DATA-for-%s-%d", finalVo.getName(), count.get()));
                            count.getAndIncrement();
                        });
            }
        }).start();
    }

    private void createVmButNotStart(CreateVmInstanceMsg msg, VmInstanceInventory inv) {
        InstantiateVmFromNewCreatedStruct struct = InstantiateVmFromNewCreatedStruct.fromMessage(msg);
        new JsonLabel().create(InstantiateVmFromNewCreatedStruct.makeLabelKey(inv.getUuid()), struct, inv.getUuid());
    }

    private void handle(final CreateVmInstanceMsg msg) {
        if(msg.getZoneUuid() == null && !CollectionUtils.isEmpty(msg.getL3NetworkSpecs())){
            String l3Uuid = VmNicSpec.getL3UuidsOfSpec(msg.getL3NetworkSpecs()).get(0);
            String zoneUuid = Q.New(L3NetworkVO.class)
                    .select(L3NetworkVO_.zoneUuid)
                    .eq(L3NetworkVO_.uuid, l3Uuid)
                    .findValue();
            msg.setZoneUuid(zoneUuid);
        }

        doCreateVmInstance(msg, null, new ReturnValueCompletion<VmInstanceInventory>(msg) {
            @Override
            public void success(VmInstanceInventory inv) {
                CreateVmInstanceReply reply = new CreateVmInstanceReply();
                reply.setInventory(inv);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateVmInstanceReply r = new CreateVmInstanceReply();
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    private void handle(final APICreateVmInstanceMsg msg) {
        doCreateVmInstance(VmInstanceUtils.fromAPICreateVmInstanceMsg(msg), msg, new ReturnValueCompletion<VmInstanceInventory>(msg) {
            APICreateVmInstanceEvent evt = new APICreateVmInstanceEvent(msg.getId());

            @Override
            public void success(VmInstanceInventory inv) {
                evt.setInventory(inv);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void doDeleteVmNic(VmNicInventory nic, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getVmNicSyncSignature(nic.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (nic.getVmInstanceUuid() == null) {
                    FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
                    fchain.setName(String.format("detach-network-service-from-vmnic-%s", nic.getUuid()));
                    for (ReleaseNetworkServiceOnDeletingNicExtensionPoint ext : pluginRgty.getExtensionList(ReleaseNetworkServiceOnDeletingNicExtensionPoint.class)) {
                        fchain.then(new NoRollbackFlow() {
                            @Override
                            public void run(FlowTrigger trigger, Map data) {
                                ext.releaseNetworkServiceOnDeletingNic(nic, new NoErrorCompletion(trigger) {
                                    @Override
                                    public void done() {
                                        trigger.next();
                                    }
                                });
                            }
                        });
                    }
                    fchain.then(new NoRollbackFlow() {
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            List<ReturnIpMsg> msgs = new ArrayList<>();
                            for (UsedIpInventory ip : nic.getUsedIps()) {
                                ReturnIpMsg returnIpMsg = new ReturnIpMsg();
                                returnIpMsg.setUsedIpUuid(ip.getUuid());
                                returnIpMsg.setL3NetworkUuid(ip.getL3NetworkUuid());
                                bus.makeTargetServiceIdByResourceUuid(returnIpMsg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
                                msgs.add(returnIpMsg);
                            }
                            new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        logger.warn(String.format("failed to return ip address[uuid: %s]", msg.getUsedIpUuid()));
                                    }
                                    com.done();
                                }
                            })).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    for (NicManageExtensionPoint ext : pluginRgty.getExtensionList(NicManageExtensionPoint.class)) {
                                        ext.beforeDeleteNic(nic);
                                    }
                                    dbf.removeByPrimaryKey(nic.getUuid(), VmNicVO.class);
                                    trigger.next();
                                }
                            });
                        }
                    });
                    fchain.done(new FlowDoneHandler(completion) {
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

                    return;
                }
                DetachNicFromVmMsg detachNicFromVmMsg = new DetachNicFromVmMsg();
                detachNicFromVmMsg.setVmInstanceUuid(nic.getVmInstanceUuid());
                detachNicFromVmMsg.setVmNicUuid(nic.getUuid());
                bus.makeTargetServiceIdByResourceUuid(detachNicFromVmMsg, VmInstanceConstant.SERVICE_ID, nic.getVmInstanceUuid());
                bus.send(detachNicFromVmMsg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                        } else {
                            completion.success();
                        }
                    }
                });
            }
            @Override
            public String getName() {
                return String.format("delete-vmNic-%s", nic.getUuid());
            }
        });
    }

    private void handle(final APIDeleteVmNicMsg msg) {
        APIDeleteVmNicEvent evt = new APIDeleteVmNicEvent(msg.getId());

        VmNicVO nicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getUuid()).find();
        doDeleteVmNic(VmNicInventory.valueOf(nicVO), new Completion(msg) {
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

    private String getVmNicSyncSignature(String nicUuid) {
        return String.format("vmNic-%s", nicUuid);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VmInstanceConstant.SERVICE_ID);
    }

    private void createVmFlowChainBuilder() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        createVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(createVmWorkFlowElements).construct();
        stopVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(stopVmWorkFlowElements).construct();
        rebootVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(rebootVmWorkFlowElements).construct();
        startVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(startVmWorkFlowElements).construct();
        destroyVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(destroyVmWorkFlowElements).construct();
        migrateVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(migrateVmWorkFlowElements).construct();
        attachVolumeFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(attachVolumeWorkFlowElements).construct();
        attachIsoFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(attachIsoWorkFlowElements).construct();
        detachIsoFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(detachIsoWorkFlowElements).construct();
        expungeVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(expungeVmWorkFlowElements).construct();
        pauseVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(pauseVmWorkFlowElements).construct();
        resumeVmFlowBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(resumeVmWorkFlowElements).construct();
    }

    @Override
    public boolean start() {
        try {
            createVmFlowChainBuilder();
            installSystemTagValidator();
            installGlobalConfigUpdater();
            vmExtensionManagers.addAll(pluginRgty.getExtensionList(VmInstanceExtensionManager.class));

            bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
                @Override
                public void beforeDeliveryMessage(Message msg) {
                    if (msg instanceof NeedQuotaCheckMessage) {
                        if (((NeedQuotaCheckMessage) msg).getAccountUuid() == null ||
                                ((NeedQuotaCheckMessage) msg).getAccountUuid().equals("")) {
                            // skip admin scheduler
                            return;
                        }
                        List<Quota> quotas = acntMgr.getMessageQuotaMap().get(msg.getClass());
                        if (quotas == null || quotas.size() == 0) {
                            return;
                        }
                        Map<String, QuotaPair> pairs = new QuotaUtil().
                                makeQuotaPairs(((NeedQuotaCheckMessage) msg).getAccountUuid());
                        for (Quota quota : quotas) {
                            quota.getOperator().checkQuota((NeedQuotaCheckMessage) msg, pairs);
                        }
                    }
                }
            }, StartVmInstanceMsg.class);

            deleteMigrateSystemTagWhenVmStateChangedToRunning();
            pluginRgty.saveExtensionAsMap(VmAttachOtherDiskExtensionPoint.class, new Function<Object, VmAttachOtherDiskExtensionPoint>() {
                @Override
                public Object call(VmAttachOtherDiskExtensionPoint arg) {
                    return arg.getDiskType();
                }
            });

            return true;
        } catch (Exception e) {
            throw new CloudConfigureFailException(VmInstanceManagerImpl.class, e.getMessage(), e);
        }
    }

    private void installGlobalConfigUpdater() {
        VmGlobalConfig.VM_EXPUNGE_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startVmExpungeTask();
            }
        });
        VmGlobalConfig.VM_EXPUNGE_PERIOD.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startVmExpungeTask();
            }
        });
        VmGlobalConfig.VM_DELETION_POLICY.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startVmExpungeTask();
            }
        });
        VmGlobalConfig.MULTI_VNIC_SUPPORT.installBeforeUpdateOnEveryNodeExtension(new GlobalConfigBeforeUpdateExtensionPoint() {
            @Override
            public void beforeUpdateExtensionPoint(GlobalConfig oldConfig, String newValue) {
                if (!oldConfig.value(Boolean.class) || "true".equalsIgnoreCase(newValue)) {
                    return;
                }

                List<Tuple> tuples;
                String sql = "select vmInstanceUuid, l3NetworkUuid, count(*) from VmNicVO group by vmInstanceUuid, l3NetworkUuid";
                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                tuples = q.getResultList();
                if (tuples == null || tuples.isEmpty()) {
                    return;
                }
                for (Tuple tuple: tuples) {
                    if (tuple.get(2, Long.class) > 1) {
                        throw new ApiMessageInterceptionException(operr("unable to enable this function. There are multi nics of L3 network[uuid:%s] in the vm[uuid: %s]",
                                    tuple.get(0, String.class), tuple.get(1, String.class)));
                    }
                }
            }
        });

        ResourceConfig resourceConfig = rcf.getResourceConfig(VmGlobalConfig.VM_HA_ACROSS_CLUSTERS.getIdentity());
        resourceConfig.installUpdateExtension(new ResourceConfigUpdateExtensionPoint() {
            @Override
            public void updateResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String oldValue, String newValue) {
                if (!VmInstanceVO.class.getSimpleName().equals(resourceType))
                    return;
                // keep back-compatibility create or delete resource binding tag if needed
                if (newValue.equals("false")) {
                    String clusterUuid = Q.New(VmInstanceVO.class).select(VmInstanceVO_.clusterUuid)
                            .eq(VmInstanceVO_.uuid, resourceUuid).findValue();
                    String token = String.format("Cluster:%s", clusterUuid);
                    SystemTagCreator creator = VmSystemTags.VM_RESOURCE_BINGDING.newSystemTagCreator(resourceUuid);
                    creator.recreate = true;
                    creator.setTagByTokens(map(e(VmSystemTags.VM_RESOURCE_BINGDING_TOKEN, token)));
                    creator.create();
                } else {
                    VmSystemTags.VM_RESOURCE_BINGDING.delete(resourceUuid);
                }
            }
        });
    }

    private void installHostnameValidator() {
        class HostNameValidator implements SystemTagCreateMessageValidator, SystemTagValidator {
            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage cmsg) {
                final NewVmInstanceMessage msg = (NewVmInstanceMessage) cmsg;

                int hostnameCount = 0;
                for (String sysTag : msg.getSystemTags()) {
                    if (VmSystemTags.HOSTNAME.isMatch(sysTag)) {
                        if (++hostnameCount > 1) {
                            throw new ApiMessageInterceptionException(argerr("only one hostname system tag is allowed, but %s got", hostnameCount));
                        }

                        String hostname = VmSystemTags.HOSTNAME.getTokenByTag(sysTag, VmSystemTags.HOSTNAME_TOKEN);
                        VmHostnameUtils.validateHostname(hostname, ImagePlatform.Windows.toString().equals(msg.getPlatform()));
                    }
                }
            }

            @Transactional(readOnly = true)
            private List<SystemTagVO> querySystemTagsByL3(String tag, String l3Uuid) {
                String sql = "select t" +
                        " from SystemTagVO t, VmInstanceVO vm, VmNicVO nic" +
                        " where t.resourceUuid = vm.uuid" +
                        " and vm.uuid = nic.vmInstanceUuid" +
                        " and nic.l3NetworkUuid = :l3Uuid" +
                        " and t.tag = :sysTag";
                TypedQuery<SystemTagVO> q = dbf.getEntityManager().createQuery(sql, SystemTagVO.class);
                q.setParameter("l3Uuid", l3Uuid);
                q.setParameter("sysTag", tag);
                return q.getResultList();
            }

            private void validateHostNameOnDefaultL3Network(String tag, String hostname, String l3Uuid) {
                List<SystemTagVO> vos = querySystemTagsByL3(tag, l3Uuid);

                if (!vos.isEmpty()) {
                    SystemTagVO sameTag = vos.get(0);
                    throw new ApiMessageInterceptionException(argerr("conflict hostname in system tag[%s];" +
                                    " there has been a VM[uuid:%s] having hostname[%s] on L3 network[uuid:%s]",
                            tag, sameTag.getResourceUuid(), hostname, l3Uuid));
                }
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                if (VmSystemTags.HOSTNAME.isMatch(systemTag)) {
                    String hostname = VmSystemTags.HOSTNAME.getTokenByTag(systemTag, VmSystemTags.HOSTNAME_TOKEN);
                    boolean isWindows = Q.New(VmInstanceVO.class)
                            .eq(VmInstanceVO_.uuid, resourceUuid)
                            .eq(VmInstanceVO_.platform, ImagePlatform.Windows.toString())
                            .isExists();
                    VmHostnameUtils.validateHostname(hostname, isWindows);
                } else if (VmSystemTags.BOOT_ORDER.isMatch(systemTag)) {
                    validateBootOrder(systemTag);
                }
            }

            private void validateBootOrder(String systemTag) {
                String order = VmSystemTags.BOOT_ORDER.getTokenByTag(systemTag, VmSystemTags.BOOT_ORDER_TOKEN);
                for (String o : order.split(",")) {
                    try {
                        VmBootDevice.valueOf(o);
                    } catch (IllegalArgumentException e) {
                        throw new OperationFailureException(argerr("invalid boot device[%s] in boot order[%s]", o, order));
                    }
                }
            }
        }

        HostNameValidator hostnameValidator = new HostNameValidator();
        tagMgr.installCreateMessageValidator(VmInstanceVO.class.getSimpleName(), hostnameValidator);
        VmSystemTags.HOSTNAME.installValidator(hostnameValidator);

        // TODO: system tags should support token format validation
        VmHardwareSystemTags.CPU_SOCKETS.installValidator((resourceUuid, resourceType, systemTag) -> {
            String sockets = VmHardwareSystemTags.CPU_SOCKETS.getTokenByTag(systemTag, VmHardwareSystemTags.CPU_SOCKETS_TOKEN);
            try {
                Integer.valueOf(sockets);
            } catch (NumberFormatException e) {
                throw new ApiMessageInterceptionException(argerr("cpuSockets must be an integer"));
            }
        });

        VmHardwareSystemTags.CPU_CORES.installValidator((resourceUuid, resourceType, systemTag) -> {
            String cores = VmHardwareSystemTags.CPU_CORES.getTokenByTag(systemTag, VmHardwareSystemTags.CPU_CORES_TOKEN);
            try {
                Integer.valueOf(cores);
            } catch (NumberFormatException e) {
                throw new ApiMessageInterceptionException(argerr("cpuCores must be an integer"));
            }
        });

        VmHardwareSystemTags.CPU_THREADS.installValidator((resourceUuid, resourceType, systemTag) -> {
            String threads = VmHardwareSystemTags.CPU_THREADS.getTokenByTag(systemTag, VmHardwareSystemTags.CPU_THREADS_TOKEN);
            try {
                Integer.valueOf(threads);
            } catch (NumberFormatException e) {
                throw new ApiMessageInterceptionException(argerr("cpuThreads must be an integer"));
            }
        });
    }

    private void installStaticIpValidator() {
        class StaticIpValidator implements SystemTagCreateMessageValidator, SystemTagValidator {
            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                ipOperator.validateStaticIpTagsInApiMessage(msg);
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                if (VmSystemTags.STATIC_IP.isMatch(systemTag)) {
                    validateStaticIp(systemTag);
                } else if (VmSystemTags.IPV4_GATEWAY.isMatch(systemTag)) {
                    validateIpv4Gateway(systemTag);
                } else if (VmSystemTags.IPV6_GATEWAY.isMatch(systemTag)) {
                    validateIpv6Gateway(systemTag);
                } else if (VmSystemTags.IPV4_NETMASK.isMatch(systemTag)) {
                    validateIpv4NetMask(systemTag);
                } else if (VmSystemTags.IPV6_PREFIX.isMatch(systemTag)) {
                    validateIpv6Prefix(systemTag);
                }
            }

            private void validateStaticIp(String sysTag) {
                String tagUsage = VmSystemTags.STATIC_IP_TOKEN;
                Map<String, String> token = TagUtils.parse(VmSystemTags.STATIC_IP.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN);
                if (!dbf.isExist(l3Uuid, L3NetworkVO.class)) {
                    throw new ApiMessageInterceptionException(argerr("L3 network[uuid:%s] not found. Please correct your system tag[%s] of %s",
                            l3Uuid, sysTag, tagUsage));
                }

                String ip = token.get(VmSystemTags.STATIC_IP_TOKEN);
                ip = IPv6NetworkUtils.ipv6TagValueToAddress(ip);
                if (!NetworkUtils.isIpv4Address(ip) && !IPv6NetworkUtils.isIpv6Address(ip)) {
                    throw new ApiMessageInterceptionException(argerr("%s is not a valid %s. Please correct your system tag[%s] of %s",
                            ip, tagUsage, sysTag, tagUsage));
                }

                L3NetworkVO l3NetworkVO = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
                if (!l3NetworkVO.enableIpAllocation()) {
                    if (Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, ip).eq(UsedIpVO_.l3NetworkUuid, l3Uuid).isExists()) {
                        logger.warn(String.format("IP[%s] is already used on the L3 network[uuid:%s]", ip, l3Uuid));
                    }
                }
            }

            private void validateIpv4Gateway(String sysTag) {
                String tagUsage = VmSystemTags.IPV4_GATEWAY_TOKEN;
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV4_GATEWAY.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV4_GATEWAY_L3_UUID_TOKEN);
                if (!dbf.isExist(l3Uuid, L3NetworkVO.class)) {
                    throw new ApiMessageInterceptionException(argerr("L3 network[uuid:%s] not found. Please correct your system tag[%s] of %s",
                            l3Uuid, sysTag, tagUsage));
                }

                String ip = token.get(VmSystemTags.IPV4_GATEWAY_TOKEN);
                if (!NetworkUtils.isIpv4Address(ip)) {
                    throw new ApiMessageInterceptionException(argerr("%s is not a valid %s. Please correct your system tag[%s] of %s",
                            ip, tagUsage, sysTag, tagUsage));
                }
            }

            private void validateIpv6Gateway(String sysTag) {
                String tagUsage = VmSystemTags.IPV6_GATEWAY_TOKEN;
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV6_GATEWAY.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV6_GATEWAY_L3_UUID_TOKEN);
                if (!dbf.isExist(l3Uuid, L3NetworkVO.class)) {
                    throw new ApiMessageInterceptionException(argerr("L3 network[uuid:%s] not found. Please correct your system tag[%s] of %s",
                            l3Uuid, sysTag, tagUsage));
                }

                String ip = IPv6NetworkUtils.ipv6TagValueToAddress(token.get(VmSystemTags.IPV6_GATEWAY_TOKEN));
                if (!IPv6NetworkUtils.isIpv6Address(ip)) {
                    throw new ApiMessageInterceptionException(argerr("%s is not a valid %s. Please correct your system tag[%s] of %s",
                            ip, tagUsage, sysTag, tagUsage));
                }
            }

            private void validateIpv4NetMask(String sysTag) {
                String tagUsage = VmSystemTags.IPV4_NETMASK_TOKEN;
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV4_NETMASK.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV4_NETMASK_L3_UUID_TOKEN);
                if (!dbf.isExist(l3Uuid, L3NetworkVO.class)) {
                    throw new ApiMessageInterceptionException(argerr("L3 network[uuid:%s] not found. Please correct your system tag[%s] of %s",
                            l3Uuid, sysTag, tagUsage));
                }

                String netMask = token.get(VmSystemTags.IPV4_NETMASK_TOKEN);
                if (!NetworkUtils.isNetmask(netMask)) {
                    throw new ApiMessageInterceptionException(argerr("%s is not a valid %s. Please correct your system tag[%s] of %s",
                            netMask, tagUsage, sysTag, tagUsage));
                }
            }

            private void validateIpv6Prefix(String sysTag) {
                String tagUsage = VmSystemTags.IPV6_PREFIX_TOKEN;
                Map<String, String> token = TagUtils.parse(VmSystemTags.IPV6_PREFIX.getTagFormat(), sysTag);
                String l3Uuid = token.get(VmSystemTags.IPV6_PREFIX_L3_UUID_TOKEN);
                if (!dbf.isExist(l3Uuid, L3NetworkVO.class)) {
                    throw new ApiMessageInterceptionException(argerr("L3 network[uuid:%s] not found. Please correct your system tag[%s] of %s",
                            l3Uuid, sysTag, tagUsage));
                }

                int prefixLen = Integer.parseInt(token.get(VmSystemTags.IPV6_PREFIX_TOKEN));
                if (prefixLen > IPv6Constants.IPV6_PREFIX_LEN_MAX || prefixLen < IPv6Constants.IPV6_PREFIX_LEN_MIN) {
                    throw new ApiMessageInterceptionException(argerr("ip range prefix length[%d] is out of range [%d - %d]. Please correct your system tag[%s] of %s",
                            prefixLen, IPv6Constants.IPV6_PREFIX_LEN_MIN, IPv6Constants.IPV6_PREFIX_LEN_MAX, sysTag, tagUsage));
                }
            }
        }

        StaticIpValidator staticIpValidator = new StaticIpValidator();
        tagMgr.installCreateMessageValidator(VmInstanceVO.class.getSimpleName(), staticIpValidator);
        VmSystemTags.STATIC_IP.installValidator(staticIpValidator);
    }

    private void installUserdataValidator() {
        class UserDataValidator implements SystemTagCreateMessageValidator, SystemTagValidator {

            private void check(String resourceUuid, Class resourceType) {
                int existUserdataTagCount = VmSystemTags.USERDATA.getTags(resourceUuid, resourceType).size();
                if (existUserdataTagCount > 0) {
                    throw new OperationFailureException(argerr(
                            "Already have one userdata systemTag for vm[uuid: %s].",
                            resourceUuid));
                }
            }

            private void checkUserdataDecode(String systemTag) {
                String userdata = VmSystemTags.USERDATA.getTokenByTag(systemTag, VmSystemTags.USERDATA_TOKEN);
                Base64.getDecoder().decode(userdata.getBytes());
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                if (!VmSystemTags.USERDATA.isMatch(systemTag)) {
                    return;
                }
                check(resourceUuid, resourceType);
                checkUserdataDecode(systemTag);
            }

            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                int userdataTagCount = 0;
                for (String sysTag : msg.getSystemTags()) {
                    if (VmSystemTags.USERDATA.isMatch(sysTag)) {
                        if (userdataTagCount > 0) {
                            throw new OperationFailureException(argerr(
                                    "Shouldn't be more than one userdata systemTag for one vm."));
                        }
                        userdataTagCount++;

                        check(msg.getResourceUuid(), VmInstanceVO.class);
                        checkUserdataDecode(sysTag);
                    }
                }
            }
        }

        UserDataValidator userDataValidator = new UserDataValidator();
        tagMgr.installCreateMessageValidator(VmInstanceVO.class.getSimpleName(), userDataValidator);
        VmSystemTags.USERDATA.installValidator(userDataValidator);
    }

    private void installBootModeValidator() {
        class BootModeValidator implements SystemTagCreateMessageValidator, SystemTagValidator {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                if (!VmSystemTags.BOOT_MODE.isMatch(systemTag)) {
                    return;
                }

                String bootMode = VmSystemTags.BOOT_MODE.getTokenByTag(systemTag, VmSystemTags.BOOT_MODE_TOKEN);
                validateBootMode(systemTag, bootMode);
            }

            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                if (msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
                    return;
                }

                int bootModeCount = 0;
                for (String systemTag : msg.getSystemTags()) {
                    if (VmSystemTags.BOOT_MODE.isMatch(systemTag)) {
                        if (++bootModeCount > 1) {
                            throw new ApiMessageInterceptionException(argerr("only one bootMode system tag is allowed, but %d got", bootModeCount));
                        }

                        String bootMode = VmSystemTags.BOOT_MODE.getTokenByTag(systemTag, VmSystemTags.BOOT_MODE_TOKEN);
                        validateBootMode(systemTag, bootMode);
                    }
                }
            }

            private void validateBootMode(String systemTag, String bootMode) {
                boolean valid = false;
                for (ImageBootMode bm : ImageBootMode.values()) {
                    if (bm.name().equalsIgnoreCase(bootMode)) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    throw new ApiMessageInterceptionException(argerr(
                            "[%s] specified in system tag [%s] is not a valid boot mode", bootMode, systemTag)
                    );
                }
            }
        }

        BootModeValidator validator = new BootModeValidator();
        tagMgr.installCreateMessageValidator(VmInstanceVO.class.getSimpleName(), validator);
        VmSystemTags.BOOT_MODE.installValidator(validator);
    }

    private void installCleanTrafficValidator() {
        class CleanTrafficValidator implements SystemTagCreateMessageValidator, SystemTagValidator {
            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                if (msg instanceof APICreateVmInstanceMsg) {
                    Optional.ofNullable(msg.getSystemTags()).ifPresent(it -> {
                        if (it.stream().anyMatch(tag -> VmSystemTags.CLEAN_TRAFFIC.isMatch(tag))) {
                            validateVmType(null, ((APICreateVmInstanceMsg) msg).getType());
                        }
                    });
                }
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                validateVmType(resourceUuid, null);
            }

            private void validateVmType(String vmUuid, String vmType) {
                if (vmType == null) {
                    vmType = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).select(VmInstanceVO_.type).findValue();
                }

                if (!VmInstanceConstant.USER_VM_TYPE.equals(vmType)) {
                    throw new ApiMessageInterceptionException(argerr(
                            "clean traffic is not supported for vm type [%s]", vmType)
                    );
                }
            }
        }

        CleanTrafficValidator validator = new CleanTrafficValidator();
        tagMgr.installCreateMessageValidator(VmInstanceVO.class.getSimpleName(), validator);
        VmSystemTags.CLEAN_TRAFFIC.installValidator(validator);
    }

    private void installMachineTypeValidator() {
        class MachineTypeValidator implements SystemTagCreateMessageValidator, SystemTagValidator {
            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                Optional.ofNullable(msg.getSystemTags()).ifPresent(it -> it.forEach(this::validateMachineType));
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                validateMachineType(systemTag);
            }

            private void validateMachineType(String systemTag) {
                if (!VmSystemTags.MACHINE_TYPE.isMatch(systemTag)) {
                    return;
                }

                String type = VmSystemTags.MACHINE_TYPE.getTokenByTag(systemTag, VmSystemTags.MACHINE_TYPE_TOKEN);
                if (VmMachineType.get(type) == null) {
                    throw new ApiMessageInterceptionException(argerr("vm machine type requires [q35, pc, virt], but get [%s]", type));
                }
            }
        }

        MachineTypeValidator validator = new MachineTypeValidator();
        tagMgr.installCreateMessageValidator(VmInstanceVO.class.getSimpleName(), validator);
        VmSystemTags.MACHINE_TYPE.installValidator(validator);
    }

    private void installL3NetworkSecurityGroupValidator() {
        class L3NetworkSecurityGroupValidator implements SystemTagCreateMessageValidator, SystemTagValidator {

            @Override
            public void validateSystemTagInCreateMessage(APICreateMessage msg) {
                if (msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
                    return;
                }

                for (String systemTag : msg.getSystemTags()) {
                    validateL3NetworkSecurityGroup(systemTag);
                }
            }

            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                validateL3NetworkSecurityGroup(systemTag);
            }

            private void validateL3NetworkSecurityGroup(String systemTag) {
                if (!VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.isMatch(systemTag)) {
                    return;
                }

                String l3Uuid = VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTokenByTag(systemTag, VmSystemTags.L3_UUID_TOKEN);
                List<String> securityGroupUuids = asList(VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF
                        .getTokenByTag(systemTag, VmSystemTags.SECURITY_GROUP_UUIDS_TOKEN).split(","));

                validateL3NetworkAttachSecurityGroup(l3Uuid, securityGroupUuids);
            }

            private void validateL3NetworkAttachSecurityGroup(String l3Uuid, List<String> securityGroupUuids) {
                pluginRgty.getExtensionList(ValidateL3SecurityGroupExtensionPoint.class)
                        .forEach(ext -> ext.validateSystemtagL3SecurityGroup(l3Uuid, securityGroupUuids));
            }
        }

        L3NetworkSecurityGroupValidator validator = new L3NetworkSecurityGroupValidator();
        tagMgr.installCreateMessageValidator(VmInstanceVO.class.getSimpleName(), validator);
        VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.installValidator(validator);
    }

    private void installSeDeviceValidator() {
        VmSystemTags.SECURITY_ELEMENT_ENABLE.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String SecurityElementEnableTokenByTag = null;
                if (VmSystemTags.SECURITY_ELEMENT_ENABLE.isMatch(systemTag)) {
                    SecurityElementEnableTokenByTag = VmSystemTags.SECURITY_ELEMENT_ENABLE.getTokenByTag(systemTag, VmSystemTags.SECURITY_ELEMENT_ENABLE_TOKEN);
                } else {
                    throw new OperationFailureException(argerr("invalid securityElementEnable[%s], %s is not securityElementEnable tag", systemTag, SecurityElementEnableTokenByTag));
                }
                if (!isBoolean(SecurityElementEnableTokenByTag)) {
                    throw new OperationFailureException(argerr("invalid securityElementEnable[%s], %s is not boolean class", systemTag, SecurityElementEnableTokenByTag));
                }
            }
            private boolean isBoolean(String param) {
                return "true".equalsIgnoreCase(param) || "false".equalsIgnoreCase(param);
            }
        });
    }

    private void installSystemTagValidator() {
        installHostnameValidator();
        installStaticIpValidator();
        installUserdataValidator();
        installBootModeValidator();
        installCleanTrafficValidator();
        installMachineTypeValidator();
        installUsbRedirectValidator();
        installL3NetworkSecurityGroupValidator();
        installSeDeviceValidator();
    }
    private void installUsbRedirectValidator() {
        VmSystemTags.USB_REDIRECT.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String usbRedirectTokenByTag = null;
                if (VmSystemTags.USB_REDIRECT.isMatch(systemTag)) {
                    usbRedirectTokenByTag = VmSystemTags.USB_REDIRECT.getTokenByTag(systemTag, VmSystemTags.USB_REDIRECT_TOKEN);
                } else {
                    throw new OperationFailureException(argerr("invalid usbRedirect[%s], %s is not usbRedirect tag", systemTag, usbRedirectTokenByTag));
                }
                if (!isBoolean(usbRedirectTokenByTag)) {
                    throw new OperationFailureException(argerr("invalid usbRedirect[%s], %s is not boolean class", systemTag, usbRedirectTokenByTag));
                }
            }
            private boolean isBoolean(String param) {
                return "true".equalsIgnoreCase(param) || "false".equalsIgnoreCase(param);
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public VmInstanceFactory getVmInstanceFactory(VmInstanceType type) {
        VmInstanceFactory factory = vmFactoryManager.getVmInstanceFactory(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No VmInstanceFactory of type[%s] found", type));
        }
        return factory;
    }

    @Override
    public VmInstanceBaseExtensionFactory getVmInstanceBaseExtensionFactory(Message msg) {
        return vmFactoryManager.getVmInstanceBaseExtensionFactory(msg.getClass());
    }

    @Override
    public VmInstanceNicFactory getVmInstanceNicFactory(VmNicType type) {
        VmInstanceNicFactory factory = vmFactoryManager.getVmInstanceNicFactory(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No VmInstanceNicFactory of type[%s] found", type));
        }
        return factory;
    }

    @Override
    public FlowChain getCreateVmWorkFlowChain(VmInstanceInventory inv) {
        return createVmFlowBuilder.build();
    }

    @Override
    public FlowChain getStopVmWorkFlowChain(VmInstanceInventory inv) {
        return stopVmFlowBuilder.build();
    }

    @Override
    public FlowChain getRebootVmWorkFlowChain(VmInstanceInventory inv) {
        return rebootVmFlowBuilder.build();
    }

    @Override
    public FlowChain getStartVmWorkFlowChain(VmInstanceInventory inv) {
        return startVmFlowBuilder.build();
    }

    @Override
    public FlowChain getDestroyVmWorkFlowChain(VmInstanceInventory inv) {
        return destroyVmFlowBuilder.build();
    }

    @Override
    public FlowChain getMigrateVmWorkFlowChain(VmInstanceInventory inv) {
        return migrateVmFlowBuilder.build();
    }

    @Override
    public FlowChain getAttachUninstantiatedVolumeWorkFlowChain(VmInstanceInventory inv) {
        return attachVolumeFlowBuilder.build();
    }


    @Override
    public FlowChain getAttachIsoWorkFlowChain(VmInstanceInventory inv) {
        return attachIsoFlowBuilder.build();
    }

    @Override
    public FlowChain getDetachIsoWorkFlowChain(VmInstanceInventory inv) {
        return detachIsoFlowBuilder.build();
    }

    @Override
    public FlowChain getExpungeVmWorkFlowChain(VmInstanceInventory inv) {
        return expungeVmFlowBuilder.build();
    }

    public FlowChain getPauseWorkFlowChain(VmInstanceInventory inv) {
        return pauseVmFlowBuilder.build();
    }

    public FlowChain getResumeVmWorkFlowChain(VmInstanceInventory inv) {
        return resumeVmFlowBuilder.build();
    }

    public void setCreateVmWorkFlowElements(List<String> createVmWorkFlowElements) {
        this.createVmWorkFlowElements = createVmWorkFlowElements;
    }

    public void setStopVmWorkFlowElements(List<String> stopVmWorkFlowElements) {
        this.stopVmWorkFlowElements = stopVmWorkFlowElements;
    }

    public void setRebootVmWorkFlowElements(List<String> rebootVmWorkFlowElements) {
        this.rebootVmWorkFlowElements = rebootVmWorkFlowElements;
    }

    public void setStartVmWorkFlowElements(List<String> startVmWorkFlowElements) {
        this.startVmWorkFlowElements = startVmWorkFlowElements;
    }

    public void setDestroyVmWorkFlowElements(List<String> destroyVmWorkFlowElements) {
        this.destroyVmWorkFlowElements = destroyVmWorkFlowElements;
    }

    public void setMigrateVmWorkFlowElements(List<String> migrateVmWorkFlowElements) {
        this.migrateVmWorkFlowElements = migrateVmWorkFlowElements;
    }

    public void setAttachVolumeWorkFlowElements(List<String> attachVolumeWorkFlowElements) {
        this.attachVolumeWorkFlowElements = attachVolumeWorkFlowElements;
    }

    public void setAttachIsoWorkFlowElements(List<String> attachIsoWorkFlowElements) {
        this.attachIsoWorkFlowElements = attachIsoWorkFlowElements;
    }

    public void setDetachIsoWorkFlowElements(List<String> detachIsoWorkFlowElements) {
        this.detachIsoWorkFlowElements = detachIsoWorkFlowElements;
    }

    public void setExpungeVmWorkFlowElements(List<String> expungeVmWorkFlowElements) {
        this.expungeVmWorkFlowElements = expungeVmWorkFlowElements;
    }

    public void setPauseVmWorkFlowElements(List<String> pauseVmWorkFlowElements) {
        this.pauseVmWorkFlowElements = pauseVmWorkFlowElements;
    }

    public void setResumeVmWorkFlowElements(List<String> resumeVmWorkFlowElements) {
        this.resumeVmWorkFlowElements = resumeVmWorkFlowElements;
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new VmTotalNumQuotaDefinition());
        quota.defineQuota(new VmRunningNumQuotaDefinition());
        quota.defineQuota(new VmRunningCpuNumQuotaDefinition());
        quota.defineQuota(new VmRunningMemoryNumQuotaDefinition());
        quota.defineQuota(new DataVolumeNumQuotaDefinition());
        quota.defineQuota(new VolumeSizeQuotaDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateVmInstanceMsg.class)
                .addCounterQuota(VmQuotaConstant.VM_TOTAL_NUM)
                .addCounterQuota(VmQuotaConstant.VM_RUNNING_NUM)
                .addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_CPU_NUM, (msg) -> {
                    if (msg.getCpuNum() != null) {
                        return Integer.toUnsignedLong(msg.getCpuNum());
                    }

                    Integer cpuNum = Q.New(InstanceOfferingVO.class)
                            .select(InstanceOfferingVO_.cpuNum)
                            .eq(InstanceOfferingVO_.uuid, msg.getInstanceOfferingUuid())
                            .findValue();
                    return Integer.toUnsignedLong(cpuNum);
                }).addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_MEMORY_SIZE, (msg) -> {
                    if (msg.getMemorySize() != null) {
                        return msg.getMemorySize();
                    }

                    return Q.New(InstanceOfferingVO.class)
                            .select(InstanceOfferingVO_.memorySize)
                            .eq(InstanceOfferingVO_.uuid, msg.getInstanceOfferingUuid())
                            .findValue();
                }).addMessageRequiredQuotaHandler(VmQuotaConstant.DATA_VOLUME_NUM, (msg) -> {
                    if (msg.getDataDiskOfferingUuids() == null || msg.getDataDiskOfferingUuids().isEmpty()) {
                        return 0L;
                    }

                    return (long) (msg.getDataDiskOfferingUuids().size());
                }).addMessageRequiredQuotaHandler(VmQuotaConstant.VOLUME_SIZE, (msg) ->  {
                    long allVolumeSizeAsked = 0;

                    String sql;
                    Long imgSize;
                    ImageConstant.ImageMediaType imgType = null;
                    if (msg.getImageUuid() != null) {
                        sql = "select img.size, img.mediaType" +
                                " from ImageVO img" +
                                " where img.uuid = :iuuid";
                        TypedQuery<Tuple> iq = dbf.getEntityManager().createQuery(sql, Tuple.class);
                        iq.setParameter("iuuid", msg.getImageUuid());
                        Tuple it = iq.getSingleResult();
                        imgSize = it.get(0, Long.class);
                        imgType = it.get(1, ImageConstant.ImageMediaType.class);
                    } else {
                        imgSize = 0L;
                    }

                    List<String> diskOfferingUuids = new ArrayList<>();
                    if (msg.getDataDiskOfferingUuids() != null && !msg.getDataDiskOfferingUuids().isEmpty()) {
                        diskOfferingUuids.addAll(msg.getDataDiskOfferingUuids());
                    }
                    if (imgType == ImageConstant.ImageMediaType.RootVolumeTemplate) {
                        allVolumeSizeAsked += imgSize;
                    } else if (imgType == ImageConstant.ImageMediaType.ISO) {
                        if (msg.getRootDiskOfferingUuid() != null) {
                            diskOfferingUuids.add(msg.getRootDiskOfferingUuid());
                        } else if (msg.getRootDiskSize() != null) {
                            allVolumeSizeAsked += msg.getRootDiskSize();
                        } else {
                            throw new ApiMessageInterceptionException(argerr("rootDiskOfferingUuid cannot be null when image mediaType is ISO"));
                        }
                    } else {
                        if (msg.getRootDiskOfferingUuid() != null) {
                            diskOfferingUuids.add(msg.getRootDiskOfferingUuid());
                        } else if (msg.getRootDiskSize() != null) {
                            allVolumeSizeAsked += msg.getRootDiskSize();
                        } else {
                            throw new ApiMessageInterceptionException(argerr("rootDiskOfferingUuid cannot be null when create vm without image"));
                        }
                    }

                    HashMap<String, Long> diskOfferingCountMap = new HashMap<>();
                    if (!diskOfferingUuids.isEmpty()) {
                        for (String diskOfferingUuid : diskOfferingUuids) {
                            if (diskOfferingCountMap.containsKey(diskOfferingUuid)) {
                                diskOfferingCountMap.put(diskOfferingUuid, diskOfferingCountMap.get(diskOfferingUuid) + 1);
                            } else {
                                diskOfferingCountMap.put(diskOfferingUuid, 1L);
                            }
                        }
                        for (String diskOfferingUuid : diskOfferingCountMap.keySet()) {
                            sql = "select diskSize from DiskOfferingVO where uuid = :uuid";
                            TypedQuery<Long> dq = dbf.getEntityManager().createQuery(sql, Long.class);
                            dq.setParameter("uuid", diskOfferingUuid);
                            Long dsize = dq.getSingleResult();
                            dsize = dsize == null ? 0 : dsize;
                            allVolumeSizeAsked += dsize * diskOfferingCountMap.get(diskOfferingUuid);
                        }
                    }

                    return allVolumeSizeAsked;
                }).addCheckCondition((msg) -> !msg.getStrategy().equals(VmCreationStrategy.JustCreate.toString())));

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIRecoverVmInstanceMsg.class)
                .addCounterQuota(VmQuotaConstant.VM_TOTAL_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIStartVmInstanceMsg.class)
                .addCounterQuota(VmQuotaConstant.VM_RUNNING_NUM)
                .addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_CPU_NUM, (msg) -> new VmQuotaUtil().getRequiredCpu(msg.getVmInstanceUuid()))
                .addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_MEMORY_SIZE, (msg) -> new VmQuotaUtil().getRequiredMemory(msg.getVmInstanceUuid())));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(StartVmInstanceMsg.class)
                .addCounterQuota(VmQuotaConstant.VM_RUNNING_NUM)
                .addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_CPU_NUM, (msg) -> new VmQuotaUtil().getRequiredCpu(msg.getVmInstanceUuid()))
                .addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_MEMORY_SIZE, (msg) -> new VmQuotaUtil().getRequiredMemory(msg.getVmInstanceUuid())));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateDataVolumeMsg.class)
                .addMessageRequiredQuotaHandler(VmQuotaConstant.VOLUME_SIZE, (msg) -> {
                    if (msg.getDiskOfferingUuid() == null) {
                        return msg.getDiskSize();
                    }

                    String sql = "select diskSize from DiskOfferingVO where uuid = :uuid ";
                    TypedQuery<Long> dq = dbf.getEntityManager().createQuery(sql, Long.class);
                    dq.setParameter("uuid", msg.getDiskOfferingUuid());
                    Long dsize = dq.getSingleResult();
                    dsize = dsize == null ? 0 : dsize;
                    return dsize;
                })
                .addCounterQuota(VmQuotaConstant.DATA_VOLUME_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIRecoverDataVolumeMsg.class)
                .addCounterQuota(VmQuotaConstant.DATA_VOLUME_NUM));

        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, msg.getResourceUuid())
                        .notEq(VmInstanceVO_.type, "baremetal2")
                        .isExists())
                .addCounterQuota(VmQuotaConstant.VM_TOTAL_NUM)
                .addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_CPU_NUM, (msg) -> {
                    VmInstanceState state = Q.New(VmInstanceVO.class)
                            .select(VmInstanceVO_.state)
                            .eq(VmInstanceVO_.uuid, msg.getResourceUuid())
                            .findValue();

                    // vm is running
                    if (list(VmInstanceState.Stopped, VmInstanceState.Destroying,
                            VmInstanceState.Destroyed, VmInstanceState.Created).contains(state)) {
                        return 0L;
                    }

                    return new VmQuotaUtil().getRequiredCpu(msg.getResourceUuid());
                }).addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_MEMORY_SIZE, (msg) -> {
                    VmInstanceState state = Q.New(VmInstanceVO.class)
                            .select(VmInstanceVO_.state)
                            .eq(VmInstanceVO_.uuid, msg.getResourceUuid())
                            .findValue();

                    // vm is running
                    if (list(VmInstanceState.Stopped, VmInstanceState.Destroying,
                            VmInstanceState.Destroyed, VmInstanceState.Created).contains(state)) {
                        return 0L;
                    }

                    return new VmQuotaUtil().getRequiredMemory(msg.getResourceUuid());
                }).addMessageRequiredQuotaHandler(VmQuotaConstant.VM_RUNNING_NUM, (msg) -> {
                    VmInstanceState state = Q.New(VmInstanceVO.class)
                            .select(VmInstanceVO_.state)
                            .eq(VmInstanceVO_.uuid, msg.getResourceUuid())
                            .findValue();

                    // vm is running
                    if (list(VmInstanceState.Stopped, VmInstanceState.Destroying,
                            VmInstanceState.Destroyed, VmInstanceState.Created).contains(state)) {
                        return 0L;
                    }

                    return 1L;
                }));
        return list(quota);
    }

    @Override
    @AsyncThread
    public void managementNodeReady() {
        //checkUnknownVm();
        startVmExpungeTask();
    }

    private synchronized void startVmExpungeTask() {
        if (expungeVmTask != null) {
            expungeVmTask.cancel(true);
        }

        expungeVmTask = thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {

            private List<Tuple> getVmDeletedStateManagedByUs() {
                int qun = 10000;
                SimpleQuery q = dbf.createQuery(VmInstanceVO.class);
                q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Destroyed);
                long amount = q.count();
                int times = (int) (amount / qun) + (amount % qun != 0 ? 1 : 0);
                int start = 0;
                List<Tuple> ret = new ArrayList<>();
                for (int i = 0; i < times; i++) {
                    q = dbf.createQuery(VmInstanceVO.class);
                    q.select(VmInstanceVO_.uuid, VmInstanceVO_.lastOpDate);
                    q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Destroyed);
                    q.setLimit(qun);
                    q.setStart(start);
                    List<Tuple> ts = q.listTuple();
                    start += qun;

                    for (Tuple t : ts) {
                        String vmUuid = t.get(0, String.class);
                        if (!destMaker.isManagedByUs(vmUuid)) {
                            continue;
                        }
                        ret.add(t);
                    }
                }

                return ret;
            }

            @Override
            public synchronized boolean run() {
                final List<Tuple> vms = getVmDeletedStateManagedByUs();
                if (vms.isEmpty()) {
                    logger.debug("[VM Expunging Task]: no vm to expunge");
                    return false;
                }

                final Timestamp current = dbf.getCurrentSqlTime();
                new While<>(vms).step((t, completion) -> {
                    String uuid = t.get(0, String.class);
                    Timestamp date = t.get(1, Timestamp.class);
                    long end = date.getTime() + TimeUnit.SECONDS.toMillis(VmGlobalConfig.VM_EXPUNGE_PERIOD.value(Long.class));
                    if (current.getTime() < end) {
                        completion.done();
                        return;
                    }

                    VmInstanceDeletionPolicy deletionPolicy = deletionPolicyMgr.getDeletionPolicy(uuid);
                    if (deletionPolicy == VmInstanceDeletionPolicy.Never) {
                        logger.debug(String.format(
                                "[VM Expunging Task]: the deletion policy of the vm[uuid:%s] is Never, don't expunge it",
                                uuid));
                        completion.done();
                        return;
                    }

                    ExpungeVmMsg msg = new ExpungeVmMsg();
                    msg.setVmInstanceUuid(uuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, uuid);
                    bus.send(msg, new CloudBusCallBack(completion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.debug(String.format("successfully expunged the vm[uuid:%s]",
                                        msg.getVmInstanceUuid()));
                            } else {
                                logger.warn(String.format("failed to expunge the vm[uuid:%s], %s",
                                        msg.getVmInstanceUuid(), reply.getError()));
                                completion.addError(reply.getError());
                            }
                            completion.done();
                        }
                    });
                }, 3).run(new WhileDoneCompletion(null) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        logger.debug(String.format("expungeVmTask done, %d/%d failed",
                                errorCodeList.getCauses().size(), vms.size()));
                    }
                });
                return false;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return VmGlobalConfig.VM_EXPUNGE_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "expunge-vm-task";
            }
        });

        logger.debug(String.format("vm expunging task starts running, [period: %s seconds, interval: %s seconds]",
                VmGlobalConfig.VM_EXPUNGE_PERIOD.value(Long.class), VmGlobalConfig.VM_EXPUNGE_INTERVAL.value(Long.class)));
    }

    @Override
    public String preDeleteL3Network(L3NetworkInventory inventory) throws L3NetworkException {
        return null;
    }

    @Override
    public void beforeDeleteL3Network(L3NetworkInventory inventory) {
    }

    @Override
    public void afterDeleteL3Network(L3NetworkInventory inventory) {
        new StaticIpOperator().deleteStaticIpByL3NetworkUuid(inventory.getUuid());
    }

    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        if (!VmInstanceVO.class.getSimpleName().equals(ref.getResourceType())) {
            return;
        }

        // change root volume
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.rootVolumeUuid);
        q.add(VmInstanceVO_.uuid, Op.EQ, ref.getResourceUuid());
        String rootVolumeUuid = q.findValue();
        if (rootVolumeUuid == null) {
            return;
        }

        acntMgr.changeResourceOwner(rootVolumeUuid, newOwnerUuid);

        // change vmnic(s)
        SimpleQuery<VmNicVO> sq = dbf.createQuery(VmNicVO.class);
        sq.select(VmNicVO_.uuid);
        sq.add(VmNicVO_.vmInstanceUuid, Op.EQ, ref.getResourceUuid());
        List<String> vmnics = sq.listValue();
        if (vmnics.isEmpty()) {
            return;
        }
        for (String vmnicUuid : vmnics) {
            acntMgr.changeResourceOwner(vmnicUuid, newOwnerUuid);
        }

        changeVmCdRomsOwner(ref.getResourceUuid(), newOwnerUuid);
    }

    private void changeVmCdRomsOwner(String vmInstanceUuid, String newOwnerUuid) {
        List<String> vmCdRomUuids = Q.New(VmCdRomVO.class)
                .select(VmCdRomVO_.uuid)
                .eq(VmCdRomVO_.vmInstanceUuid, vmInstanceUuid)
                .listValues();
        if (vmCdRomUuids.isEmpty()) {
            return;
        }

        for (String uuid :vmCdRomUuids) {
            acntMgr.changeResourceOwner(uuid, newOwnerUuid);
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APIChangeResourceOwnerMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIChangeResourceOwnerMsg) {
            validateAPIChangeResourceOwnerMsg((APIChangeResourceOwnerMsg) msg);
        }

        return msg;
    }

    private void validateAPIChangeResourceOwnerMsg(APIChangeResourceOwnerMsg msg) {
        AccountResourceRefVO ref = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, msg.getResourceUuid())
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .find();

        if (ref == null || !VolumeVO.class.getSimpleName().equals(ref.getResourceType())) {
            return;
        }

        SimpleQuery<VolumeVO> vq = dbf.createQuery(VolumeVO.class);
        vq.add(VolumeVO_.uuid, Op.EQ, ref.getResourceUuid());
        vq.add(VolumeVO_.type, Op.EQ, VolumeType.Root);
        if (vq.isExists()) {
            throw new OperationFailureException(operr("the resource[uuid:%s] is a ROOT volume, you cannot change its owner, instead," +
                            "change the owner of the VM the root volume belongs to", ref.getResourceUuid()));
        }
    }

    @Override
    public void afterChangeHostStatus(String hostUuid, HostStatus before, HostStatus next) {
        if(next == HostStatus.Disconnected) {
            List<Tuple> vms = Q.New(VmInstanceVO.class).select(VmInstanceVO_.uuid, VmInstanceVO_.state)
                    .eq(VmInstanceVO_.hostUuid, hostUuid)
                    .listTuple();
            if(vms.isEmpty()){
                return;
            }

            new While<>(vms).step((vm, completion) -> {
                String vmUuid = vm.get(0, String.class);
                String vmState = vm.get(1, VmInstanceState.class).toString();
                VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg();
                msg.setVmInstanceUuid(vmUuid);
                msg.setHostUuid(hostUuid);
                msg.setStateOnHost(VmInstanceState.Unknown);
                bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if(!reply.isSuccess()){
                            logger.warn(String.format("the host[uuid:%s] disconnected, but the vm[uuid:%s] fails to " +
                                            "change it's state to Unknown, %s", hostUuid, vmUuid, reply.getError()));
                            logger.warn(String.format("create an unknowngc job for vm[uuid:%s]", vmUuid));

                            UnknownVmGC gc = new UnknownVmGC();
                            gc.NAME = UnknownVmGC.getGCName(vmUuid);
                            gc.vmUuid = vmUuid;
                            gc.vmState = vmState;
                            gc.hostUuid = hostUuid;
                            if (gc.existedAndNotCompleted()) {
                                logger.debug(String.format("There is already a UnknownVmGC of vm[uuid:%s] " +
                                        "on host[uuid:%s], skip.", vmUuid, hostUuid));
                            } else {
                                gc.submit(VmGlobalConfig.UNKNOWN_GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);
                            }
                        } else {
                            logger.debug(String.format("the host[uuid:%s] disconnected, change the VM[uuid:%s]' state to Unknown", hostUuid, vmUuid));
                        }
                        completion.done();
                    }
                });
            }, 20).run(new NopeWhileDoneCompletion());
        }
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        if (!inv.getHypervisorType().equals(VmInstanceConstant.KVM_HYPERVISOR_TYPE)) {
            return;
        }

        VmPriorityLevel level = new VmPriorityOperator().getVmPriority(inv.getUuid());
        VmPriorityConfigVO priorityVO = Q.New(VmPriorityConfigVO.class).eq(VmPriorityConfigVO_.level, level).find();

        UpdateVmPriorityMsg msg = new UpdateVmPriorityMsg();
        msg.setPriorityConfigStructs(asList(new PriorityConfigStruct(priorityVO, inv.getUuid())));
        msg.setHostUuid(inv.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, inv.getHostUuid());
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                UpdateVmPriorityReply r = new UpdateVmPriorityReply();
                if (!reply.isSuccess()) {
                    logger.warn(String.format("update vm[%s] priority to [%s] failed,because %s",
                            inv.getUuid(), level.toString(), reply.getError()));
                }
            }
        });
    }

    @Override
    public VmNicQosConfigBackend getVmNicQosConfigBackend(String type) {
        return vmFactoryManager.getVmNicQosConfigBackend(type);
    }

    @Override
    public VmDnsBackend getVmDnsBackend(String type) {
        return vmFactoryManager.getVmDnsBackend(type);
    }

    @Override
    public ErrorCode handleSystemTag(String vmUuid, List<String> tags) {
        PatternedSystemTag tag = VmSystemTags.DIRECTORY_UUID;
        String token = VmSystemTags.DIRECTORY_UUID_TOKEN;

        String directoryUuid = SystemTagUtils.findTagValue(tags, tag, token);
        if (StringUtils.isEmpty(directoryUuid)) {
            return null;
        }
        ResourceDirectoryRefVO refVO = new ResourceDirectoryRefVO();
        refVO.setResourceUuid(vmUuid);
        refVO.setDirectoryUuid(directoryUuid);
        refVO.setResourceType(VmInstanceVO.class.getSimpleName());
        refVO.setLastOpDate(new Timestamp(new Date().getTime()));
        refVO.setCreateDate(new Timestamp(new Date().getTime()));
        dbf.persist(refVO);
        return null;
    }

    public void deleteMigrateSystemTagWhenVmStateChangedToRunning() {
        evtf.onLocal(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VmCanonicalEvents.VmStateChangedData d = (VmCanonicalEvents.VmStateChangedData) data;
                if (!Objects.equals(d.getOldState(), VmInstanceState.Migrating.toString()) && Objects.equals(d.getNewState(), VmInstanceState.Running.toString())) {
                    VmSystemTags.VM_STATE_PAUSED_AFTER_MIGRATE.delete(d.getVmUuid());
                }
            }
        });
    }
}
