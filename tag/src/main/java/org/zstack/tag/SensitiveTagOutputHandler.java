package org.zstack.tag;

public interface SensitiveTagOutputHandler {
    default String desensitizeTag(SystemTag systemTag, String tag) {
        return tag;
    }
}