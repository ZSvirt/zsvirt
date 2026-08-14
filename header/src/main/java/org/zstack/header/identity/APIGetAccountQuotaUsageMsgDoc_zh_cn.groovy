package org.zstack.header.identity

import org.zstack.header.identity.APIGetAccountQuotaUsageReply

doc {
	title "GetAccountQuotaUsage"

	category "identity"

	desc """获取账户配额使用情况"""

	rest {
		request {
			url "GET /v1/accounts/quota/{uuid}/usages"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAccountQuotaUsageMsg.class

			desc """获取账户配额使用情况"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetAccountQuotaUsageReply.class
		}
	}
}