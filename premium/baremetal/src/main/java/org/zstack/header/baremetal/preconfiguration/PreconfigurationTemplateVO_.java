package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2018-12-28.
 */
@StaticMetamodel(PreconfigurationTemplateVO.class)
public class PreconfigurationTemplateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PreconfigurationTemplateVO, String> name;
    public static volatile SingularAttribute<PreconfigurationTemplateVO, String> description;
    public static volatile SingularAttribute<PreconfigurationTemplateVO, String> distribution;
    public static volatile SingularAttribute<PreconfigurationTemplateVO, String> type;
    public static volatile SingularAttribute<PreconfigurationTemplateVO, String> content;
    public static volatile SingularAttribute<PreconfigurationTemplateVO, String> md5sum;
    public static volatile SingularAttribute<PreconfigurationTemplateVO, Boolean> isPredefined;
    public static volatile SingularAttribute<PreconfigurationTemplateVO, String> state;
    public static volatile SingularAttribute<PreconfigurationTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<PreconfigurationTemplateVO, Timestamp> lastOpDate;
}
