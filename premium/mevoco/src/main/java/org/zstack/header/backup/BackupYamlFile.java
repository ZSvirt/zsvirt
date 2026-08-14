package org.zstack.header.backup;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.*;

public class BackupYamlFile {

    private static class MyRepresenter extends Representer {
        public MyRepresenter(DumperOptions options) {
            super(options);
        }

        @Override
        protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
            if (propertyValue == null) {
                return null;
            } else if (propertyValue instanceof Map && ((Map)property).isEmpty()) {
                return null;
            } else if (property instanceof Collection && ((Collection)propertyValue).isEmpty()) {
                return null;
            } else {
                return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
            }
        }

        @Override
        protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
            if (!classTags.containsKey(javaBean.getClass())) {
                addClassTag(javaBean.getClass(), Tag.MAP);
            }

            return super.representJavaBean(properties, javaBean);
        }
    }

    public static String dump(Map<String, Map<String, List<NonBackupInfo>>> infos) {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setCanonical(false);
        opts.setIndent(2);
        opts.setPrettyFlow(true);

        return new Yaml(new MyRepresenter(opts), opts).dump(JSONObjectUtil.rehashObject(infos, LinkedHashMap.class));
    }

    public static Map<String, Map<String, List<NonBackupInfo>>> load(String content) {
        Map<String, Map<String, List<NonBackupInfo>>> results = new HashMap<>();
        Map<String, Map<String, List<Map<String, String>>>> yaml = new Yaml().loadAs(content, LinkedHashMap.class);
        for (Map.Entry<String, Map<String, List<Map<String, String>>>> serviceEntry : yaml.entrySet()) {
            results.put(serviceEntry.getKey(), new HashMap<>());
            for (Map.Entry<String, List<Map<String, String>>> resourceEntry : serviceEntry.getValue().entrySet()) {
                results.get(serviceEntry.getKey()).put(resourceEntry.getKey(), new ArrayList<>());
                for (Map<String, String> info : resourceEntry.getValue()) {
                    results.get(serviceEntry.getKey()).get(resourceEntry.getKey()).add(read(resourceEntry.getKey(), info));
                }
            }
        }

        return results;
    }

    private static NonBackupInfo read(String resourceType, Map<String, String> values) {
        NonBackupInfo info;
        if (resourceType.equals(SystemTagVO.class.getSimpleName())) {
            info = JSONObjectUtil.rehashObject(values, SystemTagNonBackupInfo.class);
        } else {
            info = JSONObjectUtil.rehashObject(values, NonBackupInfo.class);
        }

        info.setResourceType(resourceType);
        return info;
    }
}
