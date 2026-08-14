package org.zstack.header.tpm.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(TpmVO.class)
public class TpmVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<TpmVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<TpmVO, Timestamp> createDate;
    public static volatile SingularAttribute<TpmVO, Timestamp> lastOpDate;
}
