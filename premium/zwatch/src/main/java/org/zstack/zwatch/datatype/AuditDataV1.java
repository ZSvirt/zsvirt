package org.zstack.zwatch.datatype;

import org.zstack.core.db.Q;
import org.zstack.header.identity.SessionVO;
import org.zstack.header.identity.SessionVO_;
import org.zstack.utils.DebugUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AuditDataV1 extends AuditData {
    public static final String TAG_RESOURCE_UUID = "resourceUuid";
    public static final String TAG_RESOURCE_TYPE = "resourceType";
    public static final String TAG_API_NAME = "apiName";
    public static final String TAG_API_ERROR = "error";
    public static final String TAG_API_OPERATOR_ACCOUNT_UUID = "operatorAccountUuid";
    public static final String TAG_API_CLIENTIP = "clientIp";
    public static final String TAG_API_CLIENTBROWSER = "clientBrowser";

    public static Set<String> queryableLabels = new HashSet<>();
    public static Set<String> queryableLoginLabels = new HashSet<>();

    static {
        queryableLabels.add(TAG_RESOURCE_UUID);
        queryableLabels.add(TAG_RESOURCE_TYPE);
        queryableLabels.add(TAG_API_NAME);
        queryableLabels.add(TAG_API_ERROR);
        queryableLabels.add(TAG_API_OPERATOR_ACCOUNT_UUID);
        queryableLoginLabels.add(TAG_API_CLIENTIP);
        queryableLoginLabels.add(TAG_API_CLIENTBROWSER);
        queryableLoginLabels.add(TAG_API_NAME);
        queryableLoginLabels.add(TAG_API_ERROR);
        queryableLoginLabels.add(TAG_API_OPERATOR_ACCOUNT_UUID);
    }

    public static AuditDataV2 __example__() {
        AuditDataV2 ret = new AuditDataV2();
        ret.setResourceUuid("b1ca3fab6ebc4302ad9249d5402cb06c");
        ret.setResourceType("VmInstanceVO");
        ret.setClientIp("172.0.0.1");
        ret.setClientBrowser("Chrome/69.0.3497.81");
        ret.setApiName("org.zstack.header.vm.APISetVmRDPMsg");
        ret.setOperatorAccountUuid("36c27e8ff05c4780bf6d2fa65700f22e");
        ret.setDuration(22);
        ret.setRequestUuid("d78093c0b2fb3850b3ce563b3a0b029d");
        ret.setResponseUuid("08cf3fd2c4614811b4acc0620e20a98e");
        ret.setSessionUuid("05e5c663eb794faab5c5606c38103a17");
        ret.setResponseDump("{\"enable\":false,\"uuid\":\"b1ca3fab6ebc4302ad9249d5402cb06c\"}");
        ret.setResponseDump("{\"success\":true}");
        ret.setOperator("admin");
        ret.setTime(1512037844556L);
        return ret;
    }

    @Override
    public Map<String, String> asTags() {
        DebugUtils.Assert(resourceUuid != null, "resourceUuid cannot be null");
        DebugUtils.Assert(resourceType != null, "resourceType cannot be null");
        DebugUtils.Assert(apiName != null, "apiName cannot be null");

        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_RESOURCE_UUID, resourceUuid);
        tags.put(TAG_RESOURCE_TYPE, resourceType);
        tags.put(TAG_API_NAME, apiName);
        if (error != null) {
            tags.put(TAG_API_ERROR, error);
        }

        if (operatorAccountUuid == null) {
            // the API is failed at API param check, where the accountUuid is not set yet
            operatorAccountUuid = Q.New(SessionVO.class).select(SessionVO_.accountUuid)
                    .eq(SessionVO_.uuid, sessionUuid).findValue();
        }

        DebugUtils.Assert(operatorAccountUuid != null, "operatorAccountUuid cannot be null");
        DebugUtils.Assert(clientIp != null, "ip cannot be null");
        DebugUtils.Assert(clientBrowser != null, "browser cannot be null");
        tags.put(TAG_API_OPERATOR_ACCOUNT_UUID, operatorAccountUuid);
        tags.put(TAG_API_CLIENTIP, clientIp);
        tags.put(TAG_API_CLIENTBROWSER, clientBrowser);
        return tags;
    }

    @Override
    public Map<String, Object> asFields() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("duration", duration);
        fields.put("requestUuid", requestUuid);
        fields.put("responseUuid", responseUuid);
        fields.put("sessionUuid", sessionUuid);
        fields.put("requestDump", requestDump);
        fields.put("responseDump", responseDump);
        fields.put("operator",operator);
        return fields;
    }
}
