package org.zstack.header.acl;

import org.apache.commons.net.ntp.TimeStamp;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-05
 **/
@StaticMetamodel(AccessControlListVO.class)
public class AccessControlListVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AccessControlListVO, String> name;
    public static volatile SingularAttribute<AccessControlListVO, Integer> ipVersion;
    public static volatile SingularAttribute<AccessControlListVO, String> description;
    public static volatile SingularAttribute<AccessControlListVO, TimeStamp> createDate;
    public static volatile SingularAttribute<AccessControlListVO, TimeStamp> lastOpDate;
}
