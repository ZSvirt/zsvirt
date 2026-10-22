package org.zstack.test.unittest.compute;

import org.junit.Test;
import org.zstack.header.message.APIParam;
import org.zstack.header.vm.APIRegisterVmInstanceFromMetadataMsg;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class VmMetadataRegistrationInputTest {
    private static final String VM_UUID = "11111111111111111111111111111111";

    @Test
    public void inlineRequestRoutesWithoutMetadataPath() {
        APIRegisterVmInstanceFromMetadataMsg msg = inline("{\"vm\":{\"resourceUuid\":\"" + VM_UUID + "\"}}");
        assertEquals("ps-" + VM_UUID + ".vmmeta", registrationKey(msg));
    }

    @Test
    public void equivalentInlineRequestsShareThePathModeRegistrationQueue() {
        APIRegisterVmInstanceFromMetadataMsg path = new APIRegisterVmInstanceFromMetadataMsg();
        path.setPrimaryStorageUuid("ps");
        path.setMetadataPath("/storage/vm-metadata/" + VM_UUID + ".vmmeta");
        APIRegisterVmInstanceFromMetadataMsg compact = inline("{\"vm\":{\"resourceUuid\":\"" + VM_UUID + "\"}}");
        APIRegisterVmInstanceFromMetadataMsg spaced = inline("{ \"vm\" : { \"resourceUuid\" : \"" + VM_UUID + "\" }, \"volumes\" : [] }");
        assertEquals(path.getMetadataRegistrationKey(), registrationKey(compact));
        assertEquals(registrationKey(compact), registrationKey(spaced));
    }

    @Test
    public void pathIsOptionalSoInlineRequestsReachTheInterceptor() throws Exception {
        APIParam param = APIRegisterVmInstanceFromMetadataMsg.class.getDeclaredField("metadataPath").getAnnotation(APIParam.class);
        assertFalse("metadataPath must not be required before input-mode validation", param.required());
    }

    private APIRegisterVmInstanceFromMetadataMsg inline(String metadata) {
        Map<String, Object> request = new HashMap<>();
        request.put("primaryStorageUuid", "ps");
        request.put("metadata", metadata);
        return JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(request), APIRegisterVmInstanceFromMetadataMsg.class);
    }

    private String registrationKey(APIRegisterVmInstanceFromMetadataMsg msg) {
        try {
            return msg.getMetadataRegistrationKey();
        } catch (NullPointerException e) {
            fail("inline metadata must route by VM identity without dereferencing metadataPath");
            return null;
        }
    }
}
