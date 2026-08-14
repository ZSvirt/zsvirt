package org.zstack.header.storage.primary;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(PrimaryStorageHistoricalUsageBaseVO.class)
public class PrimaryStorageHistoricalUsageBaseVO_ extends HistoricalUsageAO_ {
    public static volatile SingularAttribute<PrimaryStorageHistoricalUsageBaseVO, String> primaryStorageUuid;
}
