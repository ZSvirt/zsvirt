package org.zstack.ha.message;

import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by xing5 on 2016/3/28.
 */
public class CheckerStruct {
    public String hostUuid;
    public VmInstanceInventory vmInstance;
    public long interval;
    public int maxTimes;
    public long successInterval;
    public int successTimes;
    public String hostIp;
}
