package org.zstack.usbDevice.header;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;
import org.zstack.usbDevice.entity.UsbDeviceVO;

/**
 * @ Author : yh.w
 * @ Date   : Created in 16:31 2019/3/11
 */
@TagDefinition
public class UsbSystemTags {

    public static String USB_REDIRECT_PORT_TOKEN = "port";

    public static SystemTag USB_REDIRECT_PORT =
            new SystemTag(String.format("usbRedirectPort::{%s}", USB_REDIRECT_PORT_TOKEN), UsbDeviceVO.class);
}
