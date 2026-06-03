package org.zstack.header.pki;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PkiCaVO.class)
public class PkiCaVO_ {
    public static volatile SingularAttribute<PkiCaVO, String> uuid;
    public static volatile SingularAttribute<PkiCaVO, String> scope;
    public static volatile SingularAttribute<PkiCaVO, String> caType;
    public static volatile SingularAttribute<PkiCaVO, String> subjectDn;
    public static volatile SingularAttribute<PkiCaVO, String> certChainPem;
    public static volatile SingularAttribute<PkiCaVO, String> encryptedPrivateKeyPem;
    public static volatile SingularAttribute<PkiCaVO, String> serial;
    public static volatile SingularAttribute<PkiCaVO, String> fingerprint;
    public static volatile SingularAttribute<PkiCaVO, String> status;
    public static volatile SingularAttribute<PkiCaVO, String> crlPem;
    public static volatile SingularAttribute<PkiCaVO, Timestamp> notBefore;
    public static volatile SingularAttribute<PkiCaVO, Timestamp> notAfter;
    public static volatile SingularAttribute<PkiCaVO, Timestamp> createDate;
    public static volatile SingularAttribute<PkiCaVO, Timestamp> lastOpDate;
}
