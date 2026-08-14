package org.zstack.header.storageDevice

import org.zstack.header.storageDevice.APIGetScsiLunCandidatesForAttachingVmReply

doc {
	title "GetScsiLunCandidatesForAttachingVm"

	category "storageDevice"

	desc """获取虚拟机可以加载的SCSI Lun"""

	rest {
		request {
			url "GET /v1/vm-instances/{vmInstanceUuid}/candidate-storage-devices"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetScsiLunCandidatesForAttachingVmMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.1.0"
				}
			}
		}

		response {
			clz APIGetScsiLunCandidatesForAttachingVmReply.class
		}
	}
}