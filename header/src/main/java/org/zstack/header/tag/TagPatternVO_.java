package org.zstack.header.tag;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(TagPatternVO.class)
public class TagPatternVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<TagPatternVO, String> name;
    public static volatile SingularAttribute<TagPatternVO, String> value;
    public static volatile SingularAttribute<TagPatternVO, String> description;
    public static volatile SingularAttribute<TagPatternVO, String> color;
    public static volatile SingularAttribute<TagPatternVO, TagPatternType> type;
    public static volatile SingularAttribute<TagPatternVO, Timestamp> createDate;
    public static volatile SingularAttribute<TagPatternVO, Timestamp> lastOpDate;
}
