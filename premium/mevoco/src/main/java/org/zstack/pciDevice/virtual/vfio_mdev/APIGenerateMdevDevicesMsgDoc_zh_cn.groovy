package org.zstack.pciDevice.virtual.vfio_mdev

import org.zstack.pciDevice.virtual.APIGenerateVirtualPciDevicesEvent

doc {
	title "GenerateMdevDevices"

	category "pciDevice"

	desc """虚拟化切分支持VFIO_MDEV的PCI设备"""

	rest {
		request {
			url "PUT /v1/pci-devices/{pciDeviceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGenerateMdevDevicesMsg.class

			desc """虚拟化切分支持VFIO_MDEV的PCI设备"""

			params {

				column {
					name "pciDeviceUuid"
					enclosedIn "generateMdevDevices"
					desc "PCI设备UUID"
					location "url"
					type "String"
					optional false
					since "3.5.0"
				}
				column {
					name "mdevSpecUuid"
					enclosedIn "generateMdevDevices"
					desc "MDEV设备规格UUID"
					location "body"
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
			clz APIGenerateVirtualPciDevicesEvent.class
		}
	}
}