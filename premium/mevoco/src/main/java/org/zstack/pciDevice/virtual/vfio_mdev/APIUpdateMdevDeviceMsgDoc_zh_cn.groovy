package org.zstack.pciDevice.virtual.vfio_mdev

import org.zstack.pciDevice.virtual.vfio_mdev.APIUpdateMdevDeviceEvent

doc {
	title "UpdateMdevDevice"

	category "pciDevice"

	desc """更新PCI设备切分出的MDEV设备"""

	rest {
		request {
			url "PUT /v1/mdev-devices/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateMdevDeviceMsg.class

			desc """更新PCI设备切分出的MDEV设备"""

			params {

				column {
					name "uuid"
					enclosedIn "updateMdevDevice"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.5.0"
				}
				column {
					name "name"
					enclosedIn "updateMdevDevice"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.5.0"
				}
				column {
					name "description"
					enclosedIn "updateMdevDevice"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.5.0"
				}
				column {
					name "state"
					enclosedIn "updateMdevDevice"
					desc "设备状态"
					location "body"
					type "String"
					optional true
					since "3.5.0"
					values ("Enabled","Disabled")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.5.0"
				}
			}
		}

		response {
			clz APIUpdateMdevDeviceEvent.class
		}
	}
}