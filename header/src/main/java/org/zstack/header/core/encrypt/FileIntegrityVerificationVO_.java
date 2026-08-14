package org.zstack.header.core.encrypt;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @author hanyu.liang
 * @date 2023/4/7 16:27
 */
@StaticMetamodel(FileIntegrityVerificationVO.class)
public class FileIntegrityVerificationVO_ {
    public static volatile SingularAttribute<FileIntegrityVerificationVO, String> id;
    public static volatile SingularAttribute<FileIntegrityVerificationVO, String> path;
    public static volatile SingularAttribute<FileIntegrityVerificationVO, String> nodeType;
    public static volatile SingularAttribute<FileIntegrityVerificationVO, String> nodeUuid;
    public static volatile SingularAttribute<FileIntegrityVerificationVO, String> hexType;
    public static volatile SingularAttribute<FileIntegrityVerificationVO, String> digest;
}
