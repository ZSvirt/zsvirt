package org.zstack.pciDevice

import org.zstack.pciDevice.APIGetPciDeviceCandidatesForNewCreateVmReply

doc {
	title "GetPciDeviceCandidatesForNewCreateVm"

	category "pciDevice"

	desc """为新建虚拟机获取可加载PCI设备"""

	rest {
		request {
			url "GET /v1/pci-device/candidate-pci-devices-for-new-create-vm"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPciDeviceCandidatesForNewCreateVmMsg.class

			desc """"""

			params {

				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "query"
					type "String"
					optional true
					since "2.4"
				}
				column {
					name "clusterUuids"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.4"
				}
				column {
					name "types"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.4"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.4"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.4"
				}
			}
		}

		response {
			clz APIGetPciDeviceCandidatesForNewCreateVmReply.class
		}
	}
}