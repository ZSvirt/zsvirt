package org.zstack.pciDevice.specification.pci

import org.zstack.pciDevice.specification.pci.APIAddPciDeviceSpecToVmInstanceEvent

doc {
	title "AddPciDeviceSpecToVmInstance"

	category "pciDevice"

	desc """为云主机添加PCI设备规格"""

	rest {
		request {
			url "POST /v1/pci-device-specs/{pciSpecUuid}/vm-instances/{vmInstanceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddPciDeviceSpecToVmInstanceMsg.class

			desc """为云主机添加PCI设备规格"""

			params {

				column {
					name "pciSpecUuid"
					enclosedIn "params"
					desc "PCI设备规格UUID"
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
					name "pciDeviceNumber"
					enclosedIn "params"
					desc ""
					location "body"
					type "Integer"
					optional true
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
			clz APIAddPciDeviceSpecToVmInstanceEvent.class
		}
	}
}