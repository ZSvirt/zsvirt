package org.zstack.pciDevice.specification.pci

import org.zstack.pciDevice.specification.pci.APIUpdatePciDeviceSpecEvent

doc {
	title "UpdatePciDeviceSpec"

	category "pciDevice"

	desc """更新PCI设备规格"""

	rest {
		request {
			url "PUT /v1/pci-device-specs/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdatePciDeviceSpecMsg.class

			desc """更新PCI设备规格"""

			params {

				column {
					name "uuid"
					enclosedIn "updatePciDeviceSpec"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.5.0"
				}
				column {
					name "name"
					enclosedIn "updatePciDeviceSpec"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.5.0"
				}
				column {
					name "description"
					enclosedIn "updatePciDeviceSpec"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.5.0"
				}
				column {
					name "romContent"
					enclosedIn "updatePciDeviceSpec"
					desc "BASE64编码后的固件内容"
					location "body"
					type "String"
					optional true
					since "3.5.0"
				}
				column {
					name "romVersion"
					enclosedIn "updatePciDeviceSpec"
					desc "固件版本"
					location "body"
					type "String"
					optional true
					since "3.5.0"
				}
				column {
					name "abandonSpecRom"
					enclosedIn "updatePciDeviceSpec"
					desc "删除已有固件"
					location "body"
					type "boolean"
					optional true
					since "3.5.0"
				}
				column {
					name "state"
					enclosedIn "updatePciDeviceSpec"
					desc "PCI设备规格启用状态"
					location "body"
					type "String"
					optional true
					since "3.5.0"
					values ("Enabled","Disabled")
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
			clz APIUpdatePciDeviceSpecEvent.class
		}
	}
}