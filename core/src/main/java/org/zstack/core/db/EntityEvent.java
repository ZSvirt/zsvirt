package org.zstack.core.db;

/**
 * Created by xing5 on 2016/3/12.
 */
public enum EntityEvent {
    PRE_PERSIST,
    POST_PERSIST,
    POST_LOAD,
    PRE_UPDATE,
    POST_UPDATE,
    PRE_REMOVE,
    POST_REMOVE
}
