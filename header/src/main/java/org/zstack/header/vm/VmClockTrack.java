package org.zstack.header.vm;

public enum VmClockTrack {
    guest,
    host;

    public static VmClockTrack get(String value) {
        try {
            return VmClockTrack.valueOf(value);
        } catch (Exception e){
            return null;
        }
    }
}
