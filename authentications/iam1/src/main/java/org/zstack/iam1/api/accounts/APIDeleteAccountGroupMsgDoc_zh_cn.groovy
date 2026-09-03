package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APIDeleteAccountGroupEvent

doc {
	title "DeleteAccountGroup"

	category "iam1Accounts"

	desc """删除账户组"""

	rest {
		request {
			url "DELETE /v1/account-groups/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAccountGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "账户组 UUID"
					location "url"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
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
			clz APIDeleteAccountGroupEvent.class
		}
	}
}