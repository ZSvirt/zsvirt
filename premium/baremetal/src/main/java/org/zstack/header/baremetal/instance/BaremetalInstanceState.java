package org.zstack.header.baremetal.instance;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by GuoYi on 7/4/18.
 */
@PythonClass
public enum BaremetalInstanceState {
    Created,
    Starting,
    Running,
    Stopped,
    Rebooting,
    Destroyed,
    UNKNOWN,
    Error,
}