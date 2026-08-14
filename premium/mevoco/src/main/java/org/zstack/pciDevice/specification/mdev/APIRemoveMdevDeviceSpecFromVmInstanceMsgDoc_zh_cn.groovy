package org.zstack.pciDevice.specification.mdev

import org.zstack.pciDevice.specification.mdev.APIRemoveMdevDeviceSpecFromVmInstanceEvent

doc {
	title "RemoveMdevDeviceSpecFromVmInstance"

	category "pciDevice"

	desc """为云主机删除MDEV设备规格"""

	rest {
		request {
			url "DELETE /v1/mdev-device-specs/{mdevSpecUuid}/vm-instances/{vmInstanceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveMdevDeviceSpecFromVmInstanceMsg.class

			desc """为云主机删除MDEV设备规格"""

			params {

				column {
					name "mdevSpecUuid"
					enclosedIn ""
					desc "MDEV设备规格UUID"
					location "url"
					type "String"
					optional false
					since "3.5.0"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.5.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.5.0"
				}
			}
		}

		response {
			clz APIRemoveMdevDeviceSpecFromVmInstanceEvent.class
		}
	}
}