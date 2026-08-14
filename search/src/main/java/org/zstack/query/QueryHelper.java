package org.zstack.query;

import org.zstack.header.core.StaticInit;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.utils.BeanUtils;

import java.util.HashMap;
import java.util.Map;

public class QueryHelper {
    public static Map<Class, Class> inventoryQueryMessageMap = new HashMap<Class, Class>();

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getSubTypesOf(APIQueryMessage.class).forEach(msgClass -> {
            AutoQuery at = msgClass.getAnnotation(AutoQuery.class);
            if (at == null) {
                return;
            }

            inventoryQueryMessageMap.put(at.inventoryClass(), msgClass);
        });
    }
}
