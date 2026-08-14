package org.zstack.pciDevice

import org.zstack.pciDevice.APIGetHostIommuStateReply

doc {
	title "获取物理机IOMMU启用状态"

	category "pciDevice"

	desc """获取物理机IOMMU启用状态"""

	rest {
		request {
			url "GET /v1/pci-device/hosts/{uuid}/state"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetHostIommuStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "物理机的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
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
			clz APIGetHostIommuStateReply.class
		}
	}
}