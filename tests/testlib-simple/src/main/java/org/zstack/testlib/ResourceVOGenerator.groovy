package org.zstack.testlib

import org.zstack.core.Platform
import org.zstack.header.vo.EO
import org.zstack.header.vo.ResourceAttributes
import org.zstack.header.vo.ResourceVO
import org.zstack.utils.FieldUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import javax.persistence.Entity
import java.lang.reflect.Field

/**
 * Created by xing5 on 2017/4/19.
 */
class ResourceVOGenerator {
    CLogger logger = Utils.getLogger(getClass())

    void generate(String outputDir) {
        File dir = new File([outputDir, "zstack-resourcevo"].join("/"))
        dir.mkdirs()

        Set<Class> resourceVOs = Platform.reflections.getSubTypesOf(ResourceVO.class)
        resourceVOs = resourceVOs.findAll { return it.isAnnotationPresent(Entity.class) && !it.isAnnotationPresent(EO.class) }

        String sql = writeSqlText(resourceVOs as List)

        new File([dir.absolutePath, "sql.sql"].join("/")).write(sql)
    }

    private Class getForeignClass(Class it) {
        EO eo = it.getAnnotation(EO.class)
        return eo == null ? it : eo.EOClazz()
    }

    private String writeSqlText(List<Class> voClasses) {
        voClasses.sort(new Comparator<Class>() {
            @Override
            int compare(Class o1, Class o2) {
                return o1.simpleName <=> o2.simpleName
            }
        })

        List<String> fkeys = voClasses.collect {
            return "ALTER TABLE ${getForeignClass(it).simpleName} ADD CONSTRAINT fk${it.simpleName}ResourceVO FOREIGN KEY (uuid) REFERENCES  ResourceVO (uuid) ON DELETE CASCADE;"
        }

        List<String> inserts = voClasses.collect {
            ResourceAttributes at = it.getAnnotation(ResourceAttributes.class)
            String nameFieldName = at == null ? "name" : at.nameField()
            Field nameField = FieldUtils.getField(nameFieldName, it)

            if (nameField == null) {
                return "INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, \"${it.simpleName}\" FROM ${it.simpleName} t;"
            } else {
                return "INSERT INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.${nameFieldName}, \"${it.simpleName}\" FROM ${it.simpleName} t;"
            }
        }

        return """\
${fkeys.join("\n")}

${inserts.join("\n")}
"""
    }
}
