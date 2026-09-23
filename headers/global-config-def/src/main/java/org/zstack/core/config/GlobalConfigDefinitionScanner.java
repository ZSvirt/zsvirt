package org.zstack.core.config;

import org.zstack.core.config.xml.ConfigXmlCell;
import org.zstack.core.config.xml.GlobalConfigXml;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stateless scan of {@link GlobalConfigDefinition} holder classes and
 * {@code globalConfig/*.xml}. Static fields are read as {@link ConfigDefinition}.
 */
public final class GlobalConfigDefinitionScanner {
    private static final CLogger logger = Utils.getLogger(GlobalConfigDefinitionScanner.class);
    private static final String CONFIG_FOLDER = "globalConfig";
    private static final String OTHER_CATEGORY = "Others";

    private GlobalConfigDefinitionScanner() {
    }

    public static List<GlobalConfigDescription> scan() {
        Map<String, GlobalConfigDescription> byIdentity = new LinkedHashMap<>();
        for (GlobalConfigDescription d : parseGlobalConfigFields()) {
            byIdentity.put(d.getIdentity(), d);
        }
        for (GlobalConfigDescription d : loadConfigFromXml()) {
            GlobalConfigDescription existing = byIdentity.get(d.getIdentity());
            if (existing == null) {
                byIdentity.put(d.getIdentity(), d);
            } else if (existing.type != null) {
                throw new IllegalArgumentException(String.format(
                        "duplicate global configuration. a Java @GlobalConfigDef defines a" +
                                " global config[category: %s, name: %s] that has been defined by a XML configure",
                        d.category, d.name));
            } else {
                byIdentity.put(d.getIdentity(), existing.mergeXml(d));
            }
        }
        return new ArrayList<>(byIdentity.values());
    }

    private static List<GlobalConfigDescription> parseGlobalConfigFields() {
        Map<String, GlobalConfigDescription> descriptions = new LinkedHashMap<>();
        Set<Class<?>> definitionClasses = BeanUtils.reflections.getTypesAnnotatedWith(GlobalConfigDefinition.class);
        for (Class<?> def : definitionClasses) {
            for (Field field : def.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())
                        || !ConfigDefinition.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    ConfigDefinition config = (ConfigDefinition) field.get(null);
                    if (config == null) {
                        throw new IllegalStateException(String.format(
                                "GlobalConfigDefinition[%s] defines a null ConfigDefinition[%s]." +
                                        "You must assign a value to it using new GlobalConfig(category, name)",
                                def.getName(), field.getName()));
                    }
                    GlobalConfigDef d = field.getAnnotation(GlobalConfigDef.class);
                    GlobalConfigDescription desc = new GlobalConfigDescription(
                            config.getCategory(),
                            config.getName(),
                            d == null ? null : d.type().getName(),
                            d == null ? null : d.description(),
                            d == null ? null : d.defaultValue(),
                            d == null ? null : d.validatorRegularExpression(),
                            field.getAnnotation(GlobalConfigValidation.class));
                    GlobalConfigDescription existing = descriptions.get(desc.getIdentity());
                    if (existing != null && existing.type != null && d != null) {
                        throw new IllegalArgumentException(String.format(
                                "duplicate global configuration. %s defines a" +
                                        " global config[category: %s, name: %s] that has been defined by another java class",
                                def.getName(), desc.category, desc.name));
                    }
                    if (existing == null || d != null) {
                        descriptions.put(desc.getIdentity(), desc);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return new ArrayList<>(descriptions.values());
    }

    private static List<GlobalConfigDescription> loadConfigFromXml() {
        Map<String, GlobalConfigDescription> fromXml = new LinkedHashMap<>();
        List<String> filePaths = PathUtil.scanFolderOnClassPath(CONFIG_FOLDER);
        for (String path : filePaths) {
            File file = new File(path);
            if (!file.getName().endsWith(".xml")) {
                logger.warn(String.format(
                        "file[%s] in global config folder is not end with .xml, skip it",
                        file.getAbsolutePath()));
                continue;
            }
            GlobalConfigXml gb;
            try {
                gb = GlobalConfigXml.unmarshal(file);
            } catch (JAXBException e) {
                throw new IllegalStateException(String.format(
                        "unable to unmarshal global config xml[%s]", file.getAbsolutePath()), e);
            }
            for (ConfigXmlCell cell : gb.getConfig()) {
                String category = cell.category == null ? OTHER_CATEGORY : cell.category;
                requireXmlRequiredFields(cell, category, file.getAbsolutePath());
                GlobalConfigDescription d = new GlobalConfigDescription(
                        category,
                        cell.name,
                        xmlTypeName(cell, category, file.getAbsolutePath()),
                        cell.description,
                        cell.defaultValue,
                        cell.validatorRegularExpression,
                        null);
                if (fromXml.containsKey(d.getIdentity())) {
                    throw new IllegalArgumentException(String.format(
                            "duplicate GlobalConfig[category: %s, name: %s]", d.category, d.name));
                }
                fromXml.put(d.getIdentity(), d);
            }
        }
        return new ArrayList<>(fromXml.values());
    }

    private static void requireXmlRequiredFields(ConfigXmlCell cell, String category, String filePath) {
        if (cell.name == null) {
            throw new IllegalArgumentException(String.format(
                    "GlobalConfig[category:%s] in %s must have a name",
                    category, filePath));
        }
        if (cell.defaultValue == null) {
            throw new IllegalArgumentException(String.format(
                    "GlobalConfig[category:%s, name:%s] in %s must have a default value",
                    category, cell.name, filePath));
        }
    }

    private static String xmlTypeName(ConfigXmlCell cell, String category, String filePath) {
        String raw = cell.type == null ? null : cell.type.trim();
        if (raw == null || raw.isEmpty()) {
            return GlobalConfigValueType.STRING.getJavaClassName();
        }
        try {
            return GlobalConfigValueType.parse(raw).getJavaClassName();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format(
                    "GlobalConfig[category:%s, name:%s] in %s has invalid type[%s]",
                    category, cell.name, filePath, raw), e);
        }
    }
}
