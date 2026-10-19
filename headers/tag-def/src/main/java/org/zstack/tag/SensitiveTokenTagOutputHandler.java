package org.zstack.tag;

import java.util.Map;

public class SensitiveTokenTagOutputHandler implements SensitiveTagOutputHandler {
    @Override
    public String desensitizeTag(SystemTag systemTag, String tag) {
        if (!systemTag.hasPattern || systemTag.annotation == null) {
            return tag;
        }

        String[] sensitiveTokens = systemTag.annotation.tokens();
        if (sensitiveTokens == null || sensitiveTokens.length == 0) {
            return tag;
        }

        Map<String, String> tokens = systemTag.getTokensByTag(tag);
        if (tokens == null || tokens.isEmpty()) {
            return tag;
        }

        for (String t : sensitiveTokens) {
            tokens.put(t, "*****");
        }
        return systemTag.instantiateTag(tokens);
    }
}
