package org.zstack.pciDevice.specification.pci

import org.zstack.pciDevice.specification.pci.APIGetPciDeviceSpecCandidatesReply

doc {
	title "GetPciDeviceSpecCandidates"

	category "pciDevice"

	desc """获取PCI设备规格候选列表"""

	rest {
		request {
			url "GET /v1/pci-device-specs/candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPciDeviceSpecCandidatesMsg.class

			desc """获取PCI设备规格候选列表"""

			params {

				column {
					name "clusterUuids"
					enclosedIn ""
					desc "集群UUID"
					location "query"
					type "List"
					optional true
					since "3.5.0"
				}
				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "query"
					type "String"
					optional true
					since "3.5.0"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "query"
					type "String"
					optional true
					since "3.5.0"
				}
				column {
					name "types"
					enclosedIn ""
					desc "设备类型"
					location "query"
					type "List"
					optional true
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
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.5.0"
				}
				column {
					name "vmInstanceUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetPciDeviceSpecCandidatesReply.class
		}
	}
}