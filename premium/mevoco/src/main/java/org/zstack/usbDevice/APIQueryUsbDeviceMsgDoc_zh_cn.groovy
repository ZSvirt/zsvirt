package org.zstack.usbDevice

import org.zstack.usbDevice.APIQueryUsbDeviceReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryUsbDevice"

	category "usbDevice"

	desc """查询USB设备"""

	rest {
		request {
			url "GET /v1/usb-device/usb-devices"
			url "GET /v1/usb-device/usb-devices/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryUsbDeviceMsg.class

			desc """查询USB设备"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryUsbDeviceReply.class
		}
	}
}