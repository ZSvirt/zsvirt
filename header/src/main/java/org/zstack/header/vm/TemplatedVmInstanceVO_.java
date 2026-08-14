package org.zstack.header.vm;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(TemplatedVmInstanceVO.class)
public class TemplatedVmInstanceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<TemplatedVmInstanceVO, String> uuid;
    public static volatile SingularAttribute<TemplatedVmInstanceVO, Timestamp> createDate;
    public static volatile SingularAttribute<TemplatedVmInstanceVO, Timestamp> lastOpDate;
}
