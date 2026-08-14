package org.zstack.monitoring.items;

import org.zstack.utils.DebugUtils;
import static org.zstack.core.Platform.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2017/8/3.
 */
public class AlertText {
    public String resourceName;
    public String resourceType;
    public String threshold;
    public String operator;
    public String triggerUuid;
    public String value;
    public boolean problem;
    public String itemName;
    public String resourceUuid;
    public Map<String, String> args;

    @Override
    public String toString() {
        DebugUtils.Assert(resourceName!=null, "resourceName cannot be null");
        DebugUtils.Assert(resourceType!=null, "resourceType cannot be null");
        DebugUtils.Assert(threshold!=null, "threshold cannot be null");
        DebugUtils.Assert(operator!=null, "operator cannot be null");
        DebugUtils.Assert(triggerUuid!=null, "triggerUuid cannot be null");
        DebugUtils.Assert(value!=null, "value cannot be null");
        DebugUtils.Assert(itemName!=null, "itemName cannot be null");
        DebugUtils.Assert(resourceUuid!=null, "resourceUuid cannot be null");

        Map<String, String> args = new HashMap<>();
        args.put("resourceName", resourceName);
        args.put("resourceType", resourceType);
        args.put("threshold", threshold);
        args.put("operator", operator);
        args.put("triggerUuid", triggerUuid);
        args.put("value", value);
        args.put("resourceUuid", resourceUuid);
        args.put("triggerStatus", problem ? "Problem" : "OK");
        args.put("itemName", itemName);
        if (this.args != null) {
            args.putAll(this.args);
        }

        StringBuilder sb = new StringBuilder();
        String head = i18n("A resource[name:{resourceName}, uuid:{resourceUuid}, type:{resourceType}]'s monitoring trigger[uuid:{triggerUuid}] changes" +
                " status to {triggerStatus}", args);
        sb.append(head);

        if (!problem) {
            sb.append(i18n("\n=== BELOW ARE DETAILS OF THE PREVIOUS ALERT ==="));
        }

        sb.append(i18n("\nalert details:"));
        sb.append(i18n("\ncondition: {itemName} {operator} {threshold}", args));
        sb.append(i18n("\ncurrent value: {value}", args));
        if (this.args != null) {
            for (Map.Entry<String, String> e : this.args.entrySet()) {
                sb.append(i18n(String.format("\n%s: %s", e.getKey(), e.getValue())));
            }
        }

        return sb.toString();
    }
}
