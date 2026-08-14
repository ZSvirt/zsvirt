package org.zstack.pciDevice

import org.zstack.pciDevice.APIUpdatePciDeviceEvent

doc {
	title "UpdatePciDevice"

	category "pciDevice"

	desc """更新PCI设备"""

	rest {
		request {
			url "PUT /v1/pci-device/pci-devices/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdatePciDeviceMsg.class

			desc """更新PCI设备"""

			params {

				column {
					name "uuid"
					enclosedIn "updatePciDevice"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "state"
					enclosedIn "updatePciDevice"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.1"
					values ("Enabled","Disabled")
				}
				column {
					name "passThroughState"
					enclosedIn "updatePciDevice"
					desc ""
					location "body"
					type "String"
					optional true
					since "zsv 4.10.0"
					values ("Enabled","Available")
				}
				column {
					name "description"
					enclosedIn "updatePciDevice"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.1"
				}
				column {
					name "metaData"
					enclosedIn "updatePciDevice"
					desc ""
					location "body"
					type "String"
					optional true
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
				column {
					name "name"
					enclosedIn "updatePciDevice"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.5.0"
				}
			}
		}

		response {
			clz APIUpdatePciDeviceEvent.class
		}
	}
}