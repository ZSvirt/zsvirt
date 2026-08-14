package org.zstack.query;

import org.zstack.header.query.APIQueryMessage;

import java.util.List;

public interface MysqlQueryExtensionPoint {
    <T> List<T> query(APIQueryMessage msg, Class<T> inventoryClass);

    long count(APIQueryMessage msg, Class inventoryClass);
    
    List<Class<?>> getSupportQueryMessageClasses();
}
