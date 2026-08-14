package org.zstack.header.keyprovider;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(KeyProviderVO.class)
public class KeyProviderVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<KeyProviderVO, String> name;
    public static volatile SingularAttribute<KeyProviderVO, String> description;
    public static volatile SingularAttribute<KeyProviderVO, KeyProviderType> type;
    public static volatile SingularAttribute<KeyProviderVO, Boolean> connected;
    public static volatile SingularAttribute<KeyProviderVO, Timestamp> createDate;
    public static volatile SingularAttribute<KeyProviderVO, Timestamp> lastOpDate;
}
