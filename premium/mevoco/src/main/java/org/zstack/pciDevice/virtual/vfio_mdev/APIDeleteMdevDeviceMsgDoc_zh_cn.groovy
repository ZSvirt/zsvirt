package org.zstack.pciDevice.virtual.vfio_mdev

import org.zstack.pciDevice.virtual.vfio_mdev.APIDeleteMdevDeviceEvent

doc {
	title "删除MDEV设备"

	category "mdevDevice"

	desc """删除DEV设备，只允许删除Inactive状态的Mdev设备"""

	rest {
		request {
			url "DELETE /v1/mdev-devices/{mdevDeviceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteMdevDeviceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "MDEV设备UUID"
					location "url"
					type "String"
					optional false
					since "3.15.11"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.15.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.15.11"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.15.11"
				}
			}
		}

		response {
			clz APIDeleteMdevDeviceEvent.class
		}
	}
}