package org.zstack.network.service.lb;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by shixin on 03/22/2018.
 */
@StaticMetamodel(CertificateVO.class)
public class CertificateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<CertificateVO, String> name;
    public static volatile SingularAttribute<CertificateVO, String> certificate;
    public static volatile SingularAttribute<CertificateVO, String> description;
    public static volatile SingularAttribute<CertificateVO, Timestamp> createDate;
    public static volatile SingularAttribute<CertificateVO, Timestamp> lastOpDate;
}
