package org.zstack.configuration;

import org.apache.commons.io.FileUtils;
import org.zstack.header.configuration.APIGenerateSqlForeignKeyMsg;
import org.zstack.header.configuration.APIGenerateSqlIndexMsg;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.Index;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Entity;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

/**
 */
public class SqlIndexGenerator {
    private static CLogger logger = Utils.getLogger(SqlIndexGenerator.class);

    private class IndexInfo {
        Class entity;
        Field indexField;

        IndexInfo(Class entity, Field f) {
            this.entity = entity;
            indexField = f;
        }

        String toIndexSql() {
            Index idx = indexField.getAnnotation(Index.class);
            if (String.class.isAssignableFrom(indexField.getType()) && idx.length() != -1) {
                return String.format("CREATE INDEX %s ON %s (%s(%s));",
                        String.format("idx%s%s", entity.getSimpleName(), indexField.getName()),
                        entity.getSimpleName(),
                        indexField.getName(),
                        idx.length()
                );
            } else {
                return String.format("CREATE INDEX %s ON %s (%s);",
                        String.format("idx%s%s", entity.getSimpleName(), indexField.getName()),
                        entity.getSimpleName(),
                        indexField.getName()
                );
            }
        }
    }

    private String outputPath;
    private List<String> basePkgs;

    private List<Class> entityClass = new ArrayList<Class>();
    private Map<Class, List<IndexInfo>> indexMap = new HashMap<Class, List<IndexInfo>>();
    private StringBuilder writer = new StringBuilder();

    public SqlIndexGenerator(APIGenerateSqlIndexMsg msg) {
        outputPath = msg.getOutputPath();
        if (outputPath == null) {
            outputPath = PathUtil.join(System.getProperty("user.home"), "zstack-sql", "indexes.sql");
        }
        basePkgs = msg.getBasePackageNames();
        if (basePkgs == null) {
            basePkgs = Arrays.asList("org.zstack");
        }
    }

    public void generate() {
        entityClass.addAll(BeanUtils.reflections.getTypesAnnotatedWith(Entity.class));

        for (Class entity : entityClass) {
            collectIndex(entity);
        }


        generateIndex();
    }

    private void generateIndex() {
        List<Class> classes = new ArrayList<Class>();
        classes.addAll(indexMap.keySet());
        Collections.sort(classes, new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                return o1.getSimpleName().compareTo(o2.getSimpleName());
            }
        });
        
        for (Class clz : classes) {
            generateIndexForEntity(indexMap.get(clz));
        }

        try {
            FileUtils.writeStringToFile(new File(outputPath), writer.toString());
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void generateIndexForEntity(List<IndexInfo> keys) {
        if (keys.isEmpty()) {
            return;
        }

        writer.append(String.format("\n# Index for table %s\n", keys.get(0).entity.getSimpleName()));
        for (IndexInfo key : keys) {
            writer.append(String.format("\n%s", key.toIndexSql()));
        }
        writer.append("\n");
    }


    private void collectIndex(Class entity) {
        List<Field> fs;
        Class superClass = entity.getSuperclass();
        if (superClass.isAnnotationPresent(Entity.class) || entity.isAnnotationPresent(EO.class)) {
            // parent class or EO class is also an entity, it will take care of its foreign key,
            // so we only do our own foreign keys;
            fs = FieldUtils.getAnnotatedFieldsOnThisClass(Index.class, entity);
        } else {
            fs = FieldUtils.getAnnotatedFields(Index.class, entity);
        }

        List<IndexInfo> keyInfos = indexMap.get(entity);
        if (keyInfos == null) {
            keyInfos = new ArrayList<IndexInfo>();
            indexMap.put(entity, keyInfos);
        }

        for (Field f : fs) {
            keyInfos.add(new IndexInfo(entity, f));
        }
    }
}
