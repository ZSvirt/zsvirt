package org.zstack.storage.device.nvme;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(NvmeServerVO.class)
public class NvmeServerVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<NvmeServerVO, String> ip;
    public static volatile SingularAttribute<NvmeServerVO, String> state;
    public static volatile SingularAttribute<NvmeServerVO, String> name;
    public static volatile SingularAttribute<NvmeServerVO, String> transport;
    public static volatile SingularAttribute<NvmeServerVO, Integer> port;
    public static volatile SingularAttribute<NvmeServerVO, Timestamp> createDate;
    public static volatile SingularAttribute<NvmeServerVO, Timestamp> lastOpDate;
}
