package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.APIGetCandidateVMForAttachingAffinityGroupReply

doc {
	title "GetCandidateVMForAttachingAffinityGroup"

	category "affinityGroup"

	desc """获取可绑定亲和组的云主机"""

	rest {
		request {
			url "GET /v1/VM/attachingGroup"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateVMForAttachingAffinityGroupMsg.class

			desc """"""

			params {

				column {
					name "affinityGroupUuid"
					enclosedIn ""
					desc "亲和组uuid"
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
			clz APIGetCandidateVMForAttachingAffinityGroupReply.class
		}
	}
}