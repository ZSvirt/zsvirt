package org.zstack.pciDevice

import org.zstack.pciDevice.APIDeletePciDeviceEvent

doc {
	title "删除PCI设备"

	category "pciDevice"

	desc """删除失效的PCI设备，只允许删除Inactive状态的PCI设备"""

	rest {
		request {
			url "DELETE /v1/pci-device/pci-devices/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeletePciDeviceMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "2.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.1"
				}
			}
		}

		response {
			clz APIDeletePciDeviceEvent.class
		}
	}
}