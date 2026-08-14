package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.APIGetCandidateAffinityGroupForAttachingVmReply

doc {
	title "GetCandidateAffinityGroupForAttachingVm"

	category "affinityGroup"

	desc """获取可绑定云主机的亲和组"""

	rest {
		request {
			url "GET /v1/affinityGroup/attachingVm"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateAffinityGroupForAttachingVmMsg.class

			desc """"""

			params {

				column {
					name "vmUuid"
					enclosedIn ""
					desc "虚拟机uuid"
					location "query"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APIGetCandidateAffinityGroupForAttachingVmReply.class
		}
	}
}