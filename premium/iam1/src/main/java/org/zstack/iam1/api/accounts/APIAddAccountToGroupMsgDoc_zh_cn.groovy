package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APIAddAccountToGroupEvent

doc {
	title "AddAccountToGroup"

	category "iam1Accounts"

	desc """账户加入账户组"""

	rest {
		request {
			url "POST /v1/account-groups/{groupUuid}/accounts"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddAccountToGroupMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn "params"
					desc "账户组 UUID"
					location "url"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "accountUuids"
					enclosedIn "params"
					desc "账户 UUID 列表"
					location "body"
					type "List"
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
			clz APIAddAccountToGroupEvent.class
		}
	}
}