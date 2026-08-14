package org.zstack.ipsec;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by shixin on 11/21/2017
 */
@StaticMetamodel(IPsecL3NetworkRefVO.class)
public class IPsecL3NetworkRefVO_ {
    public static volatile SingularAttribute<IPsecL3NetworkRefVO, String> uuid;
    public static volatile SingularAttribute<IPsecL3NetworkRefVO, String> connectionUuid;
    public static volatile SingularAttribute<IPsecL3NetworkRefVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<IPsecL3NetworkRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<IPsecL3NetworkRefVO, Timestamp> lastOpDate;
}
