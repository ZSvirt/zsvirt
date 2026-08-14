package org.zstack.drs.util;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.drs.DRSConstants;
import org.zstack.drs.api.Threshold;
import org.zstack.drs.entity.ClusterDRSVO;
import org.zstack.drs.entity.ClusterDRSVO_;
import org.zstack.header.affinitygroup.AffinityGroupUsageVO;
import org.zstack.header.affinitygroup.AffinityGroupUsageVO_;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.primary.sharedblock.SharedBlockConstants;
import org.zstack.utils.TagUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.*;

/**
 * Created by lining on 2019/12/20.
 */
public class DRSUtils {
    /**
     * @param clusterUuid
     * @return
     *  Reason not supported
     *     supported: return empty list
     */
    public static List<String> validateSupportDRS(String clusterUuid) {
        List<String> notSupportedReasons = new ArrayList<>();

        String hypervisorType = Q.New(ClusterVO.class)
                .select(ClusterVO_.hypervisorType)
                .eq(ClusterVO_.uuid, clusterUuid)
                .findValue();
        if (!hypervisorType.equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
            notSupportedReasons.add(String.format("The cluster[%s] virtualization type needs to be %s", clusterUuid, KVMConstant.KVM_HYPERVISOR_TYPE));
        }

        List<String> supportPsTypes = new ArrayList<>();
        supportPsTypes.add(CephConstants.CEPH_PRIMARY_STORAGE_TYPE);
        supportPsTypes.add(SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE);
        List<String> psTypes = SQL.New("select pri.type" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :clusterUuid", String.class)
                .param("clusterUuid", clusterUuid)
                .list();
        if (!psTypes.isEmpty() && !supportPsTypes.containsAll(psTypes)) {
            notSupportedReasons.add(String.format("The cluster[%s] primary storage type needs to be %s",
                    clusterUuid, String.join(",", supportPsTypes)));
        }

        // simplify the code because be just need check the consistency of system tag no need to get all host uuid into memory
        List<String> modelNames = SQL.New("select distinct(tag.tag) from SystemTagVO tag, HostVO host where " +
                "host.clusterUuid = :clusterUuid " +
                "and tag.resourceUuid = host.uuid " +
                "and tag.tag like :tag", String.class)
                .param("clusterUuid", clusterUuid)
                .param("tag", TagUtils.tagPatternToSqlPattern(KVMSystemTags.CPU_MODEL_NAME.getTagFormat()))
                .list();
        if (modelNames.size() > 1) {
            List<String> details = new ArrayList<>();
            modelNames.forEach(modelName ->
                    details.add(String.format("Host[CPU:%s]", modelName))
            );
            String detail = String.join(",", details);
            notSupportedReasons.add(String.format("Inconsistent CPU models of physical machines in the cluster, %s", detail));
        }

        return notSupportedReasons;
    }

    public static Float getCPUUsedPercentThreshold(String drsUuid) {
        Integer cpuUsedPercentThreshold = null;

        String thresholdStr = Q.New(ClusterDRSVO.class)
                .select(ClusterDRSVO_.thresholds)
                .eq(ClusterDRSVO_.uuid, drsUuid)
                .findValue();

        ArrayList<Threshold> thresholds = JSONObjectUtil.toCollection(thresholdStr, ArrayList.class, Threshold.class);
        for (Threshold threshold : thresholds) {
            if (DRSConstants.CPU_USED_PERCENT_THRESHOLD.equals(threshold.getThresholdName())) {
                cpuUsedPercentThreshold = Integer.parseInt(threshold.getThresholdValue());
            }
        }

        if (cpuUsedPercentThreshold == null) {
            return null;
        }

        return cpuUsedPercentThreshold.floatValue();
    }

    public static Float getMemUsedPercentThreshold(String drsUuid) {
        Integer memUsedPercentThreshold = null;

        String thresholdStr = Q.New(ClusterDRSVO.class)
                .select(ClusterDRSVO_.thresholds)
                .eq(ClusterDRSVO_.uuid, drsUuid)
                .findValue();

        ArrayList<Threshold> thresholds = JSONObjectUtil.toCollection(thresholdStr, ArrayList.class, Threshold.class);
        for (Threshold threshold : thresholds) {
            if (DRSConstants.MEMORY_USED_PERCENT_THRESHOLD.equals(threshold.getThresholdName())) {
                memUsedPercentThreshold = Integer.parseInt(threshold.getThresholdValue());
            }
        }

        if (memUsedPercentThreshold == null) {
            return null;
        }

        return memUsedPercentThreshold.floatValue();
    }
}
