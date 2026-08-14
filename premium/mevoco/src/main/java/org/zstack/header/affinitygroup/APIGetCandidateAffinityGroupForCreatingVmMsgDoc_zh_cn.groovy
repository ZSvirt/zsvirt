package org.zstack.header.affinitygroup

import org.zstack.header.affinitygroup.APIGetCandidateAffinityGroupForCreatingVmReply

doc {
	title "GetCandidateAffinityGroupForCreatingVm"

	category "affinityGroup"

	desc """创建VM获取可用非亲和组"""

	rest {
		request {
			url "GET /v1/vm-instances/candidate-affinityGroup"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateAffinityGroupForCreatingVmMsg.class

			desc """"""

			params {

				column {
					name "zoneUuid"
					enclosedIn ""
					desc "区域UUID"
					location "query"
					type "String"
					optional false
					since "3.11.0"
				}
				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "query"
					type "String"
					optional true
					since "3.11.0"
				}
				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "query"
					type "String"
					optional true
					since "3.11.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.11.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.11.0"
				}
			}
		}

		response {
			clz APIGetCandidateAffinityGroupForCreatingVmReply.class
		}
	}
}