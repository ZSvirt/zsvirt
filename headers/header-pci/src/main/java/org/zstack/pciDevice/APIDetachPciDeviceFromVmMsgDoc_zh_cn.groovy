package org.zstack.pciDevice

import org.zstack.pciDevice.APIDetachPciDeviceFromVmEvent

doc {
	title "卸载PCI设备"

	category "pciDevice"

	desc """从虚拟机上上卸载PCI设备"""

	rest {
		request {
			url "POST /v1/pci-device/pci-devices/{pciDeviceUuid}/detach"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachPciDeviceFromVmMsg.class

			desc """"""

			params {

				column {
					name "pciDeviceUuid"
					enclosedIn "params"
					desc ""
					location "url"
					type "String"
					optional false
					since "zsv_2.1"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "虚拟机UUID"
					location "body"
					type "String"
					optional false
					since "zsv_2.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv_2.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv_2.1"
				}
			}
		}

		response {
			clz APIDetachPciDeviceFromVmEvent.class
		}
	}
}