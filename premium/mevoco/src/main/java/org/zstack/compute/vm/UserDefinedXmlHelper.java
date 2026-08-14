package org.zstack.compute.vm;

import org.apache.commons.codec.binary.Base64;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelInventory;

public class UserDefinedXmlHelper {
    public static JsonLabelInventory getUserDefinedVmXmlBase64(String vmUuid) {
        String labelKey = getUserDefinedVmXmlLabelKey(vmUuid);
        if (! new JsonLabel().exists(labelKey)) {
            return null;
        }

        return new JsonLabel().get(labelKey);
    }

    public static String getUserDefinedVmXml(String vmUuid) {
        JsonLabelInventory label = getUserDefinedVmXmlBase64(vmUuid);
        if (label == null) {
            return null;
        }

        String xmlBase64 = label.getLabelValue();
        return new String(Base64.decodeBase64(xmlBase64));
    }

    public static void removeUserDefinedVmXmlIfExists(String vmUuid) {
        JsonLabelInventory label = getUserDefinedVmXmlBase64(vmUuid);
        if (label != null) {
            new JsonLabel().delete(getUserDefinedVmXmlLabelKey(vmUuid));
        }
    }

    public static boolean VmXmlHookScriptExists(String vmUuid) {
        String labelKey = String.format("user-defined-xml-hook-script-%s", vmUuid);
        if (! new JsonLabel().exists(labelKey)) {
            return false;
        }
        return true;
    }

    public static String getUserDefinedVmXmlLabelKey(String vmUuid) {
        return String.format("user-defined-xml-%s", vmUuid);
    }
}
