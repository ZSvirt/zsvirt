package org.zstack.core.config;

import org.zstack.utils.BeanUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Stateless scan of {@link GlobalConfigDefinition} holder classes.
 * Static fields are read as {@link ConfigDefinition}, not {@code GlobalConfig}.
 */
public final class GlobalConfigDefinitionScanner {
    private GlobalConfigDefinitionScanner() {
    }

    public static List<GlobalConfigDescription> scan() {
        return parseGlobalConfigFields();
    }

    private static List<GlobalConfigDescription> parseGlobalConfigFields() {
        List<GlobalConfigDescription> descriptions = new ArrayList<>();
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
                    descriptions.add(new GlobalConfigDescription(
                            config.getCategory(),
                            config.getName(),
                            field.getAnnotation(GlobalConfigDef.class),
                            field.getAnnotation(GlobalConfigValidation.class)));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return descriptions;
    }
}
