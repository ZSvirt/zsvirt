package org.zstack.abstraction.header;

public class InvalidPluginDefinitionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidPluginDefinitionException() {
        super();
    }

    public InvalidPluginDefinitionException(String message) {
        super(message);
    }

    public InvalidPluginDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPluginDefinitionException(Throwable cause) {
        super(cause);
    }
}
