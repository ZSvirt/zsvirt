package org.zstack.zwatch.datatype;

import org.zstack.core.Platform;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ApiAuditor {
    protected Class apiClass;

    static Map<Class, List<ApiAuditor>> audits = new HashMap<>();

    public static List<ApiAuditor> getApiAuditors(Class clz) {
        return audits.get(clz);
    }

    public static class Result {
        public String resourceUuid;
        public Class resourceType;

        public Result(String resourceUuid, Class resourceType) {
            this.resourceUuid = resourceUuid;
            this.resourceType = resourceType;
        }
    }

    public ApiAuditor(Class apiClass) {
        if (!APIMessage.class.isAssignableFrom(apiClass)) {
            throw new CloudRuntimeException(String.format("%s is not an APIMessage", apiClass));
        }
        this.apiClass = apiClass;
    }

    public abstract Result audit(APIMessage msg, APIEvent rsp);

    public static void register(ApiAuditor auditor) {
        if (audits.containsKey(auditor.apiClass)) {
            audits.forEach((clz, lst) -> {
                if (auditor.apiClass.isAssignableFrom(clz)) {
                    lst.add(auditor);
                }
            });

            return;
        }

        List<Class> classes = new ArrayList<>();
        classes.add(auditor.apiClass);
        classes.addAll(Platform.getReflections().getSubTypesOf(auditor.apiClass));

        classes.forEach(clz -> {
            List<ApiAuditor> lst = audits.computeIfAbsent(clz, k->new ArrayList<>());
            lst.add(auditor);
        });
    }

    public void register() {
        register(this);
    }
}
