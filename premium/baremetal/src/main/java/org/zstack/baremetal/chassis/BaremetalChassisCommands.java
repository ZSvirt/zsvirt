package org.zstack.baremetal.chassis;

/**
 * Created by GuoYi on 6/23/17.
 */
public class BaremetalChassisCommands {
    public static class SendHardwareInfoCmd {
        // FIX ZSTAC-37078
        // if three ipmi address in found inside one chassis, then ipmiAddress is like "1.1.1.1 | 2.2.2.2 | 3.3.3.3";
        public String ipmiAddress;
        public Integer ipmiPort;
        public String type;
        public String content;
    }
}
