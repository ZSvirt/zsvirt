package org.zstack.pciDevice.virtual.sr_iov

import org.zstack.pciDevice.virtual.APIGenerateVirtualPciDevicesEvent

doc {
	title "GenerateSriovPciDevices"

	category "pciDevice"

	desc """虚拟化切分支持SR-IOV的PCI设备"""

	rest {
		request {
			url "PUT /v1/pci-devices/{pciDeviceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGenerateSriovPciDevicesMsg.class

			desc """虚拟化切分支持SR-IOV的PCI设备"""

			params {

				column {
					name "pciDeviceUuid"
					enclosedIn "generateSriovPciDevices"
					desc "PCI UUID"
					location "url"
					type "String"
					optional false
					since "3.5.0"
				}
				column {
					name "virtPartNum"
					enclosedIn "generateSriovPciDevices"
					desc "切分数量"
					location "body"
					type "Integer"
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