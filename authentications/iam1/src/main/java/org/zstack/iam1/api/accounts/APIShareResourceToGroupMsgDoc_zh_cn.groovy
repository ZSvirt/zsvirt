package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APIShareResourceToGroupEvent

doc {
	title "ShareResourceToGroup"

	category "iam1Accounts"

	desc """将资源分享给账户组"""

	rest {
		request {
			url "PUT /v1/account-groups/resources/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIShareResourceToGroupMsg.class

			desc """"""

			params {

				column {
					name "resourceUuids"
					enclosedIn "shareResourceToGroup"
					desc "待分享的资源 UUID 列表, 必须是资源组中的资源"
					location "body"
					type "List"
					optional false
					since "4.10.0"
				}
				column {
					name "groupUuid"
					enclosedIn "shareResourceToGroup"
					desc "账户组 UUID"
					location "body"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.0"
				}
			}
		}

		response {
			clz APIShareResourceToGroupEvent.class
		}
	}
}