package org.zstack.core.db;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public interface DbEntityLister {
    <T> List<T> listAll(Class<T> clazz);
    
    <T> List<T> listAll(int offset, int length, Class<T> clazz);
    
    <T> List<T> listByUuids(List<String> uuids, Class<T> clazz);
    
    <T> List<T> listByUuids(List<String> uuids, int offset, int length, Class<T> clazz);

    <T> List<T> listByApiMessage(APIListMessage msg, Class<T> clazz);
}
