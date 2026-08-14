package org.zstack.zwatch.datatype;

import org.zstack.core.db.Q;
import org.zstack.header.identity.SessionVO;
import org.zstack.header.identity.SessionVO_;
import org.zstack.header.rest.SDK;
import org.zstack.utils.DebugUtils;
import org.zstack.zwatch.ZWatchConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SDK
public class AuditDataV2 extends AuditData {
    public static final String TAG_RESOURCE_UUID = "resourceUuid";
    public static final String TAG_API_OPERATOR_ACCOUNT_UUID = "operatorAccountUuid";
    public static final String TAG_SUCCESS = "success";


    public static final String FIELD_API_ERROR = "error";
    public static final String FIELD_API_CLIENTIP = "clientIp";
    public static final String FIELD_API_CLIENTBROWSER = "clientBrowser";
    public static final String FIELD_RESOURCE_TYPE = "resourceType";
    public static final String FIELD_API_NAME = "apiName";
    public static final String FIELD_REQUEST_UUID = "requestUuid";


    public static Set<String> queryableLabels = new HashSet<>();
    public static Set<String> queryableLoginLabels = new HashSet<>();

    static {
        queryableLabels.add(TAG_RESOURCE_UUID);
        queryableLabels.add(TAG_API_OPERATOR_ACCOUNT_UUID);
        queryableLabels.add(FIELD_API_ERROR);
        queryableLabels.add(FIELD_RESOURCE_TYPE);
        queryableLabels.add(FIELD_API_NAME);
        queryableLabels.add(FIELD_REQUEST_UUID);

        queryableLoginLabels.add(TAG_API_OPERATOR_ACCOUNT_UUID);
        queryableLoginLabels.add(FIELD_API_NAME);
        queryableLoginLabels.add(FIELD_API_CLIENTIP);
        queryableLoginLabels.add(FIELD_API_CLIENTBROWSER);
        queryableLoginLabels.add(FIELD_API_ERROR);

    }

    private String success = ZWatchConstants.AUDIT_SUCCESS;

    protected String resourceName;

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
        ret.setSuccess(ZWatchConstants.AUDIT_SUCCESS);
        ret.setTime(1512037844556L);
        return ret;
    }

    @Override
    public Map<String, String> asTags() {
        DebugUtils.Assert(resourceUuid != null, "resourceUuid cannot be null");

        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_RESOURCE_UUID, resourceUuid);

        if (operatorAccountUuid == null) {
            // the API is failed at API param check, where the accountUuid is not set yet
            operatorAccountUuid = Q.New(SessionVO.class).select(SessionVO_.accountUuid)
                    .eq(SessionVO_.uuid, sessionUuid).findValue();
        }

        DebugUtils.Assert(operatorAccountUuid != null, "operatorAccountUuid cannot be null");

        tags.put(TAG_API_OPERATOR_ACCOUNT_UUID, operatorAccountUuid);
        if (error == null) {
            tags.put(TAG_SUCCESS, ZWatchConstants.AUDIT_SUCCESS);
        } else {
            tags.put(TAG_SUCCESS, ZWatchConstants.AUDIT_FAILED);
        }
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
        DebugUtils.Assert(clientIp != null, "ip cannot be null");
        DebugUtils.Assert(clientBrowser != null, "browser cannot be null");
        DebugUtils.Assert(apiName != null, "apiName cannot be null");
        fields.put(FIELD_API_CLIENTIP, clientIp);
        fields.put(FIELD_API_CLIENTBROWSER, clientBrowser);
        fields.put(FIELD_API_NAME, apiName);

        DebugUtils.Assert(resourceType != null, "resourceType cannot be null");
        fields.put(FIELD_RESOURCE_TYPE, resourceType);

        if (error != null) {
            fields.put(FIELD_API_ERROR, error);
        }
        return fields;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    private static Set<String> tagsFromV1 = new HashSet<>();
    static {
        tagsFromV1.add(FIELD_RESOURCE_TYPE);
        tagsFromV1.add(FIELD_API_NAME);
        tagsFromV1.add(FIELD_API_CLIENTIP);
        tagsFromV1.add(FIELD_API_CLIENTBROWSER);
        tagsFromV1.add(FIELD_API_ERROR);
    }
    public static Set<String> tagToFieldFromV1() {
        return tagsFromV1;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}
