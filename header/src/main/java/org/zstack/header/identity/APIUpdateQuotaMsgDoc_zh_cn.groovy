package org.zstack.header.identity

import org.zstack.header.identity.APIUpdateQuotaEvent

doc {
	title "UpdateQuota"

	category "identity"

	desc """更新配额"""

	rest {
		request {
			url "PUT /v1/accounts/quotas/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateQuotaMsg.class

			desc """更新配额"""

			params {

				column {
					name "identityUuid"
					enclosedIn "updateQuota"
					desc "身份实体的UUID（账户的，用户的）"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updateQuota"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "value"
					enclosedIn "updateQuota"
					desc "配额值"
					location "body"
					type "long"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
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
			clz APIUpdateQuotaEvent.class
		}
	}
}