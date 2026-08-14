package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.identity.ResourceHelper;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by miao on 16-10-9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmQuotaUtil {
    @Autowired
    private DatabaseFacade dbf;

    public class VmQuota {
        public long totalVmNum;
        public long runningVmNum;
        public long runningVmCpuNum;
        public long runningVmMemorySize;
    }

    @Transactional(readOnly = true)
    public long getUsedDataVolumeCount(String accountUuid) {
        return ResourceHelper.countOwnResources(VolumeVO.class, accountUuid,
                q -> q.eq(VolumeVO_.type, VolumeType.Data).notEq(VolumeVO_.status, VolumeStatus.Deleted));
    }

    @Transactional(readOnly = true)
    public long getUsedAllVolumeSize(String accountUuid) {
        final List<VolumeVO> volumes = ResourceHelper.findOwnResources(VolumeVO.class, accountUuid);
        long size = 0L;
        for (VolumeVO volume : volumes) {
            size += volume.getSize();
        }
        return size;
    }

    @Transactional(readOnly = true)
    public VmQuota getUsedVmCpuMemory(String accountUUid, String excludeVmUuid) {
        VmQuota quota = new VmQuota();
        // get running info
        String sql = "select count(vm), sum(vm.cpuNum), sum(vm.memorySize)" +
                " from VmInstanceVO vm, AccountResourceRefVO ref" +
                " where vm.uuid = ref.resourceUuid" +
                " and ref.accountUuid = :auuid" +
                " and ref.resourceType = :rtype" +
                " and ref.type = :type" +
                " and not (vm.state = :starting and vm.hostUuid is null)" +
                " and vm.state not in (:states)" +
                " and vm.type != :vmtype";
        if (excludeVmUuid != null) {
            sql += " and vm.uuid != (:excludeVmUuid)";
        }

        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("auuid", accountUUid);
        q.setParameter("rtype", VmInstanceVO.class.getSimpleName());
        q.setParameter("type", AccessLevel.Own);
        q.setParameter("starting", VmInstanceState.Starting);
        q.setParameter("states", list(VmInstanceState.Stopped, VmInstanceState.Destroying,
                VmInstanceState.Destroyed, VmInstanceState.Created));
        q.setParameter("vmtype", "baremetal2");

        if (excludeVmUuid != null) {
            q.setParameter("excludeVmUuid", excludeVmUuid);
        }

        Tuple t = q.getSingleResult();
        Long vnum = t.get(0, Long.class);
        quota.runningVmNum = vnum == null ? 0 : vnum;
        Long cnum = t.get(1, Long.class);
        quota.runningVmCpuNum = cnum == null ? 0 : cnum;
        Long msize = t.get(2, Long.class);
        quota.runningVmMemorySize = msize == null ? 0 : msize;
        // get total vm
        String sql2 = "select count(vm)" +
                " from VmInstanceVO vm, AccountResourceRefVO ref" +
                " where vm.uuid = ref.resourceUuid" +
                " and ref.accountUuid = :auuid" +
                " and ref.resourceType = :rtype" +
                " and ref.type = :type" +
                // for vm without host and volume info, treat them as new creating vm
                " and not (vm.hostUuid is null and vm.lastHostUuid is null and vm.rootVolumeUuid is null)" +
                " and vm.state not in (:states)" +
                " and vm.type != :vmtype";
        TypedQuery<Long> q2 = dbf.getEntityManager().createQuery(sql2, Long.class);
        q2.setParameter("auuid", accountUUid);
        q2.setParameter("rtype", VmInstanceVO.class.getSimpleName());
        q2.setParameter("type", AccessLevel.Own);
        q2.setParameter("states", list(VmInstanceState.Destroyed));
        q2.setParameter("vmtype", "baremetal2");
        Long totalVmNum = q2.getSingleResult();
        quota.totalVmNum = totalVmNum == null ? 0 : totalVmNum;
        return quota;
    }

    @Transactional(readOnly = true)
    public VmQuota getUsedVmCpuMemory(String accountUUid) {
        return getUsedVmCpuMemory(accountUUid, null);
    }

    @Transactional(readOnly = true)
    public long getVmInstanceRootVolumeSize(String vmInstanceUuid) {
        SimpleQuery<VolumeVO> sq = dbf.createQuery(VolumeVO.class);
        sq.select(VolumeVO_.size);
        sq.add(VolumeVO_.type, SimpleQuery.Op.EQ, VolumeType.Root);
        sq.add(VolumeVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vmInstanceUuid);
        Long rootVolumeSize = sq.findValue();
        rootVolumeSize = rootVolumeSize == null ? 0 : rootVolumeSize;
        return rootVolumeSize;
    }

    @Transactional(readOnly = true)
    public Long getRequiredCpu(String vmInstanceUuid) {
        Integer cpuNum = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.cpuNum)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .findValue();

        return Integer.toUnsignedLong(cpuNum);
    }

    @Transactional(readOnly = true)
    public Long getRequiredMemory(String vmInstanceUuid) {
        return Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.memorySize)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .findValue();
    }
}
