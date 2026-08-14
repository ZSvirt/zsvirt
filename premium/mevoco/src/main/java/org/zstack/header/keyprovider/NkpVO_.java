package org.zstack.header.keyprovider;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(NkpVO.class)
public class NkpVO_ extends KeyProviderVO_ {
    public static volatile SingularAttribute<NkpVO, String> kdf;
    public static volatile SingularAttribute<NkpVO, String> saltPolicy;
    public static volatile SingularAttribute<NkpVO, Boolean> backedUp;
    public static volatile SingularAttribute<NkpVO, Integer> currentVersion;
}
