package org.zstack.header.cloudformation;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
@StaticMetamodel(StackTemplateVO.class)
public class StackTemplateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<StackTemplateVO, String> name;
    public static volatile SingularAttribute<StackTemplateVO, String> description;
    public static volatile SingularAttribute<StackTemplateVO, String> type;
    public static volatile SingularAttribute<StackTemplateVO, String> version;
    public static volatile SingularAttribute<StackTemplateVO, Boolean> state;
    public static volatile SingularAttribute<StackTemplateVO, String> content;
    public static volatile SingularAttribute<StackTemplateVO, String> md5sum;
    public static volatile SingularAttribute<StackTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<StackTemplateVO, Timestamp> lastOpDate;
}
