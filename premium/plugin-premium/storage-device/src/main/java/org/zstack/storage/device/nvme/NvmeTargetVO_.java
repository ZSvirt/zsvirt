package org.zstack.storage.device.nvme;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by MaJin on 2022/8/10.
 */

@StaticMetamodel(NvmeTargetVO.class)
public class NvmeTargetVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<NvmeTargetVO, String> state;
    public static volatile SingularAttribute<NvmeTargetVO, String> name;
    public static volatile SingularAttribute<NvmeTargetVO, String> nqn;
    public static volatile SingularAttribute<NvmeTargetVO, String> nvmeServerUuid;
    public static volatile SingularAttribute<NvmeTargetVO, Timestamp> createDate;
    public static volatile SingularAttribute<NvmeTargetVO, Timestamp> lastOpDate;
}
