package org.zstack.core.keyvalue;

import org.zstack.header.core.keyvalue.KeyValueEntity;

/**
 */
public interface KeyValueFacade {
    void persist(KeyValueEntity entity);

    void update(KeyValueEntity entity);

    void delete(String uuid);

    void delete(KeyValueEntity entity);

    <T> T find(String uuid);
}
