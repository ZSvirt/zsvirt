package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APICreateAccountGroupEvent

doc {
	title "CreateAccountGroup"

	category "iam1Accounts"

	desc """创建账户组"""

	rest {
		request {
			url "POST /v1/account-groups"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAccountGroupMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "账户组名称"
					location "body"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "账户组的详细描述"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "parentUuid"
					enclosedIn "params"
					desc "上层账户组 UUID"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "预定义的账户组 UUID"
					location "body"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APICreateAccountGroupEvent.class
		}
	}
}