package org.zstack.mttyDevice

import org.zstack.mttyDevice.APIUngenerateSeMdevDevicesEvent

doc {
	title "UngenerateSeMdevDevices"

	category "mttyDevice"

	desc """虚拟化还原支持VFIO_MDEV的MTTY设备"""

	rest {
		request {
			url "PUT /v1/mtty-devices/{mttyDeviceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUngenerateSeMdevDevicesMsg.class

			desc """虚拟化还原支持VFIO_MDEV的MTTY设备"""

			params {

				column {
					name "mttyDeviceUuid"
					enclosedIn "ungenerateSeMdevDevices"
					desc "被切分的MTTY设备UUID"
					location "url"
					type "String"
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
			clz APIUngenerateSeMdevDevicesEvent.class
		}
	}
}