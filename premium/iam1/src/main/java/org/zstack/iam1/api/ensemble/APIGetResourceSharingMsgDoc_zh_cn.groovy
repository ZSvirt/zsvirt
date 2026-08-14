package org.zstack.iam1.api.ensemble

import org.zstack.iam1.api.ensemble.APIGetResourceSharingReply

doc {
	title "GetResourceSharing"

	category "iam1Ensemble"

	desc """获取资源被分享对象"""

	rest {
		request {
			url "GET /v1/iam1/resource-ensemble/view-sharing"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetResourceSharingMsg.class

			desc """获取资源被分享给哪些账户和账户组"""

			params {

				column {
					name "resourceUuid"
					enclosedIn ""
					desc "资源UUID"
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
			clz APIGetResourceSharingReply.class
		}
	}
}