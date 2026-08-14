package org.zstack.baremetal.cluster;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.cluster.ClusterBase;
import org.zstack.header.baremetal.BaremetalConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.*;

/**
 * Created by GuoYi on 6/26/18.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BaremetalClusterFactory implements ClusterFactory {
    static final ClusterType type = new ClusterType(BaremetalConstant.BAREMETAL_CLUSTER_TYPE);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public ClusterType getType() {
        return type;
    }

    @Override
    public ClusterVO createCluster(ClusterVO vo, CreateClusterMessage msg) {
        vo.setType(type.toString());
        vo = dbf.updateAndRefresh(vo);
        return vo;
    }

    @Override
    public Cluster getCluster(ClusterVO vo) {
        return new ClusterBase(vo);
    }
}
