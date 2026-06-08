package org.zstack.header.core.external.service;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import java.sql.Timestamp;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 1:30 AM
 */
public class ExternalServiceConfigurationVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ExternalServiceConfigurationVO, String> serviceType;
    public static volatile SingularAttribute<ExternalServiceConfigurationVO, String> configuration;
    public static volatile SingularAttribute<ExternalServiceConfigurationVO, String> description;
    public static volatile SingularAttribute<ExternalServiceConfigurationVO, Timestamp> createDate;
    public static volatile SingularAttribute<ExternalServiceConfigurationVO, Timestamp> lastOpDate;
}
