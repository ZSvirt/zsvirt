package org.zstack.header.pki;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostCertificateVO.class)
public class HostCertificateVO_ {
    public static volatile SingularAttribute<HostCertificateVO, String> uuid;
    public static volatile SingularAttribute<HostCertificateVO, String> hostUuid;
    public static volatile SingularAttribute<HostCertificateVO, String> caUuid;
    public static volatile SingularAttribute<HostCertificateVO, String> usage;
    public static volatile SingularAttribute<HostCertificateVO, String> serial;
    public static volatile SingularAttribute<HostCertificateVO, String> fingerprint;
    public static volatile SingularAttribute<HostCertificateVO, String> sanSnapshot;
    public static volatile SingularAttribute<HostCertificateVO, String> status;
    public static volatile SingularAttribute<HostCertificateVO, Timestamp> notBefore;
    public static volatile SingularAttribute<HostCertificateVO, Timestamp> notAfter;
    public static volatile SingularAttribute<HostCertificateVO, Timestamp> lastInstallDate;
    public static volatile SingularAttribute<HostCertificateVO, String> lastError;
    public static volatile SingularAttribute<HostCertificateVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostCertificateVO, Timestamp> lastOpDate;
}
