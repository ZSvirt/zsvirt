package org.zstack.header.vm;

import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeState;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

import static java.util.Arrays.asList;

@RestResponse(fieldsTo = "all")
public class APISetVmUserDefinedXmlHookScriptEvent extends APIEvent {
    private String vmUserDefinedXmlHookScript;

    public String getVmUserDefinedXmlHookScript() {
        return vmUserDefinedXmlHookScript;
    }

    public void setVmUserDefinedXmlHookScript(String vmUserDefinedXmlHookScript) {
        this.vmUserDefinedXmlHookScript = vmUserDefinedXmlHookScript;
    }

    public APISetVmUserDefinedXmlHookScriptEvent() {
    }

    public APISetVmUserDefinedXmlHookScriptEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmUserDefinedXmlHookScriptEvent __example__() {
        APISetVmUserDefinedXmlHookScriptEvent evt = new APISetVmUserDefinedXmlHookScriptEvent();

        evt.vmUserDefinedXmlHookScript =
                "def config_cpu_mode(root, cpu_mode, hook):\n" +
                "    for cpu in root.findall(\"cpu\"):\n" +
                "        hook.modify_value_of_attribute(cpu, \"mode\", cpu_mode)\n" +
                "\n" +
                "config_cpu_mode(root, \"host-model\", hook)\n";

        return evt;
    }
}
