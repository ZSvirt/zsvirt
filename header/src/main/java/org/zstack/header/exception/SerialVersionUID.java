package org.zstack.header.exception;

public interface SerialVersionUID {
    public static final long Base = (long) 0x564D4F70 << 32;

    public static final long CloudRuntimeException = Base | 0x01;
    public static final long CloudConfigureFailException = Base | (0x01 << 1);
    public static final long CloudInvalidParameterException = Base | (0x01 << 3);
    public static final long CloudUnkownMessageException = Base | (0x01 << 4);
    public static final long CloudPluginMissDependencyException = Base | (0x01 << 5);
    public static final long CloudStateMachineException = Base | (0x01 << 5);
    public static final long CloudCheckPointException = Base | (0x01 << 6);
    public static final long CloudCheckPointExecutionException = Base | (0x01 << 7);
    public static final long CloudErrorCodeExistingException = Base | (0x01 << 8);
}
