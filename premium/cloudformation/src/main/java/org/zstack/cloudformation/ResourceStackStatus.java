package org.zstack.cloudformation;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
public enum ResourceStackStatus {
    Initial,
    Created,
    Failed,
    Creating,
    Deleting,
    Deleted,
    Rollbacking,
    Rollbacked
}
