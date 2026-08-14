package org.zstack.guesttools.advanced;

import org.zstack.header.configuration.VmCustomSpecificationDomainMode;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmCustomSpecificationVO.class)
public class VmCustomSpecificationVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VmCustomSpecificationVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmCustomSpecificationVO, String> name;
    public static volatile SingularAttribute<VmCustomSpecificationVO, String> description;
    public static volatile SingularAttribute<VmCustomSpecificationVO, String> platform;
    public static volatile SingularAttribute<VmCustomSpecificationVO, String> hostname;
    public static volatile SingularAttribute<VmCustomSpecificationVO, String> rootPassword;
    public static volatile SingularAttribute<VmCustomSpecificationVO, Boolean> generateSID;
    public static volatile SingularAttribute<VmCustomSpecificationVO, VmCustomSpecificationDomainMode> domainMode;
    public static volatile SingularAttribute<VmCustomSpecificationVO, String> domainName;
    public static volatile SingularAttribute<VmCustomSpecificationVO, String> domainUsername;
    public static volatile SingularAttribute<VmCustomSpecificationVO, String> domainPassword;
    public static volatile SingularAttribute<VmCustomSpecificationVO, String> organization;
    public static volatile SingularAttribute<VmCustomSpecificationVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmCustomSpecificationVO, Timestamp> lastOpDate;
}
