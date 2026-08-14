package org.zstack.storage.device.iscsi;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/2
 */
@StaticMetamodel(IscsiServerVO.class)
public class IscsiServerVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<IscsiServerVO, String> ip;
    public static volatile SingularAttribute<IscsiServerVO, String> state;
    public static volatile SingularAttribute<IscsiServerVO, String> name;
    public static volatile SingularAttribute<IscsiServerVO, Integer> port;
    public static volatile SingularAttribute<IscsiServerVO, String> chapUserName;
    public static volatile SingularAttribute<IscsiServerVO, String> chapUserPassword;
    public static volatile SingularAttribute<IscsiServerVO, Timestamp> createDate;
    public static volatile SingularAttribute<IscsiServerVO, Timestamp> lastOpDate;
}
