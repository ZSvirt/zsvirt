package org.zstack.storage.device.hba;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/9/18 15:48
 */
public enum PortState {
    Online,
    Linkdown,
    Offline,
    Loopback,
    Testing,
    Error,
    Initializing,
    Unknown
}
