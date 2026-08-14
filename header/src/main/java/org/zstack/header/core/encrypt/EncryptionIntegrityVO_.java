package org.zstack.header.core.encrypt;

import org.zstack.header.core.trash.InstallPathRecycleVO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * @Author: DaoDao
 * @Date: 2021/10/29
 */
@StaticMetamodel(EncryptionIntegrityVO.class)
public class EncryptionIntegrityVO_ {
    public static volatile SingularAttribute<EncryptionIntegrityVO, Long> id;
    public static volatile SingularAttribute<EncryptionIntegrityVO, String> resourceUuid;
    public static volatile SingularAttribute<EncryptionIntegrityVO, String> resourceType;
    public static volatile SingularAttribute<EncryptionIntegrityVO, String> signedText;
    public static volatile SingularAttribute<EncryptionIntegrityVO, Timestamp> createDate;
    public static volatile SingularAttribute<EncryptionIntegrityVO, Timestamp> lastOpDate;
}
