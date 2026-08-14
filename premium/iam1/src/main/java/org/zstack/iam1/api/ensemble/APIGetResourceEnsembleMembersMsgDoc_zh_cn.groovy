package org.zstack.iam1.api.ensemble

import org.zstack.iam1.api.ensemble.APIGetResourceEnsembleMembersReply

doc {
	title "GetResourceEnsembleMembers"

	category "iam1Ensemble"

	desc """获取资源组中的所有成员"""

	rest {
		request {
			url "GET /v1/iam1/resource-ensemble"
			url "GET /v1/iam1/resource-ensemble/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetResourceEnsembleMembersMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，资源组中任意成员即可"
					location "query"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "4.10.0"
				}
			}
		}

		response {
			clz APIGetResourceEnsembleMembersReply.class
		}
	}
}