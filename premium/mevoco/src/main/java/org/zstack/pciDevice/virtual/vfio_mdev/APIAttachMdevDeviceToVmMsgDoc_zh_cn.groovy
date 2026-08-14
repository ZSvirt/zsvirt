package org.zstack.pciDevice.virtual.vfio_mdev

import org.zstack.pciDevice.virtual.vfio_mdev.APIAttachMdevDeviceToVmEvent

doc {
	title "AttachMdevDeviceToVm"

	category "pciDevice"

	desc """绑定PCI设备切分出的MDEV设备到云主机"""

	rest {
		request {
			url "POST /v1/mdev-devices/{mdevDeviceUuid}/vm-instances/{vmInstanceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachMdevDeviceToVmMsg.class

			desc """绑定PCI设备切分出的MDEV设备到云主机"""

			params {

				column {
					name "mdevDeviceUuid"
					enclosedIn "params"
					desc "MDEV设备UUID"
					location "url"
					type "String"
					optional false
					since "3.5.0"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "params"
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
			clz APIAttachMdevDeviceToVmEvent.class
		}
	}
}