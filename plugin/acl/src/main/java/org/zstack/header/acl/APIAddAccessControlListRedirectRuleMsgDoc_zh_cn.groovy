package org.zstack.header.acl

import org.zstack.header.acl.APIAddAccessControlListEntryEvent

doc {
	title "AddAccessControlListRedirectRule"

	category "acl"

	desc """向访问控制策略组添加转发规则"""

	rest {
		request {
			url "POST /v1/access-control-lists/{aclUuid}/redirectRules"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddAccessControlListRedirectRuleMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.11.3"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.11.3"
				}
				column {
					name "domain"
					enclosedIn "params"
					desc "域名"
					location "body"
					type "String"
					optional true
					since "3.11.3"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "url"
					location "body"
					type "String"
					optional true
					since "3.11.3"
				}
				column {
					name "aclUuid"
					enclosedIn "params"
					desc "访问控制策略组的唯一标识"
					location "url"
					type "String"
					optional false
					since "3.11.3"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.11.3"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APIAddAccessControlListEntryEvent.class
		}
	}
}