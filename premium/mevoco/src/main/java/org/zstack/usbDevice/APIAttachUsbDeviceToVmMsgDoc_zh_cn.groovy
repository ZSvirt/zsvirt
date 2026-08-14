package org.zstack.usbDevice

import org.zstack.usbDevice.APIAttachUsbDeviceToVmEvent

doc {
	title "AttachUsbDeviceToVm"

	category "usbDevice"

	desc """USB透传"""

	rest {
		request {
			url "POST /v1/usb-device/usb-devices/{usbDeviceUuid}/attach"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachUsbDeviceToVmMsg.class

			desc """USB透传"""

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
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "body"
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
				column {
					name "attachType"
					enclosedIn "params"
					desc "加载方式"
					location "body"
					type "String"
					optional true
					since "3.5"
					values ("PassThrough","Redirect")
				}
			}
		}

		response {
			clz APIAttachUsbDeviceToVmEvent.class
		}
	}
}