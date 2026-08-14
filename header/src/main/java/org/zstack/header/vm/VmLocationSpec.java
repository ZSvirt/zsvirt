package org.zstack.header.vm;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;

import java.io.Serializable;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * <p>Recommended VM start in cluster 7382465192e74001a4c2876e77cc63d2
 * <blockquote><pre>
 * {uuids=[7382465192e74001a4c2876e77cc63d2], resourceType=ClusterVO, level=Recommended}
 * </pre></blockquote>
 *
 * <p>Not Recommended VM start at host 198468a0e6024087ba00ed25112e6425
 * <blockquote><pre>
 * {uuids=[198468a0e6024087ba00ed25112e6425], resourceType=HostVO, level=NotRecommended}
 * </pre></blockquote>
 */
public class VmLocationSpec implements Serializable {
    public static final String RECOMMENDED = "Recommended";
    public static final String NOT_RECOMMENDED = "NotRecommended";
    public static final String AVOID = "Avoid";
    public static final String ONLY_ALLOWED = "OnlyAllowed";

    private List<String> uuids;
    private String resourceType;
    private String level;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public boolean avoid() {
        return AVOID.equals(level);
    }

    public boolean recommended() {
        return RECOMMENDED.equals(level);
    }

    public boolean notRecommended() {
        return NOT_RECOMMENDED.equals(level);
    }

    public boolean onlyAllowed() {
        return ONLY_ALLOWED.equals(level);
    }

    public static VmLocationSpec recommendCluster(String... clusterUuidArray) {
        return recommendCluster(list(clusterUuidArray));
    }

    public static VmLocationSpec recommendCluster(List<String> clusterUuidList) {
        VmLocationSpec spec = new VmLocationSpec();
        spec.uuids = clusterUuidList;
        spec.resourceType = ClusterVO.class.getSimpleName();
        spec.level = RECOMMENDED;
        return spec;
    }

    public static VmLocationSpec notRecommendCluster(String... clusterUuidArray) {
        return notRecommendCluster(list(clusterUuidArray));
    }

    public static VmLocationSpec notRecommendCluster(List<String> clusterUuidList) {
        VmLocationSpec spec = new VmLocationSpec();
        spec.uuids = clusterUuidList;
        spec.resourceType = ClusterVO.class.getSimpleName();
        spec.level = NOT_RECOMMENDED;
        return spec;
    }

    public static VmLocationSpec onlyAllowedCluster(String... clusterUuidArray) {
        return onlyAllowedCluster(list(clusterUuidArray));
    }

    public static VmLocationSpec onlyAllowedCluster(List<String> clusterUuidList) {
        VmLocationSpec spec = new VmLocationSpec();
        spec.uuids = clusterUuidList;
        spec.resourceType = ClusterVO.class.getSimpleName();
        spec.level = ONLY_ALLOWED;
        return spec;
    }

    public static VmLocationSpec recommendHost(String... hostUuidArray) {
        return recommendHost(list(hostUuidArray));
    }

    public static VmLocationSpec recommendHost(List<String> hostUuidList) {
        VmLocationSpec spec = new VmLocationSpec();
        spec.uuids = hostUuidList;
        spec.resourceType = HostVO.class.getSimpleName();
        spec.level = RECOMMENDED;
        return spec;
    }

    public static VmLocationSpec notRecommendHost(String... hostUuidArray) {
        return notRecommendHost(list(hostUuidArray));
    }

    public static VmLocationSpec notRecommendHost(List<String> hostUuidList) {
        VmLocationSpec spec = new VmLocationSpec();
        spec.uuids = hostUuidList;
        spec.resourceType = HostVO.class.getSimpleName();
        spec.level = NOT_RECOMMENDED;
        return spec;
    }

    public static VmLocationSpec avoidHost(String... hostUuidArray) {
        return avoidHost(list(hostUuidArray));
    }

    public static VmLocationSpec avoidHost(List<String> hostUuidList) {
        VmLocationSpec spec = new VmLocationSpec();
        spec.uuids = hostUuidList;
        spec.resourceType = HostVO.class.getSimpleName();
        spec.level = AVOID;
        return spec;
    }
}
