package org.zstack.usbDevice;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * @ Author : yh.w
 * @ Date   : Created in 16:31 2019/3/11
 */
@TagDefinition
public class UsbSystemTags {

    public static String USB_REDIRECT_PORT_TOKEN = "port";

    public static PatternedSystemTag USB_REDIRECT_PORT =
            new PatternedSystemTag(String.format("usbRedirectPort::{%s}", USB_REDIRECT_PORT_TOKEN), UsbDeviceVO.class);
}
