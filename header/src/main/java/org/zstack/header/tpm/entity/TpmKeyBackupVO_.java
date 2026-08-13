package org.zstack.header.tpm.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(TpmKeyBackupVO.class)
public class TpmKeyBackupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<TpmKeyBackupVO, Timestamp> createDate;
    public static volatile SingularAttribute<TpmKeyBackupVO, Timestamp> lastOpDate;
}
