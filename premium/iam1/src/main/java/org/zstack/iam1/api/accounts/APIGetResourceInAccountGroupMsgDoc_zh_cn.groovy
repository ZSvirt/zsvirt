package org.zstack.iam1.api.accounts

import org.zstack.iam1.api.accounts.APIGetResourceInAccountGroupReply

doc {
	title "GetResourceInAccountGroup"

	category "iam1Accounts"

	desc """获取账户组下所有分享的资源"""

	rest {
		request {
			url "GET /v1/account-groups/{groupUuid}/resources"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetResourceInAccountGroupMsg.class

			desc """"""

			params {

				column {
					name "groupUuid"
					enclosedIn ""
					desc "账户组 UUID"
					location "url"
					type "String"
					optional false
					since "4.10.0"
				}
				column {
					name "includeInheritedResources"
					enclosedIn ""
					desc "是否包含父账户组的继承的资源; 如果为 true, 则结果将包含父账户组分享的资源"
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
			clz APIGetResourceInAccountGroupReply.class
		}
	}
}