package org.zstack.pciDevice.KvmPciDeviceBackend;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.header.tag.FormTagExtensionPoint;
import org.zstack.kvm.AddKVMHostMsg;
import org.zstack.utils.function.Function;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: kefeng.wang
 * @date: 2018-11-16
 **/
public class PageTableExtensionBackend implements FormTagExtensionPoint {
    private static final Map<String, Boolean> textToBoolMap = ImmutableMap
            .<String, Boolean>builder()
            .put("n", false)
            .put("no", false)
            .put("disabled", false)
            .put("y", true)
            .put("yes", true)
            .put("enabled", true)
            .build();

    public static Boolean getTextBool(String boolText, Boolean defValue) {
        Boolean textBool = null;
        if (StringUtils.isNotEmpty(boolText)) {
            textBool = textToBoolMap.get(StringUtils.lowerCase(boolText));
        }

        return (textBool == null) ? defValue : textBool;
    }

    @Override
    public Map<String, Function<String, String>> getTagMappers(Class clz) {
        if (!AddKVMHostMsg.class.isAssignableFrom(clz)) {
            return new HashMap<>();
        }

        return Collections.singletonMap("pageTableExtensionDisabled", disabled -> {
            Boolean pageTableDisabled = PageTableExtensionBackend.getTextBool(disabled, false);
            return pageTableDisabled ? HostSystemTags.PAGE_TABLE_EXTENSION_DISABLED_TOKEN : null;
        });
    }
}
