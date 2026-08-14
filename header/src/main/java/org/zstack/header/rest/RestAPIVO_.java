package org.zstack.header.rest;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(RestAPIVO.class)
public class RestAPIVO_ {
    public static volatile SingularAttribute<RestAPIVO, String> uuid;
    public static volatile SingularAttribute<RestAPIVO, String> result;
    public static volatile SingularAttribute<RestAPIVO, String> apiMessageName;
    public static volatile SingularAttribute<RestAPIVO, Timestamp> createDate;
    public static volatile SingularAttribute<RestAPIVO, Timestamp> lastOpDate;
}
