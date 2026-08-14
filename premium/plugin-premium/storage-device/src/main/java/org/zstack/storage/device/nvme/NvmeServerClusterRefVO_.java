package org.zstack.storage.device.nvme;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(NvmeServerClusterRefVO.class)
public class NvmeServerClusterRefVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<NvmeLunVO, String> nvmeServerUuid;
    public static volatile SingularAttribute<NvmeLunVO, String> clusterUuid;
}
