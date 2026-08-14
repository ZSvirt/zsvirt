package org.zstack.compute.cluster;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BaseClusterFactory implements ClusterFactory {
    static final ClusterType type = new ClusterType(ClusterConstant.ZSTACK_CLUSTER_TYPE);
    
    @Autowired
    private DatabaseFacade dbf;
    
    @Override
    public ClusterType getType() {
        return type;
    }

    @Override
    public ClusterVO createCluster(ClusterVO vo, CreateClusterMessage msg) {
        vo.setType(type.toString());
        vo = dbf.persistAndRefresh(vo);
        return vo;
    }

    @Override
    public Cluster getCluster(ClusterVO vo) {
        return new ClusterBase(vo);
    }
}
