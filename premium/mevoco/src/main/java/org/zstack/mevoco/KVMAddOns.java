package org.zstack.mevoco;

/**
 * Created by frank on 9/20/2015.
 */
public class KVMAddOns {
    public static class VolumeQos {
        public String uuid;
        public Long totalBandwidth;
        public Long totalIops;
    }

    public static class NicQos {
        public String uuid;
        public Long outboundBandwidth;
        public Long inboundBandwidth;
    }
}
