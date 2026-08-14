package org.zstack.header.message;

import org.apache.commons.beanutils.PropertyUtils;
import org.zstack.header.exception.CloudRuntimeException;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public interface APIResponse {
    Map<Class, Map<String, String>> responseMappingFields = new HashMap<>();

    default LinkedHashMap toResponseMap(APIResponse rsp) {
        LinkedHashMap ret = new LinkedHashMap();

        Map<String, String> mapping = responseMappingFields.get(rsp.getClass());
        if (mapping == null) {
            throw new CloudRuntimeException(String.format("cannot find response mapping for API reply[%s]", rsp.getClass()));
        }

        mapping.forEach((k, v) -> {
            try {
                ret.put(k, PropertyUtils.getProperty(rsp, v));
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        });

        return ret;
    }
}
