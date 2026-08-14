package org.zstack.header.acl

import org.zstack.header.acl.APIChangeAccessControlListRedirectRuleEvent

doc {
	title "ChangeAccessControlListRedirectRule"

	category "acl"

	desc """修改控制策略组转发规则的名称"""

	rest {
		request {
			url "PUT /v1/access-control-lists/redirectRules/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIChangeAccessControlListRedirectRuleMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "changeAccessControlListRedirectRule"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.11.3"
				}
				column {
					name "name"
					enclosedIn "changeAccessControlListRedirectRule"
					desc "转发规则名称"
					location "body"
					type "String"
					optional true
					since "3.11.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.11.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.11.3"
				}
			}
		}

		response {
			clz APIChangeAccessControlListRedirectRuleEvent.class
		}
	}
}