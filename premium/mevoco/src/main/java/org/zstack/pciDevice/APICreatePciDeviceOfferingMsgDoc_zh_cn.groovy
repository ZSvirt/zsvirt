package org.zstack.pciDevice

import org.zstack.pciDevice.APICreatePciDeviceOfferingEvent

doc {
	title "创建PCI设备规格"

	category "pciDevice"

	desc """创建PCI设备规格"""

	rest {
		request {
			url "POST /v1/pci-device/pci-device-offerings"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreatePciDeviceOfferingMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.1"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.1"
				}
				column {
					name "vendorId"
					enclosedIn "params"
					desc "供应商ID"
					location "body"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "deviceId"
					enclosedIn "params"
					desc "产品ID"
					location "body"
					type "String"
					optional false
					since "2.1"
				}
				column {
					name "subvendorId"
					enclosedIn "params"
					desc "子供应商ID"
					location "body"
					type "String"
					optional true
					since "2.1"
				}
				column {
					name "subdeviceId"
					enclosedIn "params"
					desc "子设备ID"
					location "body"
					type "String"
					optional true
					since "2.1"
				}
				column {
					name "ramSize"
					enclosedIn "params"
					desc "RAM容量"
					location "body"
					type "String"
					optional true
					since "3.5.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
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
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APICreatePciDeviceOfferingEvent.class
		}
	}
}