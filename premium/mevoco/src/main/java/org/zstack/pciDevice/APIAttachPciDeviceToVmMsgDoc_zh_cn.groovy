package org.zstack.pciDevice

import org.zstack.pciDevice.APIAttachPciDeviceToVmEvent

doc {
	title "绑定PCI设备到云主机"

	category "pciDevice"

	desc """绑定PCI设备到云主机，管理员可以直接将状态为System、Active的PCI设备绑定到状态为Active、Stop的云主机"""

	rest {
		request {
			url "POST /v1/pci-device/pci-devices/{pciDeviceUuid}/attach"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachPciDeviceToVmMsg.class

			desc """"""

			params {

				column {
					name "pciDeviceUuid"
					enclosedIn "params"
					desc "PCI设备UUID"
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "云主机UUID"
					location "body"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.1"
				}
			}
		}

		response {
			clz APIAttachPciDeviceToVmEvent.class
		}
	}
}