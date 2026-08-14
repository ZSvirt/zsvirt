package org.zstack.header.image;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(GuestOsCategoryVO.class)
public class GuestOsCategoryVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<GuestOsCategoryVO, String> platform;
    public static volatile SingularAttribute<GuestOsCategoryVO, String> name;
    public static volatile SingularAttribute<GuestOsCategoryVO, String> version;
    public static volatile SingularAttribute<GuestOsCategoryVO, String> osRelease;
}
