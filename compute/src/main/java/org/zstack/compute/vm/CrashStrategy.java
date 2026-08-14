package org.zstack.compute.vm;

/**
 * Created by LiangHanYu on 2021/6/18 11:05
 */
public enum CrashStrategy {
    None,
    Preserve,
    Reboot,
    Shutdown;
    
    public boolean isCrashStrategyEnable() {
        return this != None;
    }
}
