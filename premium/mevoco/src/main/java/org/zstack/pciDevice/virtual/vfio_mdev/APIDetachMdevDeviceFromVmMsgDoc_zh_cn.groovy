package org.zstack.pciDevice.virtual.vfio_mdev

import org.zstack.pciDevice.virtual.vfio_mdev.APIDetachMdevDeviceFromVmEvent

doc {
	title "DetachMdevDeviceFromVm"

	category "pciDevice"

	desc """从云主机卸载PCI设备切分后产生的MDEV设备"""

	rest {
		request {
			url "DELETE /v1/mdev-devices/{mdevDeviceUuid}/vm-instances/{vmInstanceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachMdevDeviceFromVmMsg.class

			desc """从云主机卸载PCI设备切分后产生的MDEV设备"""

			params {

				column {
					name "mdevDeviceUuid"
					enclosedIn ""
					desc "MDEV设备UUID"
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
			clz APIDetachMdevDeviceFromVmEvent.class
		}
	}
}