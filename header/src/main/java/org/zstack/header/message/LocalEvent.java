package org.zstack.header.message;

abstract public class LocalEvent extends Event {
    @NoJsonSchema
    private Type type = null;

    @Override
    public final Type getType() {
        if (type == null) {
            type = new Type(Event.Category.LOCAL, getSubCategory());
        }
        return type;
    }
}
