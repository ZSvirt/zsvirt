package org.zstack.pciDevice.virtual.vfio_mdev

import org.zstack.pciDevice.virtual.APIUngenerateVirtualPciDevicesEvent

doc {
	title "UngenerateMdevDevices"

	category "pciDevice"

	desc """虚拟化还原支持VFIO_MDEV的PCI设备"""

	rest {
		request {
			url "PUT /v1/pci-devices/{pciDeviceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUngenerateMdevDevicesMsg.class

			desc """虚拟化还原支持VFIO_MDEV的PCI设备"""

			params {

				column {
					name "pciDeviceUuid"
					enclosedIn "ungenerateMdevDevices"
					desc "被切分的PCI设备UUID"
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
			clz APIUngenerateVirtualPciDevicesEvent.class
		}
	}
}