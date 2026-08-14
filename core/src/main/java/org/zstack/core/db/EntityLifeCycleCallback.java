package org.zstack.core.db;

/**
 * Created by xing5 on 2016/3/12.
 */
public interface EntityLifeCycleCallback {
    void entityLifeCycleEvent(EntityEvent evt, Object o);
}
