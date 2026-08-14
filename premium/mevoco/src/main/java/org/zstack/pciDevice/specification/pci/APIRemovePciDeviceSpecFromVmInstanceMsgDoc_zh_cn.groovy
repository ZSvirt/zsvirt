package org.zstack.pciDevice.specification.pci

import org.zstack.pciDevice.specification.pci.APIRemovePciDeviceSpecFromVmInstanceEvent

doc {
	title "RemovePciDeviceSpecFromVmInstance"

	category "pciDevice"

	desc """为云主机删除PCI设备规格"""

	rest {
		request {
			url "DELETE /v1/pci-device-specs/{pciSpecUuid}/vm-instances/{vmInstanceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemovePciDeviceSpecFromVmInstanceMsg.class

			desc """为云主机删除PCI设备规格"""

			params {

				column {
					name "pciSpecUuid"
					enclosedIn ""
					desc "PCI设备规格UUID"
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
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.5.0"
				}
			}
		}

		response {
			clz APIRemovePciDeviceSpecFromVmInstanceEvent.class
		}
	}
}