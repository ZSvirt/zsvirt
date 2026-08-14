package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.DiskOfferingTagAllocatorExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.allocator.InstanceOfferingTagAllocatorExtensionPoint;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.PrimaryStorageTagAllocatorExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.TagInventory;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vo.ResourceVO;
import org.zstack.utils.CollectionUtils;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.i18m;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class PrimaryStorageTagAllocatorExtension implements InstanceOfferingTagAllocatorExtensionPoint,
        PrimaryStorageTagAllocatorExtensionPoint, DiskOfferingTagAllocatorExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Transactional(readOnly = true)
    private void uuidTagAllocateHost(List<HostCandidate> candidates, String psUuid) {
        List<String> hostUuids = CollectionUtils.transform(candidates, HostCandidate::getUuid);

        String sql = "select h.uuid from HostVO h where h.clusterUuid in (select ref.clusterUuid from PrimaryStorageClusterRefVO ref where ref.primaryStorageUuid = :psUuid) and h.uuid in (:huuids)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("psUuid", psUuid);
        q.setParameter("huuids", hostUuids);
        List<String> results = q.getResultList();

        for (HostCandidate candidate : candidates) {
            if (!results.contains(candidate.getUuid())) {
                candidate.markAsRejected(
                        getClass().getSimpleName(),
                        i18m("not attach to primary storage[uuid:%s] the instance offering tag specified", psUuid));
            }
        }
    }

    @Override
    public void allocateHost(List<TagInventory> tags, List<HostCandidate> candidates, HostAllocatorSpec spec) {
        if (!VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            return;
        }

        for (TagInventory tag : tags) {
            String uuid = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.getTokenByTag(tag.getTag(), "uuid");
            if (uuid != null) {
                uuidTagAllocateHost(candidates, uuid);
                return;
            }

            String requiredUserTag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG_MANDATORY.getTokenByTag(tag.getTag(), "tag");
            if (requiredUserTag != null) {
                userTagAllocateHost(candidates, requiredUserTag, true);
                return;
            }

            String userTag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG.getTokenByTag(tag.getTag(), "tag");
            if (userTag != null) {
                userTagAllocateHost(candidates, userTag, false);
                return;
            }
        }
    }

    @Transactional(readOnly = true)
    private void userTagAllocateHost(List<HostCandidate> candidates, String tag, boolean required) {
        List<String> hostUuids = CollectionUtils.transform(candidates, HostCandidate::getUuid);

        String sql = "select h.uuid from HostVO h where h.clusterUuid in (select ref.clusterUuid from PrimaryStorageClusterRefVO ref where ref.primaryStorageUuid in (select t.resourceUuid from UserTagVO t where t.tag = :tag and t.resourceType = :resourceType)) and h.uuid in (:huuids)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("tag", tag);
        q.setParameter("resourceType", PrimaryStorageVO.class.getSimpleName());
        q.setParameter("huuids", hostUuids);
        List<String> vos = q.getResultList();

        if (vos.isEmpty() && !required) {
            return;
        }

        for (HostCandidate candidate : candidates) {
            if (!vos.contains(candidate.getUuid())) {
                candidate.markAsRejected(
                        getClass().getSimpleName(),
                        i18m("not attach to primary storage the user tag[%s] specified", tag));
            }
        }
    }

    @Override
    public List<PrimaryStorageVO> allocatePrimaryStorage(List<SystemTagInventory> tags, List<PrimaryStorageVO> candidates) {
        for (SystemTagInventory tag : tags) {
            final String uuid = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.getTokenByTag(tag.getTag(), "uuid");
            if (uuid != null) {
                PrimaryStorageVO pvo = CollectionUtils.findOneOrNull(candidates, arg -> uuid.equals(arg.getUuid()));

                if (pvo == null) {
                    throw new OperationFailureException(operr("cannot find primary storage[uuid:%s], the uuid is specified in instance offering or disk offering", uuid));
                }

                List<PrimaryStorageVO> psvos = new ArrayList<PrimaryStorageVO>();
                psvos.add(pvo);
                return psvos;
            }

            String requiredUserTag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG_MANDATORY.getTokenByTag(tag.getTag(), "tag");
            if (requiredUserTag != null) {
                return allocatePrimaryStorageByUserTag(requiredUserTag, candidates, true);
            }

            String userTag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG.getTokenByTag(tag.getTag(), "tag");
            if (userTag != null) {
                return allocatePrimaryStorageByUserTag(userTag, candidates, false);
            }
        }

        return candidates;
    }


    @Transactional(readOnly = true)
    private List<PrimaryStorageVO> allocatePrimaryStorageByUserTag(String tag, List<PrimaryStorageVO> candidates, boolean required) {
        List<String> uuids = transformAndRemoveNull(candidates, ResourceVO::getUuid);

        String sql = "select ps from PrimaryStorageVO ps where ps.uuid in (:uuids) and ps.uuid in (select t.resourceUuid from UserTagVO t where t.tag = :tag and t.resourceType = :resourceType)";
        TypedQuery<PrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
        q.setParameter("uuids", uuids);
        q.setParameter("tag", tag);
        q.setParameter("resourceType", PrimaryStorageVO.class.getSimpleName());
        List<PrimaryStorageVO> vos = q.getResultList();

        if (vos.isEmpty() && required) {
            throw new OperationFailureException(operr("cannot find primary storage having user tag[%s]. The user tag is specified in instance offering or disk offering", tag));
        } else if (vos.isEmpty()) {
            return candidates;
        } else {
            return vos;
        }
    }
}
