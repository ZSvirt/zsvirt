package org.zstack.header.baremetal.instance;

/**
 * Created by GuoYi on 2018-10-17.
 */
public interface BaremetalInstanceStaticConfigManager {
    void writeNoVNCProxy(String bmUuid);
    void writeTerminalProxy(String bmUuid);

    void deleteNginxProxy(String bmUuid);
}
