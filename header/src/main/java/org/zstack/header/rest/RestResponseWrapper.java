package org.zstack.header.rest;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RestResponseWrapper {
    public RestResponse annotation;
    public Map<String, String> responseMappingFields = new HashMap<>();
    public Class apiResponseClass;

    public RestResponseWrapper(RestResponse annotation, Class apiResponseClass) {
        this.annotation = annotation;
        this.apiResponseClass = apiResponseClass;

        if (annotation.fieldsTo().length > 0) {
            responseMappingFields = new HashMap<>();

            if (annotation.fieldsTo().length == 1 && "all".equals(annotation.fieldsTo()[0])) {
                List<Field> apiFields = FieldUtils.getAllFields(apiResponseClass);
                apiFields = apiFields.stream().filter(f -> !f.isAnnotationPresent(APINoSee.class) && !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList());

                for (Field f : apiFields) {
                    responseMappingFields.put(f.getName(), f.getName());
                }
            } else {
                for (String mf : annotation.fieldsTo()) {
                    String[] kv = mf.split("=");
                    if (kv.length == 2) {
                        responseMappingFields.put(kv[0].trim(), kv[1].trim());
                    } else if (kv.length == 1) {
                        responseMappingFields.put(kv[0].trim(), kv[0].trim());
                    } else {
                        throw new CloudRuntimeException(String.format("bad mappingFields[%s] of %s", mf, apiResponseClass));
                    }
                }
            }
        }
    }
}

