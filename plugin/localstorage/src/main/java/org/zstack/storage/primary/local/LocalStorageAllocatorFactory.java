package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostAllocatorStrategyExtensionPoint;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.primary.PrimaryStorageCapacityChecker;
import org.zstack.storage.primary.PrimaryStoragePhysicalCapacityManager;
import org.zstack.storage.snapshot.SnapshotDeletionExtensionPoint;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.i18m;
import static org.zstack.header.image.ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageAllocatorFactory implements PrimaryStorageAllocatorStrategyFactory, Component,
        HostAllocatorFilterExtensionPoint, PrimaryStorageAllocatorStrategyExtensionPoint, PrimaryStorageAllocatorFlowNameSetter,
        HostAllocatorStrategyExtensionPoint, SnapshotDeletionExtensionPoint, PSCapacityExtensionPoint {
    private CLogger logger = Utils.getLogger(LocalStorageAllocatorFactory.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;

    public static PrimaryStorageAllocatorStrategyType type = new PrimaryStorageAllocatorStrategyType(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY);

    private List<String> allocatorFlowNames;
    private FlowChainBuilder builder = new FlowChainBuilder();
    private LocalStorageAllocatorStrategy strategy;
    private static HashMap<String, AbstractUriParser> uriParsers = new HashMap<>();

    @Override
    public PrimaryStorageAllocatorStrategyType getPrimaryStorageAllocatorStrategyType() {
        return type;
    }

    @Override
    public PrimaryStorageAllocatorStrategy getPrimaryStorageAllocatorStrategy() {
        return strategy;
    }

    public List<String> getAllocatorFlowNames() {
        return allocatorFlowNames;
    }

    public void setAllocatorFlowNames(List<String> allocatorFlowNames) {
        this.allocatorFlowNames = allocatorFlowNames;
    }

    static {
        parseRequiredInstallUri();
    }

    @Override
    public boolean start() {
        builder.setFlowClassNames(allocatorFlowNames).construct();
        strategy = new LocalStorageAllocatorStrategy(builder);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        if (VmOperation.Start.toString().equals(spec.getVmOperation())) {
            checkLocalStorageForVmStart(spec.getVmInstance(), candidates);
            return;
        }

        if (!VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            return;
        }

        List<String> huuids = getNeedCheckHostLocalStorageList(candidates, spec);
        if (huuids.isEmpty()) {
            return;
        }

        SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
        q.add(LocalStorageHostRefVO_.hostUuid, Op.IN, huuids);
        if (!spec.getRequiredPrimaryStorageUuids().isEmpty()) {
            q.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.IN, spec.getRequiredPrimaryStorageUuids());
        }
        List<LocalStorageHostRefVO> refs = q.list();

        final Set<String> toRemoveHuuids = new HashSet<>();
        final Set<String> toAddHuuids = new HashSet<>();
        for (LocalStorageHostRefVO ref : refs) {
            String huuid = ref.getHostUuid();
            String psUuid = ref.getPrimaryStorageUuid();
            // check primary storage capacity and host physical capacity
            boolean capacityChecked = PrimaryStorageCapacityChecker.New(psUuid,
                    ref.getAvailableCapacity(), ref.getTotalPhysicalCapacity(), ref.getAvailablePhysicalCapacity())
                    .checkRequiredSize(spec.getDiskSize());

            if (!capacityChecked) {
                addHostPrimaryStorageBlacklist(huuid, psUuid, spec);
                toRemoveHuuids.add(huuid);
            } else {
                toAddHuuids.add(huuid);
            }
        }
        // for more than one local storage, maybe one of it fit the requirement
        toRemoveHuuids.removeAll(toAddHuuids);
        if (!toRemoveHuuids.isEmpty()) {
            logger.debug(String.format("local storage filters out hosts%s, because they don't have required disk capacity[%s bytes]",
                    toRemoveHuuids, spec.getDiskSize()));

            for (HostCandidate candidate : candidates) {
                if (toRemoveHuuids.contains(candidate.getUuid())) {
                    candidate.markAsRejected(getClass(), i18m("disk capacity[%s bytes] required", spec.getDiskSize()));
                }
            }
        }
    }

    private void checkLocalStorageForVmStart(VmInstanceInventory vm, List<HostCandidate> candidates) {
        final List<String> localPS = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.uuid)
                .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .listValues();
        if (localPS.isEmpty()) {
            return;
        }

        for (VolumeInventory volume : vm.getAllVolumes()) {
            if (!localPS.contains(volume.getPrimaryStorageUuid())) {
                continue;
            }

            final String hostUuid = Q.New(LocalStorageResourceRefVO.class)
                    .eq(LocalStorageResourceRefVO_.resourceUuid, volume.getUuid())
                    .select(LocalStorageResourceRefVO_.hostUuid)
                    .findValue();
            if (hostUuid != null) {
                for (HostCandidate candidate : candidates) {
                    if (!hostUuid.equals(candidate.getUuid())) {
                        candidate.markAsRejected(getClass(),
                                i18m("local storage volume[uuid:%s] is not on this host", volume.getUuid()));
                    }
                }

                // A VM with local disk can only run on this host
                break;
            }
        }
    }

    /**
     * @return hostUuid list
     * <p>
     * Just check it :
     * The current cluster is mounted only local storage
     * Specified local storage
     * <p>
     * Negative impact
     * In the case of local + non-local and no ps specified (non-local is Disconnected/Disabled, or non-local capacity not enough), the allocated host may not have enough disks
     */
    private List<String> getNeedCheckHostLocalStorageList(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        boolean isRequireNonLocalStorage = spec.getRequiredPrimaryStorageUuids()
                .stream().noneMatch(LocalStorageUtils::isLocalStorage);
        Map<String, List<String>> grouped = candidates.stream().collect(
                Collectors.groupingBy(
                        candidate -> candidate.host.getClusterUuid(),
                        Collectors.mapping(HostCandidate::getUuid, Collectors.toList())
                )
        );

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            boolean isOnlyAttachedLocalStorage = LocalStorageUtils.isOnlyAttachedLocalStorage(entry.getKey());
            if (!isOnlyAttachedLocalStorage && (isRequireNonLocalStorage || spec.isDryRun())) {
                continue;
            }

            result.addAll(entry.getValue());
        }

        return result;
    }

    private void addHostPrimaryStorageBlacklist(String hostUuid, String psUuid, HostAllocatorSpec spec) {
        Map<String, Set<String>> host2PrimaryStorageBlacklist = spec.getHost2PrimaryStorageBlacklist();
        Set<String> psList = host2PrimaryStorageBlacklist.get(hostUuid);

        if (psList == null) {
            psList = new HashSet<>();
        }

        psList.add(psUuid);
        host2PrimaryStorageBlacklist.put(hostUuid, psList);
    }

    @Override
    public String getPrimaryStorageAllocatorStrategyName(final AllocatePrimaryStorageMsg msg) {
        String allocatorType = null;
        if (msg.getExcludeAllocatorStrategies() != null
                && msg.getExcludeAllocatorStrategies().contains(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY)
                ) {
            allocatorType = null;
        } else if (LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY.equals(msg.getAllocationStrategy())) {
            allocatorType = LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
        } else if (!msg.getCandidatePrimaryStorageUuids().isEmpty()) {
            SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
            q.add(PrimaryStorageVO_.type, Op.EQ, LocalStorageConstants.LOCAL_STORAGE_TYPE);
            q.add(PrimaryStorageVO_.uuid, Op.IN, msg.getCandidatePrimaryStorageUuids());
            if (q.count() == msg.getCandidatePrimaryStorageUuids().size()) {
                allocatorType = LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
            }
        } else if (msg.getRequiredHostUuid() != null) {

            if (msg.getAllocationStrategy() != null && !LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY.equals(msg.getAllocationStrategy())) {
                return null;
            }

            allocatorType = new Callable<String>() {
                @Override
                @Transactional(readOnly = true)
                public String call() {
                    String sql = "select ps.type" +
                            " from PrimaryStorageVO ps, PrimaryStorageClusterRefVO ref, HostVO host" +
                            " where ps.uuid = ref.primaryStorageUuid" +
                            " and ref.clusterUuid = host.clusterUuid" +
                            " and host.uuid = :huuid" +
                            " and ps.state = :state" +
                            " and ps.status = :status";
                    TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                    q.setParameter("huuid", msg.getRequiredHostUuid());
                    q.setParameter("state", PrimaryStorageState.Enabled);
                    q.setParameter("status", PrimaryStorageStatus.Connected);
                    List<String> types = q.getResultList();
                    for (String type : types) {
                        if (type.equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)) {
                            return LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
                        }
                    }
                    return null;
                }
            }.call();
        }

        return allocatorType;
    }

    @Override
    public String getAllocatorStrategy(HostInventory host) {
        if (host != null && !"KVM".equals(host.getHypervisorType())) {
            return null;
        }
        return LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY;
    }

    @Override
    public String getHostAllocatorStrategyName(HostAllocatorSpec spec) {
        if (!VmOperation.Migrate.toString().equals(spec.getVmOperation())) {
            return null;
        }

        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.select(PrimaryStorageVO_.type);
        q.add(PrimaryStorageVO_.uuid, Op.EQ, spec.getVmInstance().getRootVolume().getPrimaryStorageUuid());
        String type = q.findValue();
        if (!LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(type)) {
            return null;
        }

        return LocalStorageConstants.LOCAL_STORAGE_MIGRATE_VM_ALLOCATOR_TYPE;
    }

    @Override
    public String getHostUuidByResourceUuid(String primaryStorageUuid, String resUuid) {
        if (LocalStorageUtils.isLocalStorage(primaryStorageUuid)) {
            return LocalStorageUtils.getHostUuidByResourceUuid(resUuid);
        }
        return null;
    }

    @Override
    public String buildAllocatedInstallUrl(AllocatePrimaryStorageSpaceMsg msg, PrimaryStorageInventory psInv) {
        String hostUuid = null;
        if (msg.getRequiredInstallUri() != null) {
            hostUuid = getHostUuidFromURIScheme(msg.getRequiredInstallUri());
        } else if (msg.getRequiredHostUuid() != null) {
            hostUuid = msg.getRequiredHostUuid();
        } else if (msg.getSystemTags() != null) {
            PatternedSystemTag lsTag = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME;
            hostUuid = msg.getSystemTags().stream().filter(lsTag::isMatch)
                    .map(it -> lsTag.getTokenByTag(it, LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN))
                    .findAny().orElse(null);
        } else if (msg.getImageUuid() != null) {
            String url = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).select(ImageVO_.url).findValue();
            if (url.startsWith(SNAPSHOT_REUSE_IMAGE_SCHEMA)) {
                hostUuid = getHostUuidFromURIScheme(url);
            }
        }

        if (hostUuid == null) {
            throw new OperationFailureException(argerr("To create volume on the local primary storage, " +
                            "you must specify the host that the volume is going to be created using the system tag [%s]",
                    LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.getTagFormat()));
        }

        LocalStorageUtils.InstallPath path = new LocalStorageUtils.InstallPath();
        path.installPath = psInv.getUrl();
        path.hostUuid = hostUuid;

        return path.makeFullPath();
    }

    public static String getHostUuidFromURIScheme(String uri) {
        String protocol;
        try {
            protocol = new URI(uri).getScheme();
        } catch (URISyntaxException e) {
            throw new OperationFailureException(argerr("invalid uri, " +
                    "correct example is file://$URL;hostUuid://$HOSTUUID or volume://$VOLUMEUUID" +
                    " or volumeSnapshotReuse://$SNAPSHOTUUID"));
        }
        return uriParsers.get(protocol).parseUri(uri).hostUuid;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long reserveCapacity(AllocatePrimaryStorageSpaceMsg msg, String allocatedInstallUrl, long size, String psUuid) {
        LocalStorageUtils.InstallPath path = new LocalStorageUtils.InstallPath();
        path.fullPath = allocatedInstallUrl;
        String hostUuid = path.disassemble().hostUuid;
        PrimaryStorageVO primaryStorageVO = dbf.findByUuid(psUuid, PrimaryStorageVO.class);

        long hostCapacityBeforeAllocate = Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, hostUuid)
                .eq(LocalStorageHostRefVO_.primaryStorageUuid, psUuid)
                .select(LocalStorageHostRefVO_.availableCapacity)
                .findValue();

        new LocalStorageUtils().reserveCapacityOnHost(hostUuid, size, psUuid, primaryStorageVO, msg.isForce());
        return hostCapacityBeforeAllocate;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseCapacity(String allocatedInstallUrl, long size, String psUuid) {
        LocalStorageUtils.InstallPath path = new LocalStorageUtils.InstallPath();
        path.fullPath = allocatedInstallUrl;
        String hostUuid = path.disassemble().hostUuid;
        PrimaryStorageVO primaryStorageVO = dbf.findByUuid(psUuid, PrimaryStorageVO.class);
        new LocalStorageUtils().returnStorageCapacityToHost(hostUuid, size, primaryStorageVO);
    }

    private static void parseRequiredInstallUri() {
        String protocolVolume = "volume";
        String protocolFile = "file";
        String protocolSnapshotReuse = "volumeSnapshotReuse";

        AbstractUriParser volumeParser = new AbstractUriParser() {
            @Override
            LocalStorageUtils.InstallPath parseUri(String uri) {
                String volumeUuid = uri.replaceFirst("volume://", "");
                String path = Q.New(VolumeVO.class).select(VolumeVO_.installPath).eq(VolumeVO_.uuid, volumeUuid).findValue();
                String hostUuid = getHostUuidFromLocalStorageResourceRefVO(volumeUuid, VolumeVO.class.getSimpleName());
                LocalStorageUtils.InstallPath p = new LocalStorageUtils.InstallPath();
                p.hostUuid = hostUuid;
                p.installPath = path;
                return p;
            }
        };

        AbstractUriParser fileParser = new AbstractUriParser() {
            @Override
            LocalStorageUtils.InstallPath parseUri(String uri) {
                LocalStorageUtils.InstallPath p = new LocalStorageUtils.InstallPath();
                p.fullPath = uri;
                p.disassemble();
                return p;
            }
        };

        AbstractUriParser snapshotReuseParser = new AbstractUriParser() {
            @Override
            LocalStorageUtils.InstallPath parseUri(String uri) {
                String snapshotUuid = uri.replaceFirst(SNAPSHOT_REUSE_IMAGE_SCHEMA, "");
                String path = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshotUuid)
                        .select(VolumeSnapshotVO_.primaryStorageInstallPath).findValue();
                String hostUuid = getHostUuidFromLocalStorageResourceRefVO(snapshotUuid, VolumeSnapshotVO.class.getSimpleName());
                LocalStorageUtils.InstallPath p = new LocalStorageUtils.InstallPath();
                p.hostUuid = hostUuid;
                p.installPath = path;
                return p;
            }
        };

        uriParsers.put(protocolVolume, volumeParser);
        uriParsers.put(protocolFile, fileParser);
        uriParsers.put(protocolSnapshotReuse, snapshotReuseParser);
    }

    private static String getHostUuidFromLocalStorageResourceRefVO(String resourceUuid, String resourceType) {
        return Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, resourceUuid)
                .eq(LocalStorageResourceRefVO_.resourceType, resourceType)
                .select(LocalStorageResourceRefVO_.hostUuid)
                .findValue();
    }

    abstract static class AbstractUriParser {
        abstract LocalStorageUtils.InstallPath parseUri(String uri);
    }

    @Override
    public PrimaryStorageType getPrimaryStorageType() {
        return LocalStorageFactory.type;
    }
}
