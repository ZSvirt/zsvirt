package org.zstack.core.plugin;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PluginDriverVO.class)
public class PluginDriverVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PluginDriverVO, String> name;
    public static volatile SingularAttribute<PluginDriverVO, String> type;
    public static volatile SingularAttribute<PluginDriverVO, String> vendor;
    public static volatile SingularAttribute<PluginDriverVO, String> license;
    public static volatile SingularAttribute<PluginDriverVO, String> version;
    public static volatile SingularAttribute<PluginDriverVO, String> description;
    public static volatile SingularAttribute<PluginDriverVO, String> features;
    public static volatile SingularAttribute<PluginDriverVO, String> optionTypes;
    public static volatile SingularAttribute<PluginDriverVO, Timestamp> createDate;
    public static volatile SingularAttribute<PluginDriverVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<PluginDriverVO, String> deleted;
}
