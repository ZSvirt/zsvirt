package org.zstack.usbDevice

import org.zstack.usbDevice.APIUpdateUsbDeviceEvent

doc {
	title "UpdateUsbDevice"

	category "usbDevice"

	desc """更新USB设备"""

	rest {
		request {
			url "PUT /v1/usb-device/usb-devices/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateUsbDeviceMsg.class

			desc """更新USB设备"""

			params {

				column {
					name "uuid"
					enclosedIn "updateUsbDevice"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.2"
				}
				column {
					name "name"
					enclosedIn "updateUsbDevice"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.2"
				}
				column {
					name "description"
					enclosedIn "updateUsbDevice"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.2"
				}
				column {
					name "state"
					enclosedIn "updateUsbDevice"
					desc "USB设备状态"
					location "body"
					type "String"
					optional true
					since "2.2"
					values ("Enabled","Disabled")
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
			clz APIUpdateUsbDeviceEvent.class
		}
	}
}