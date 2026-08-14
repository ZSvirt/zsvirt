package org.zstack.pciDevice

import org.zstack.pciDevice.APIGetPciDeviceCandidatesForAttachingVmReply

doc {
	title "GetPciDeviceCandidatesForAttachingVm"

	category "pciDevice"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/candidate-pci-devices"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetPciDeviceCandidatesForAttachingVmMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "types"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "pciSpecUuids"
					enclosedIn ""
					desc "pci规格Uuid"
					location "query"
					type "List"
					optional true
					since "4.10.6"
				}
			}
		}

		response {
			clz APIGetPciDeviceCandidatesForAttachingVmReply.class
		}
	}
}