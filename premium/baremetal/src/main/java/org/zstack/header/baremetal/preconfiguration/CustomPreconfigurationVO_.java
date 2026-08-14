package org.zstack.header.baremetal.preconfiguration;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by GuoYi on 2018-12-28.
 */
@StaticMetamodel(CustomPreconfigurationVO.class)
public class CustomPreconfigurationVO_ {
    public static volatile SingularAttribute<CustomPreconfigurationVO, String> uuid;
    public static volatile SingularAttribute<CustomPreconfigurationVO, String> baremetalInstanceUuid;
    public static volatile SingularAttribute<CustomPreconfigurationVO, String> param;
    public static volatile SingularAttribute<CustomPreconfigurationVO, String> value;
}
