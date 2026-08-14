package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.identity.APIChangeResourceOwnerMsg;
import org.zstack.header.identity.AccountType;
import org.zstack.header.identity.Quota;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.vm.*;
import org.zstack.header.volume.*;
import org.zstack.identity.QuotaUtil;
import org.zstack.utils.data.Pair;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmQuotaOperator implements Quota.QuotaOperator {
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    ErrorFacade errf;

    @Override
    public void checkQuota(APIMessage msg, Map<String, Quota.QuotaPair> pairs) {
        AccountType type = new QuotaUtil().getAccountType(msg.getSession().getAccountUuid());

        if (type != AccountType.SystemAdmin) {
            if (msg instanceof APICreateVmInstanceMsg) {
                if (((APICreateVmInstanceMsg) msg).getStrategy().
                        equals(VmCreationStrategy.JustCreate.toString())) {
                    return;
                }
                check((APICreateVmInstanceMsg) msg, pairs);
            } else if (msg instanceof APICreateDataVolumeMsg) {
                check((APICreateDataVolumeMsg) msg, pairs);
            } else if (msg instanceof APIRecoverDataVolumeMsg) {
                check((APIRecoverDataVolumeMsg) msg, pairs);
            } else if (msg instanceof APIStartVmInstanceMsg) {
                check((APIStartVmInstanceMsg) msg, pairs);
            } else if (msg instanceof APIChangeResourceOwnerMsg) {
                check((APIChangeResourceOwnerMsg) msg, pairs);
            } else if (msg instanceof APIRecoverVmInstanceMsg) {
                check((APIRecoverVmInstanceMsg) msg, pairs);
            }
        } else {
            if (msg instanceof APIChangeResourceOwnerMsg) {
                check((APIChangeResourceOwnerMsg) msg, pairs);
            }
        }
    }

    @Override
    public void checkQuota(NeedQuotaCheckMessage msg, Map<String, Quota.QuotaPair> pairs) {
        if (!new QuotaUtil().isAdminAccount(msg.getAccountUuid())) {
            if (msg instanceof StartVmInstanceMsg) {
                check((StartVmInstanceMsg) msg, pairs);
            }
        }
    }

    @Override
    public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
        List<Quota.QuotaUsage> usages = new ArrayList<>();

        VmQuotaUtil.VmQuota vmQuota = new VmQuotaUtil().getUsedVmCpuMemory(accountUuid);
        Quota.QuotaUsage usage;

        usage = new Quota.QuotaUsage();
        usage.setName(VmQuotaConstant.VM_TOTAL_NUM);
        usage.setUsed(vmQuota.totalVmNum);
        usages.add(usage);

        usage = new Quota.QuotaUsage();
        usage.setName(VmQuotaConstant.VM_RUNNING_NUM);
        usage.setUsed(vmQuota.runningVmNum);
        usages.add(usage);

        usage = new Quota.QuotaUsage();
        usage.setName(VmQuotaConstant.VM_RUNNING_CPU_NUM);
        usage.setUsed(vmQuota.runningVmCpuNum);
        usages.add(usage);

        usage = new Quota.QuotaUsage();
        usage.setName(VmQuotaConstant.VM_RUNNING_MEMORY_SIZE);
        usage.setUsed(vmQuota.runningVmMemorySize);
        usages.add(usage);

        usage = new Quota.QuotaUsage();
        usage.setName(VmQuotaConstant.DATA_VOLUME_NUM);
        usage.setUsed(new VmQuotaUtil().getUsedDataVolumeCount(accountUuid));
        usages.add(usage);

        usage = new Quota.QuotaUsage();
        usage.setName(VmQuotaConstant.VOLUME_SIZE);
        usage.setUsed(new VmQuotaUtil().getUsedAllVolumeSize(accountUuid));
        usages.add(usage);

        return usages;
    }

    private void check(APIStartVmInstanceMsg msg, Map<String, Quota.QuotaPair> pairs) {
        checkStartVmInstance(msg.getSession().getAccountUuid(), msg.getVmInstanceUuid(), pairs);
    }

    private void check(StartVmInstanceMsg msg, Map<String, Quota.QuotaPair> pairs) {
        checkStartVmInstance(msg.getAccountUuid(), msg.getVmInstanceUuid(), pairs);
    }

    private void checkStartVmInstance(String currentAccountUuid,
                                      String vmInstanceUuid,
                                      Map<String, Quota.QuotaPair> pairs) {
        String resourceTargetOwnerAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(vmInstanceUuid);
        checkVmInstanceQuota(currentAccountUuid, resourceTargetOwnerAccountUuid, vmInstanceUuid, pairs);
    }

    // It is a bad hack here for checking VmQuotaGlobalConfig.VM_TOTAL_NUM
    //
    // 'checkVmInstanceQuota' is now (temporary) a public method, but it has
    // no context of what happened:
    //  1. Are we creating new VM?
    //  2. Are we recovering a deleted VM?
    //  3. Are we starting an existing VM?
    //
    // We must differentiate creating new resources and operating on old resources
    // when handling the quota for VM_TOTAL_NUM.
    private void checkTotalVMQuota(String currentAccountUuid,
                                   String resourceTargetOwnerAccountUuid,
                                   String vmInstanceUuid,
                                   long totalVmNumQuota,
                                   long totalVmNum) {
        if (Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .notNull(VmInstanceVO_.lastHostUuid)
                .isExists()) {
            // Dirty hack - VM with last host UUID means existing VM.
            return;
        }

        QuotaUtil.QuotaCompareInfo quotaCompareInfo;
        quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
        quotaCompareInfo.currentAccountUuid = currentAccountUuid;
        quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
        quotaCompareInfo.quotaName = VmQuotaConstant.VM_TOTAL_NUM;
        quotaCompareInfo.quotaValue = totalVmNumQuota;
        quotaCompareInfo.currentUsed = totalVmNum;
        quotaCompareInfo.request = 1;
        new QuotaUtil().CheckQuota(quotaCompareInfo);
    }

    @Transactional(readOnly = true)
    public void checkVmInstanceQuota(String currentAccountUuid,
                                      String resourceTargetOwnerAccountUuid,
                                      String vmInstanceUuid,
                                      Map<String, Quota.QuotaPair> pairs) {
        long vmNumQuota = pairs.get(VmQuotaConstant.VM_RUNNING_NUM).getValue();

        VmQuotaUtil.VmQuota vmQuotaUsed = new VmQuotaUtil().getUsedVmCpuMemory(resourceTargetOwnerAccountUuid, null);
        //
        {
            QuotaUtil.QuotaCompareInfo quotaCompareInfo;
            quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
            quotaCompareInfo.currentAccountUuid = currentAccountUuid;
            quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
            quotaCompareInfo.quotaName = VmQuotaConstant.VM_RUNNING_NUM;
            quotaCompareInfo.quotaValue = vmNumQuota;
            quotaCompareInfo.currentUsed = vmQuotaUsed.runningVmNum;
            quotaCompareInfo.request = 1;
            new QuotaUtil().CheckQuota(quotaCompareInfo);
        }
        //
        checkTotalVMQuota(currentAccountUuid,
                resourceTargetOwnerAccountUuid,
                vmInstanceUuid,
                pairs.get(VmQuotaConstant.VM_TOTAL_NUM).getValue(),
                vmQuotaUsed.totalVmNum);
        //
        VmInstanceVO vm = dbf.getEntityManager().find(VmInstanceVO.class, vmInstanceUuid);

        checkVmCupAndMemoryCapacity(currentAccountUuid, resourceTargetOwnerAccountUuid, vm.getCpuNum(), vm.getMemorySize(), pairs);
    }

    @Transactional(readOnly = true)
    public void checkVmCupAndMemoryCapacity(String currentAccountUuid, String resourceTargetOwnerAccountUuid, long cpu, long memory, Map<String, Quota.QuotaPair> pairs) {
        VmQuotaUtil.VmQuota vmQuotaUsed = new VmQuotaUtil().getUsedVmCpuMemory(resourceTargetOwnerAccountUuid);
        long cpuNumQuota = pairs.get(VmQuotaConstant.VM_RUNNING_CPU_NUM).getValue();
        long memoryQuota = pairs.get(VmQuotaConstant.VM_RUNNING_MEMORY_SIZE).getValue();

        {
            QuotaUtil.QuotaCompareInfo quotaCompareInfo;
            quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
            quotaCompareInfo.currentAccountUuid = currentAccountUuid;
            quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
            quotaCompareInfo.quotaName = VmQuotaConstant.VM_RUNNING_CPU_NUM;
            quotaCompareInfo.quotaValue = cpuNumQuota;
            quotaCompareInfo.currentUsed = vmQuotaUsed.runningVmCpuNum;
            quotaCompareInfo.request = cpu;
            new QuotaUtil().CheckQuota(quotaCompareInfo);
        }
        {
            QuotaUtil.QuotaCompareInfo quotaCompareInfo;
            quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
            quotaCompareInfo.currentAccountUuid = currentAccountUuid;
            quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
            quotaCompareInfo.quotaName = VmQuotaConstant.VM_RUNNING_MEMORY_SIZE;
            quotaCompareInfo.quotaValue = memoryQuota;
            quotaCompareInfo.currentUsed = vmQuotaUsed.runningVmMemorySize;
            quotaCompareInfo.request = memory;
            new QuotaUtil().CheckQuota(quotaCompareInfo);
        }
    }

    private void checkVolumeQuotaForChangeResourceOwner(List<String> dataVolumeUuids,
                                                        List<String> rootVolumeUuids,
                                                        String resourceTargetOwnerAccountUuid,
                                                        String currentAccountUuid,
                                                        Map<String, Quota.QuotaPair> pairs) {
        long dataVolumeNumQuota = pairs.get(VmQuotaConstant.DATA_VOLUME_NUM).getValue();
        long allVolumeSizeQuota = pairs.get(VmQuotaConstant.VOLUME_SIZE).getValue();

        ArrayList<String> volumeUuids = new ArrayList<>();
        if (dataVolumeUuids != null && !dataVolumeUuids.isEmpty()) {
            volumeUuids.addAll(dataVolumeUuids);
        }
        if (rootVolumeUuids != null && !rootVolumeUuids.isEmpty()) {
            volumeUuids.addAll(rootVolumeUuids);
        }

        // skip empty volume uuid list
        if (volumeUuids.isEmpty()) {
            return;
        }
        // check data volume num
        long dataVolumeNumUsed = new VmQuotaUtil().getUsedDataVolumeCount(resourceTargetOwnerAccountUuid);
        if (dataVolumeUuids != null && !dataVolumeUuids.isEmpty()) {
            long dataVolumeNumAsked = dataVolumeUuids.size();
            {
                QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                quotaCompareInfo.quotaName = VmQuotaConstant.DATA_VOLUME_NUM;
                quotaCompareInfo.quotaValue = dataVolumeNumQuota;
                quotaCompareInfo.currentUsed = dataVolumeNumUsed;
                quotaCompareInfo.request = dataVolumeNumAsked;
                new QuotaUtil().CheckQuota(quotaCompareInfo);
            }
        }

        // check data volume size
        long allVolumeSizeAsked;
        String sql = "select sum(size) from VolumeVO where uuid in (:uuids) ";
        TypedQuery<Long> dq = dbf.getEntityManager().createQuery(sql, Long.class);
        dq.setParameter("uuids", volumeUuids);
        Long dsize = dq.getSingleResult();
        dsize = dsize == null ? 0 : dsize;
        allVolumeSizeAsked = dsize;

        long allVolumeSizeUsed = new VmQuotaUtil().getUsedAllVolumeSize(resourceTargetOwnerAccountUuid);
        {
            QuotaUtil.QuotaCompareInfo quotaCompareInfo;
            quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
            quotaCompareInfo.currentAccountUuid = currentAccountUuid;
            quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
            quotaCompareInfo.quotaName = VmQuotaConstant.VOLUME_SIZE;
            quotaCompareInfo.quotaValue = allVolumeSizeQuota;
            quotaCompareInfo.currentUsed = allVolumeSizeUsed;
            quotaCompareInfo.request = allVolumeSizeAsked;
            new QuotaUtil().CheckQuota(quotaCompareInfo);
        }
    }


    private void checkRunningVMQuotaForChangeResourceOwner(String vmInstanceUuid,
                                                           String resourceTargetOwnerAccountUuid,
                                                           String currentAccountUuid,
                                                           Map<String, Quota.QuotaPair> pairs) {
        checkVmInstanceQuota(currentAccountUuid, resourceTargetOwnerAccountUuid, vmInstanceUuid, pairs);
    }

    @Transactional(readOnly = true)
    private void check(APIChangeResourceOwnerMsg msg, Map<String, Quota.QuotaPair> pairs) {
        String currentAccountUuid = msg.getSession().getAccountUuid();
        String resourceTargetOwnerAccountUuid = msg.getAccountUuid();
        if (new QuotaUtil().isAdminAccount(resourceTargetOwnerAccountUuid)) {
            return;
        }

        String resourceType = new QuotaUtil().getResourceType(msg.getResourceUuid());
        if (resourceType.equals(VolumeVO.class.getSimpleName())) {
            String volumeUuid = msg.getResourceUuid();
            ArrayList<String> volumeUuids = new ArrayList<>();
            volumeUuids.add(volumeUuid);
            checkVolumeQuotaForChangeResourceOwner(volumeUuids, null,
                    resourceTargetOwnerAccountUuid, currentAccountUuid, pairs);

        } else if (resourceType.equals(VmInstanceVO.class.getSimpleName())) {
            VmInstanceVO vmInstanceVO = dbf.findByUuid(msg.getResourceUuid(), VmInstanceVO.class);

            // filter vm state
            if (vmInstanceVO.getState().equals(VmInstanceState.Created)) {
                return;
            } else if (!vmInstanceVO.getState().equals(VmInstanceState.Stopped)
                    && !vmInstanceVO.getState().equals(VmInstanceState.Running)
                    && !vmInstanceVO.getState().equals(VmInstanceState.Paused)
                    && !vmInstanceVO.getState().equals(VmInstanceState.Starting)) {
                throw new ApiMessageInterceptionException(err(VmErrors.NOT_IN_CORRECT_STATE,
                        "Incorrect VM State.VM[uuid:%s] current state:%s. ",
                        msg.getResourceUuid(), vmInstanceVO.getState()
                ));
            }

            String vmInstanceUuid = msg.getResourceUuid();

            // check vm
            if (vmInstanceVO.getState().equals(VmInstanceState.Running)
             || vmInstanceVO.getState().equals(VmInstanceState.Paused)) {
                checkRunningVMQuotaForChangeResourceOwner(vmInstanceUuid, resourceTargetOwnerAccountUuid,
                        currentAccountUuid, pairs);
            }

            // check volume
            ArrayList<String> rootVolumeUuids = new ArrayList<>();
            SimpleQuery<VolumeVO> sq = dbf.createQuery(VolumeVO.class);
            sq.add(VolumeVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vmInstanceUuid);
            sq.add(VolumeVO_.type, SimpleQuery.Op.EQ, VolumeType.Root);
            VolumeVO volumeVO = sq.find();
            if (volumeVO != null) {
                rootVolumeUuids.add(volumeVO.getUuid());
            }

            ArrayList<String> dataVolumeUuids = new ArrayList<>();
            SimpleQuery<VolumeVO> sq1 = dbf.createQuery(VolumeVO.class);
            sq1.add(VolumeVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vmInstanceUuid);
            sq1.add(VolumeVO_.type, SimpleQuery.Op.EQ, VolumeType.Data);
            List<VolumeVO> volumeVOs = sq1.list();
            if (volumeVOs != null && !volumeVOs.isEmpty()) {
                for (VolumeVO vvo : volumeVOs) {
                    dataVolumeUuids.add(vvo.getUuid());
                }
            }

            checkVolumeQuotaForChangeResourceOwner(dataVolumeUuids, rootVolumeUuids,
                    resourceTargetOwnerAccountUuid, currentAccountUuid, pairs);
        }

    }

    private void check(APIRecoverDataVolumeMsg msg, Map<String, Quota.QuotaPair> pairs) {
        String currentAccountUuid = msg.getSession().getAccountUuid();
        String resourceTargetOwnerAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(msg.getVolumeUuid());
        // check data volume num
        long dataVolumeNumQuota = pairs.get(VmQuotaConstant.DATA_VOLUME_NUM).getValue();
        long dataVolumeNumUsed = new VmQuotaUtil().getUsedDataVolumeCount(resourceTargetOwnerAccountUuid);
        long dataVolumeNumAsked = 1;

        QuotaUtil.QuotaCompareInfo quotaCompareInfo;
        {
            quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
            quotaCompareInfo.currentAccountUuid = currentAccountUuid;
            quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
            quotaCompareInfo.quotaName = VmQuotaConstant.DATA_VOLUME_NUM;
            quotaCompareInfo.quotaValue = dataVolumeNumQuota;
            quotaCompareInfo.currentUsed = dataVolumeNumUsed;
            quotaCompareInfo.request = dataVolumeNumAsked;
            new QuotaUtil().CheckQuota(quotaCompareInfo);
        }
    }

    private long getDataVolumeSizeAsked(APICreateDataVolumeMsg msg) {
        if (msg.getDiskOfferingUuid() == null) {
            return msg.getDiskSize();
        }

        String sql = "select diskSize from DiskOfferingVO where uuid = :uuid ";
        TypedQuery<Long> dq = dbf.getEntityManager().createQuery(sql, Long.class);
        dq.setParameter("uuid", msg.getDiskOfferingUuid());
        Long dsize = dq.getSingleResult();
        dsize = dsize == null ? 0 : dsize;
        return dsize;
    }

    @Transactional(readOnly = true)
    private void check(APICreateDataVolumeMsg msg, Map<String, Quota.QuotaPair> pairs) {
        String currentAccountUuid = msg.getSession().getAccountUuid();
        String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();

        long dataVolumeNumQuota = pairs.get(VmQuotaConstant.DATA_VOLUME_NUM).getValue();
        long allVolumeSizeQuota = pairs.get(VmQuotaConstant.VOLUME_SIZE).getValue();

        // check data volume num
        long dataVolumeNumUsed = new VmQuotaUtil().getUsedDataVolumeCount(currentAccountUuid);
        long dataVolumeNumAsked = 1;
        QuotaUtil.QuotaCompareInfo quotaCompareInfo;
        {
            quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
            quotaCompareInfo.currentAccountUuid = currentAccountUuid;
            quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
            quotaCompareInfo.quotaName = VmQuotaConstant.DATA_VOLUME_NUM;
            quotaCompareInfo.quotaValue = dataVolumeNumQuota;
            quotaCompareInfo.currentUsed = dataVolumeNumUsed;
            quotaCompareInfo.request = dataVolumeNumAsked;
            new QuotaUtil().CheckQuota(quotaCompareInfo);
        }

        // check data volume size
        long allVolumeSizeAsked = getDataVolumeSizeAsked(msg);

        long allVolumeSizeUsed = new VmQuotaUtil().getUsedAllVolumeSize(currentAccountUuid);
        {
            quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
            quotaCompareInfo.currentAccountUuid = currentAccountUuid;
            quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
            quotaCompareInfo.quotaName = VmQuotaConstant.VOLUME_SIZE;
            quotaCompareInfo.quotaValue = allVolumeSizeQuota;
            quotaCompareInfo.currentUsed = allVolumeSizeUsed;
            quotaCompareInfo.request = allVolumeSizeAsked;
            new QuotaUtil().CheckQuota(quotaCompareInfo);
        }
    }

    private Pair<Integer, Long> getInstanceOfferingAsked(APICreateVmInstanceMsg msg) {

        if (msg.getInstanceOfferingUuid() != null) {
            String sql = "select i.cpuNum, i.memorySize" +
                    " from InstanceOfferingVO i" +
                    " where i.uuid = :uuid";
            TypedQuery<Tuple> iq = dbf.getEntityManager().createQuery(sql, Tuple.class);
            iq.setParameter("uuid", msg.getInstanceOfferingUuid());
            Tuple t = iq.getSingleResult();
            return new Pair<Integer, Long>(t.get(0, Integer.class), t.get(1, Long.class));
        }

        return new Pair<Integer, Long>(msg.getCpuNum(), msg.getMemorySize());
    }

    @Transactional(readOnly = true)
    private void check(APICreateVmInstanceMsg msg, Map<String, Quota.QuotaPair> pairs) {
        String currentAccountUuid = msg.getSession().getAccountUuid();
        String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();

        long totalVmNumQuota = pairs.get(VmQuotaConstant.VM_TOTAL_NUM).getValue();
        long runningVmNumQuota = pairs.get(VmQuotaConstant.VM_RUNNING_NUM).getValue();
        long runningVmCpuNumQuota = pairs.get(VmQuotaConstant.VM_RUNNING_CPU_NUM).getValue();
        long runningVmMemorySizeQuota = pairs.get(VmQuotaConstant.VM_RUNNING_MEMORY_SIZE).getValue();
        long dataVolumeNumQuota = pairs.get(VmQuotaConstant.DATA_VOLUME_NUM).getValue();
        long allVolumeSizeQuota = pairs.get(VmQuotaConstant.VOLUME_SIZE).getValue();


        VmQuotaUtil.VmQuota vmQuotaUsed = new VmQuotaUtil().getUsedVmCpuMemory(currentAccountUuid);

        if (vmQuotaUsed.totalVmNum + 1 > totalVmNumQuota) {
            throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                    currentAccountUuid, VmQuotaConstant.VM_TOTAL_NUM, totalVmNumQuota));
        }

        if (vmQuotaUsed.runningVmNum + 1 > runningVmNumQuota) {
            throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                    currentAccountUuid, VmQuotaConstant.VM_RUNNING_NUM, runningVmNumQuota));
        }

        final Pair<Integer, Long> pair = getInstanceOfferingAsked(msg);
        int cpuNumAsked = pair.first();
        long memoryAsked = pair.second();

        if (vmQuotaUsed.runningVmCpuNum + cpuNumAsked > runningVmCpuNumQuota) {
            throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                    currentAccountUuid, VmQuotaConstant.VM_RUNNING_CPU_NUM, runningVmCpuNumQuota));
        }

        if (vmQuotaUsed.runningVmMemorySize + memoryAsked > runningVmMemorySizeQuota) {
            throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                    currentAccountUuid, VmQuotaConstant.VM_RUNNING_MEMORY_SIZE, runningVmMemorySizeQuota));
        }

        // check data volume num
        if (msg.getDataDiskOfferingUuids() != null && !msg.getDataDiskOfferingUuids().isEmpty()) {
            long dataVolumeNumUsed = new VmQuotaUtil().getUsedDataVolumeCount(currentAccountUuid);
            long dataVolumeNumAsked = msg.getDataDiskOfferingUuids().size();
            if (dataVolumeNumUsed + dataVolumeNumAsked > dataVolumeNumQuota) {
                throw new ApiMessageInterceptionException(new QuotaUtil().buildQuataExceedError(
                        currentAccountUuid, VmQuotaConstant.DATA_VOLUME_NUM, dataVolumeNumQuota));
            }
        }

        // check all volume size
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
            imgSize = null;
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

        long allVolumeSizeUsed = new VmQuotaUtil().getUsedAllVolumeSize(currentAccountUuid);
        QuotaUtil.QuotaCompareInfo quotaCompareInfo;
        {
            quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
            quotaCompareInfo.currentAccountUuid = currentAccountUuid;
            quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
            quotaCompareInfo.quotaName = VmQuotaConstant.VOLUME_SIZE;
            quotaCompareInfo.quotaValue = allVolumeSizeQuota;
            quotaCompareInfo.currentUsed = allVolumeSizeUsed;
            quotaCompareInfo.request = allVolumeSizeAsked;
            new QuotaUtil().CheckQuota(quotaCompareInfo);
        }
    }

    private void check(APIRecoverVmInstanceMsg msg, Map<String, Quota.QuotaPair> pairs) {
        String currentAccountUuid = msg.getSession().getAccountUuid();
        String resourceTargetOwnerAccountUuid = msg.getSession().getAccountUuid();

        long totalVmNumQuota = pairs.get(VmQuotaConstant.VM_TOTAL_NUM).getValue();
        VmQuotaUtil.VmQuota vmQuotaUsed = new VmQuotaUtil().getUsedVmCpuMemory(currentAccountUuid);
        long totalVmNumAsked = 1;
        QuotaUtil.QuotaCompareInfo quotaCompareInfo;
        {
            quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
            quotaCompareInfo.currentAccountUuid = currentAccountUuid;
            quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
            quotaCompareInfo.quotaName = VmQuotaConstant.VM_TOTAL_NUM;
            quotaCompareInfo.quotaValue = totalVmNumQuota;
            quotaCompareInfo.currentUsed = vmQuotaUsed.totalVmNum;
            quotaCompareInfo.request = totalVmNumAsked;
            new QuotaUtil().CheckQuota(quotaCompareInfo);
        }
    }
}
