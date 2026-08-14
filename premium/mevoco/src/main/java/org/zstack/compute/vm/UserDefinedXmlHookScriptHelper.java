package org.zstack.compute.vm;

import org.apache.commons.codec.binary.Base64;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelInventory;

public class UserDefinedXmlHookScriptHelper {
    public static JsonLabelInventory getUserDefinedVmXmlHookScriptBase64(String vmUuid) {
        String labelKey = getUserDefinedVmXmlHookScriptLabelKey(vmUuid);
        if (! new JsonLabel().exists(labelKey)) {
            return null;
        }

        return new JsonLabel().get(labelKey);
    }

    public static String getUserDefinedVmXmlHookScript(String vmUuid) {
        JsonLabelInventory label = getUserDefinedVmXmlHookScriptBase64(vmUuid);
        if (label == null) {
            return null;
        }

        String xmlHookScriptBase64 = label.getLabelValue();
        return new String(Base64.decodeBase64(xmlHookScriptBase64));
    }

    public static void removeUserDefinedVmXmlHookScriptIfExists(String vmUuid) {
        JsonLabelInventory label = getUserDefinedVmXmlHookScriptBase64(vmUuid);
        if (label != null) {
            new JsonLabel().delete(getUserDefinedVmXmlHookScriptLabelKey(vmUuid));
        }
    }

    public static boolean VmUserDefinedXmlExists(String vmUuid) {
        String labelKey = String.format("user-defined-xml-%s", vmUuid);
        if (! new JsonLabel().exists(labelKey)) {
            return false;
        }
        return true;
    }

    public static String getUserDefinedVmXmlHookScriptLabelKey(String vmUuid) {
        return String.format("user-defined-xml-hook-script-%s", vmUuid);
    }
}
