package org.zstack.usbDevice

import org.zstack.usbDevice.APIDetachUsbDeviceFromVmEvent

doc {
	title "DetachUsbDeviceFromVm"

	category "usbDevice"

	desc """USB卸载"""

	rest {
		request {
			url "POST /v1/usb-device/usb-devices/{usbDeviceUuid}/detach"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachUsbDeviceFromVmMsg.class

			desc """USB卸载"""

			params {

				column {
					name "usbDeviceUuid"
					enclosedIn "params"
					desc "USB设备UUID"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.2"
				}
			}
		}

		response {
			clz APIDetachUsbDeviceFromVmEvent.class
		}
	}
}