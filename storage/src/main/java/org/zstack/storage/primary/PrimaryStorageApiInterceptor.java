package org.zstack.storage.primary;

import java.util.Arrays;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.addon.primary.PrimaryStorageOutputProtocolRefVO;
import org.zstack.header.storage.addon.primary.PrimaryStorageOutputProtocolRefVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.APICreateVolumeSnapshotGroupMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class PrimaryStorageApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRegistry;
    @Autowired
    private PrimaryStorageManagerImpl primaryStorageManager;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof PrimaryStorageMessage) {
            PrimaryStorageMessage pmsg = (PrimaryStorageMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, pmsg.getPrimaryStorageUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeletePrimaryStorageMsg) {
            validate((APIDeletePrimaryStorageMsg) msg);
        } else if (msg instanceof APIAttachPrimaryStorageToClusterMsg) {
            validate((APIAttachPrimaryStorageToClusterMsg) msg);
        } else if (msg instanceof APIDetachPrimaryStorageFromClusterMsg) {
            validate((APIDetachPrimaryStorageFromClusterMsg) msg);
        } else if (msg instanceof APIGetPrimaryStorageCapacityMsg) {
            validate((APIGetPrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof APIGetTrashOnPrimaryStorageMsg) {
            validate(((APIGetTrashOnPrimaryStorageMsg) msg));
        } else if (msg instanceof APICreateVolumeSnapshotGroupMsg) {
            validate((APICreateVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APIAddStorageProtocolMsg) {
            validate((APIAddStorageProtocolMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APIAddStorageProtocolMsg msg) {
        if (Q.New(PrimaryStorageOutputProtocolRefVO.class)
                .eq(PrimaryStorageOutputProtocolRefVO_.primaryStorageUuid, msg.getUuid())
                .eq(PrimaryStorageOutputProtocolRefVO_.outputProtocol, msg.getOutputProtocol())
                .isExists()) {
            throw new ApiMessageInterceptionException(argerr("outputProtocol[%s] is exist on primary storage[%s]" +
                    "no need to add again", msg.getOutputProtocol(), msg.getPrimaryStorageUuid()));
        }
        String type = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid()).select(PrimaryStorageVO_.type).find();
        PrimaryStorageFactory factory = primaryStorageManager.getPrimaryStorageFactory(type);
        if (factory == null) {
            throw new ApiMessageInterceptionException(argerr("unknown primary storage type[%s]", type));
        }
        factory.validateStorageProtocol(msg.getOutputProtocol());
    }

    private void validate(APIGetPrimaryStorageCapacityMsg msg) {
        boolean pass = false;
        if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
            pass = true;
        }
        if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
            pass = true;
        }
        if (msg.getPrimaryStorageUuids() != null && !msg.getPrimaryStorageUuids().isEmpty()) {
            pass = true;
        }

        if (!pass && !msg.isAll()) {
            throw new ApiMessageInterceptionException(argerr("zoneUuids, clusterUuids, primaryStorageUuids must have at least one be none-empty list, or all is set to true"));
        }

        if (msg.isAll() && (msg.getZoneUuids() == null || msg.getZoneUuids().isEmpty())) {
            List<String> zuuids = Q.New(ZoneVO.class)
                    .select(ZoneVO_.uuid)
                    .listValues();
            msg.setZoneUuids(zuuids);

            if (msg.getZoneUuids().isEmpty()) {
                APIGetPrimaryStorageCapacityReply reply = new APIGetPrimaryStorageCapacityReply();
                bus.reply(msg, reply);
                throw new StopRoutingException();
            }
        }
    }

    private void validate(APIDetachPrimaryStorageFromClusterMsg msg) {
        if (!Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(argerr("primary storage[uuid:%s] has not been attached to cluster[uuid:%s] yet",
                            msg.getPrimaryStorageUuid(), msg.getClusterUuid()));
        }
    }

    @Transactional
    private void validate(APIAttachPrimaryStorageToClusterMsg msg) {
        {
            String sql = "select count(ref)" +
                    " from PrimaryStorageClusterRefVO ref" +
                    " where ref.clusterUuid = :clusterUuid" +
                    " and ref.primaryStorageUuid = :psUuid";
            TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
            q.setParameter("psUuid", msg.getPrimaryStorageUuid());
            q.setParameter("clusterUuid", msg.getClusterUuid());
            long count = q.getSingleResult();
            if (count != 0) {
                throw new ApiMessageInterceptionException(operr("primary storage[uuid:%s] has been attached to cluster[uuid:%s]",
                                msg.getPrimaryStorageUuid(), msg.getClusterUuid()));
            }
        }
        {
            String sql = "select count(ps)" +
                    " from PrimaryStorageVO ps, ClusterVO cluster" +
                    " where cluster.zoneUuid = ps.zoneUuid" +
                    " and cluster.uuid = :clusterUuid" +
                    " and ps.uuid = :psUuid";
            TypedQuery<Long> jq = dbf.getEntityManager().createQuery(sql, Long.class);
            jq.setParameter("psUuid", msg.getPrimaryStorageUuid());
            jq.setParameter("clusterUuid", msg.getClusterUuid());
            long count = jq.getSingleResult();
            if (count == 0) {
                throw new ApiMessageInterceptionException(argerr("primary storage[uuid:%s] and cluster[uuid:%s] are not in the same zone",
                                msg.getPrimaryStorageUuid(), msg.getClusterUuid()));
            }
        }
        {
            String url = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.url)
                    .eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid())
                    .findValue();
            long count = SQL.New("select count(ps)" +
                    " from PrimaryStorageVO ps" +
                    " where ps.uuid in" +
                    " (" +
                    " select ref.primaryStorageUuid from PrimaryStorageClusterRefVO ref" +
                    " where ref.clusterUuid = :clusterUuid" +
                    " )" +
                    " and ps.url = :url", Long.class)
                    .param("clusterUuid", msg.getClusterUuid())
                    .param("url", url)
                    .find();

            if(count > 0){
                throw new ApiMessageInterceptionException(
                        argerr("url[%s] has been occupied, it cannot be duplicate in same cluster",
                        url));
            }
        }

        String clusterType = Q.New(ClusterVO.class)
                .eq(ClusterVO_.uuid, msg.getClusterUuid())
                .select(ClusterVO_.hypervisorType)
                .findValue();

        String primaryStorageType = Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, msg.getPrimaryStorageUuid())
                .select(PrimaryStorageVO_.type)
                .findValue();

        List<StorageAttachClusterMetric> storageAttachClusterMetrics =
                new ArrayList<>(pluginRegistry.getExtensionList(StorageAttachClusterMetric.class));

        storageAttachClusterMetrics.stream().filter(m ->
                m.getClusterHypervisorType().toString().equals(clusterType) && m.getPrimaryStorageType().toString().equals(primaryStorageType)
        ).forEach(m ->
                m.checkSupport(msg.getPrimaryStorageUuid(), msg.getClusterUuid())
        );
    }

    private void validate(APIDeletePrimaryStorageMsg msg) {
        if (!dbf.isExist(msg.getUuid(), PrimaryStorageVO.class)) {
            APIDeletePrimaryStorageEvent evt = new APIDeletePrimaryStorageEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(final APIGetTrashOnPrimaryStorageMsg msg) {
        if ((msg.getResourceType() != null) ^ (msg.getResourceUuid() != null)) {
            throw new ApiMessageInterceptionException((argerr("'resourceUuid' and 'resourceType' must be set both or neither!")));
        }
    }

    private void validate(final APICreateVolumeSnapshotGroupMsg msg) {
        Set<String> psUuids = msg.getVmInstance().getAllVolumes().stream()
                .map(VolumeInventory::getPrimaryStorageUuid)
                .collect(Collectors.toSet());

        List<String> allowedPsUuids = Q.New(PrimaryStorageVO.class).in(PrimaryStorageVO_.uuid, psUuids)
                .eq(PrimaryStorageVO_.status, PrimaryStorageStatus.Connected)
                .eq(PrimaryStorageVO_.state, PrimaryStorageState.Enabled)
                .select(PrimaryStorageVO_.uuid)
                .listValues();

        psUuids.removeAll(allowedPsUuids);
        if (!psUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("primary storage(s) [uuid: %s] where volume(s) locate" +
                    " is not Enabled or Connected", psUuids));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APICreateVolumeSnapshotGroupMsg.class,
            APIAddPrimaryStorageMsg.class,
            APIChangePrimaryStorageStateMsg.class,
            APIDeletePrimaryStorageMsg.class,
            APIDetachPrimaryStorageFromClusterMsg.class,
            APIAttachPrimaryStorageToClusterMsg.class,
            APIGetPrimaryStorageTypesMsg.class,
            APIGetPrimaryStorageAllocatorStrategiesMsg.class,
            APIGetPrimaryStorageCapacityMsg.class,
            APIReconnectPrimaryStorageMsg.class,
            APISyncPrimaryStorageCapacityMsg.class,
            APIUpdatePrimaryStorageMsg.class,
            APICleanUpImageCacheOnPrimaryStorageMsg.class,
            APIQueryImageCacheMsg.class,
            APICleanUpTrashOnPrimaryStorageMsg.class,
            APIGetTrashOnPrimaryStorageMsg.class,
            APIGetPrimaryStorageLicenseInfoMsg.class,
            APIGetPrimaryStorageUsageReportMsg.class,
            APICleanUpStorageTrashOnPrimaryStorageMsg.class,
            APIAddStorageProtocolMsg.class,
            APITakeoverPrimaryStorageMsg.class,
            APIDiscoverStrangePrimaryStorageMsg.class,
            APIScanVmInstanceMetadataFromPrimaryStorageMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.DEFAULT;
    }

}
