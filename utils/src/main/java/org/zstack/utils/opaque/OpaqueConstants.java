package org.zstack.utils.opaque;

public final class OpaqueConstants {
    private OpaqueConstants() {
    }

    public static final String OPAQUE_KEY_ERROR_LOCATION = "error.location";
    public static final String OPAQUE_KEY_TEMPLATE = "template";
    public static final String OPAQUE_KEY_ARG0 = "arg.0";
    public static final String OPAQUE_KEY_ARG1 = "arg.1";
    public static final String OPAQUE_KEY_ARG2 = "arg.2";
    public static final String OPAQUE_KEY_ARG3 = "arg.3";
    public static final String OPAQUE_KEY_ARG4 = "arg.4";
    public static final String OPAQUE_KEY_ARG5 = "arg.5";
    public static final String OPAQUE_KEY_ARG6 = "arg.6";
    public static final String OPAQUE_KEY_ARG7 = "arg.7";
    public static final String OPAQUE_KEY_ARG8 = "arg.8";
    public static final String OPAQUE_KEY_ARG9 = "arg.9";
    public static final String OPAQUE_KEY_ARG_PREFIX = "arg.";

    public static final String opaqueKeyForArg(int index) {
        switch (index) {
        case 0: return OPAQUE_KEY_ARG0;
        case 1: return OPAQUE_KEY_ARG1;
        case 2: return OPAQUE_KEY_ARG2;
        case 3: return OPAQUE_KEY_ARG3;
        case 4: return OPAQUE_KEY_ARG4;
        case 5: return OPAQUE_KEY_ARG5;
        case 6: return OPAQUE_KEY_ARG6;
        case 7: return OPAQUE_KEY_ARG7;
        case 8: return OPAQUE_KEY_ARG8;
        case 9: return OPAQUE_KEY_ARG9;
        default: return OPAQUE_KEY_ARG_PREFIX + index;
        }
    }

    public static final boolean isOpaqueKey(String key) {
        return key.startsWith(OPAQUE_KEY_ARG_PREFIX);
    }

    public static final String OPAQUE_KEY_EXCEPTION = "exception";

    public static final String OPAQUE_KEY_BASH_CMD = "bash.cmd";
    public static final String OPAQUE_KEY_BASH_CODE = "bash.code";
    public static final String OPAQUE_KEY_BASH_OUTPUT = "bash.output";
    public static final String OPAQUE_KEY_BASH_ERROR = "bash.error";
    public static final String OPAQUE_KEY_SSH_CMD = "ssh.cmd";
    public static final String OPAQUE_KEY_SSH_CODE = "ssh.code";
    public static final String OPAQUE_KEY_SSH_OUTPUT = "ssh.output";
    public static final String OPAQUE_KEY_SSH_ERROR = "ssh.error";

    public static final String OPAQUE_KEY_RESPONSE_OUTPUT = "response.output";
    public static final String OPAQUE_KEY_RESPONSE_ERROR = "response.error";

    public static final String OPAQUE_KEY_PARAMETER_NAME = "parameter.name";
}
