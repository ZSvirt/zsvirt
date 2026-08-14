package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APIMoveAccountGroupEvent

doc {
	title "MoveAccountGroup"

	category "iam1Accounts"

	desc """移动账户组"""

	rest {
		request {
			url "PUT /v1/account-groups/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIMoveAccountGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "moveAccountGroup"
					desc "账户组 UUID"
					location "url"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "parentUuid"
					enclosedIn "moveAccountGroup"
					desc "父账户组 UUID"
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
			clz APIMoveAccountGroupEvent.class
		}
	}
}