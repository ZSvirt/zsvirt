package org.zstack.header.vm

import org.zstack.header.vm.APIAttachIsoToVmInstanceEvent

doc {
	title "加载ISO到云主机(AttachIsoToVmInstance)"

	category "vmInstance"

	desc """加载一个ISO镜像到Running或Stopped的云主机"""

	rest {
		request {
			url "POST /v1/vm-instances/{vmInstanceUuid}/iso/{isoUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachIsoToVmInstanceMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "isoUuid"
					enclosedIn ""
					desc "ISO UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIAttachIsoToVmInstanceEvent.class
		}
	}
}