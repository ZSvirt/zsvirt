package org.zstack.header.network.l2;

import org.zstack.header.cluster.ClusterVO;

import java.util.List;

public interface L2NetworkCandidateFilterExtensionPoint {
    void filterL2NetworkCandidates(List<L2NetworkVO> candidates, ClusterVO clusterVO);
    void filterClusterCandidates(List<ClusterVO> candidates, L2NetworkVO l2NetworkVO);
}
