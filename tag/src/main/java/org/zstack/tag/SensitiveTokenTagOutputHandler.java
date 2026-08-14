package org.zstack.tag;

import java.util.Map;

public class SensitiveTokenTagOutputHandler implements SensitiveTagOutputHandler {
    @Override
    public String desensitizeTag(SystemTag systemTag, String tag) {
        if (!(systemTag instanceof PatternedSystemTag)) {
            return tag;
        }
        PatternedSystemTag patternedSystemTag = (PatternedSystemTag) systemTag;

        String[] sensitiveTokens = patternedSystemTag.annotation.tokens();
        if (sensitiveTokens == null || sensitiveTokens.length == 0) {
            return tag;
        }

        Map<String, String> tokens = patternedSystemTag.getTokensByTag(tag);
        if (tokens == null || tokens.isEmpty()) {
            return tag;
        }

        for (String t : sensitiveTokens) {
            tokens.put(t, "*****");
        }
        return patternedSystemTag.instantiateTag(tokens);
    }
}
