package org.zstack.iam1.compute.ensemble;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.network.l3.AfterAddIpRangeExtensionPoint;
import org.zstack.header.network.l3.IpRangeInventory;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2024/10/08
 */
public class L3ResourceEnsembleExtensions implements
        AfterAddIpRangeExtensionPoint {
    @Autowired
    private EnsembleExtensions extensions;

    @Override
    public void afterAddIpRange(IpRangeInventory ipr, List<String> systemTags) {
        extensions.changeResourceEnsemble(ipr.getL3NetworkUuid(), list(ipr.getUuid()));
    }
}
