package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APIGetAccountGroupTreeReply

doc {
	title "GetAccountsInAccountGroup"

	category "iam1Accounts"

	desc """获取账户组下所有账户和组"""

	rest {
		request {
			url "GET /v1/account-groups/tree"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAccountGroupTreeMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn ""
					desc "账户组 UUID; null 表示查询所有最上层账户组"
					location "query"
					type "String"
					optional true
					since "4.10.0"
				}
				column {
					name "level"
					enclosedIn ""
					desc "向下查询子账户组的深度; 默认为 0 表示不查询子账户下的账户和组; 1 表示查询子账户下的账户和组"
					location "query"
					type "int"
					optional true
					since "4.10.0"
				}
				column {
					name "showGroup"
					enclosedIn ""
					desc "返回结果是否包含账户组信息, 默认为 true 表示返回结果包含账户组信息"
					location "query"
					type "boolean"
					optional true
					since "4.10.0"
				}
				column {
					name "showAccount"
					enclosedIn ""
					desc "返回结果是否包含账户信息, 默认为 true 表示返回结果包含账户信息"
					location "query"
					type "boolean"
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
			clz APIGetAccountGroupTreeReply.class
		}
	}
}