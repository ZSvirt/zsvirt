package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APIUpdateAccountGroupEvent

doc {
	title "UpdateAccountGroup"

	category "iam1Accounts"

	desc """更新账户组"""

	rest {
		request {
			url "PUT /v1/account-groups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAccountGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateAccountGroup"
					desc "账户组 UUID"
					location "url"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "name"
					enclosedIn "updateAccountGroup"
					desc "账户组名称"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "description"
					enclosedIn "updateAccountGroup"
					desc "账户组的详细描述"
					location "body"
					type "String"
					optional true
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
			clz APIUpdateAccountGroupEvent.class
		}
	}
}