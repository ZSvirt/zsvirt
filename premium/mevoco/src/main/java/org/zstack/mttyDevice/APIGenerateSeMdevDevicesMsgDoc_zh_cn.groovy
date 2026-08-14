package org.zstack.mttyDevice

import org.zstack.mttyDevice.APIGenerateSeMdevDevicesEvent

doc {
	title "GenerateSeMdevDevices"

	category "mttyDevice"

	desc """虚拟化切分支持VFIO_MDEV的MTTY设备"""

	rest {
		request {
			url "PUT /v1/mtty-devices/{mttyDeviceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGenerateSeMdevDevicesMsg.class

			desc """虚拟化切分支持VFIO_MDEV的MTTY设备"""

			params {

				column {
					name "mttyDeviceUuid"
					enclosedIn "generateSeMdevDevices"
					desc "MTTY设备UUID"
					location "url"
					type "String"
					optional false
					since "3.15.11"
				}
				column {
					name "virtPartNum"
					enclosedIn "generateSeMdevDevices"
					desc "切分数量"
					location "body"
					type "Integer"
					optional false
					since "3.15.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.15.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.15.11"
				}
			}
		}

		response {
			clz APIGenerateSeMdevDevicesEvent.class
		}
	}
}