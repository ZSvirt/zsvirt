package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.L2NetworkInventory;

import java.util.List;

/**
 * Created by weiwang on 02/05/2017.
 */
public interface VxlanNetworkChecker {
    APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException;
    void validateSystemTagFormat(List<String> systemTags);
    void validateVniRangeOverlap(L2NetworkInventory inv, String clusterUuid);
}
