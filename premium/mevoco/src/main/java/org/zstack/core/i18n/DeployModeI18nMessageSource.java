package org.zstack.core.i18n;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.HashSet;
import java.util.Set;

public class DeployModeI18nMessageSource extends ReloadableResourceBundleMessageSource {
    private static Set<String> basenameSet = new HashSet<>();

    static {
        basenameSet.add("i18n/messages");
        basenameSet.add("i18n/permission_name");
    }

    @NotNull
    @Override
    public Set<String> getBasenameSet() {
        return basenameSet;
    }
}
