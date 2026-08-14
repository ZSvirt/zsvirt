package org.zstack.identity.rbac;

import org.zstack.header.identity.rbac.RBAC;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.BASE_PACKAGE_NAME;

public class PolicyUtils {
    public static boolean isAdminOnlyAction(String action) {
        return RBAC.isAdminOnlyAPI(action);
    }

    /**
     * Example:<br/>
     *
     * input: "org.zstack.header.vm.APIStartVmInstanceMsg"<br/>
     * output [".**", ".header.**", ".header.vm.**", ".header.vm.*", ".header.vm.APIStartVmInstanceMsg"]<br/>
     */
    public static List<String> findAllMatchedApiPatterns(String api) {
        List<String> results = new ArrayList<>();

        if (!api.startsWith(BASE_PACKAGE_NAME)) {
            results.add(api);
            return results;
        }

        api = api.substring(BASE_PACKAGE_NAME.length()); // start without "."
        String[] split = api.split("\\.");
        results.add(".**");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < split.length - 1; i++) {
            builder.append(".").append(split[i]);
            results.add(builder + ".**");
        }

        results.add(builder + ".*");
        results.add("." + api);
        return results;
    }
}
