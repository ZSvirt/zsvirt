package org.zstack.header.acl

import org.zstack.header.acl.APIAddAccessControlListEntryEvent

doc {
	title "AddAccessControlListEntry"

	category "acl"

	desc """向访问控制策略组添加IP组"""

	rest {
		request {
			url "POST /v1/access-control-lists/{aclUuid}/ipentries"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddAccessControlListEntryMsg.class

			desc """"""

			params {

				column {
					name "aclUuid"
					enclosedIn "params"
					desc "访问控制策略组的唯一标识"
					location "url"
					type "String"
					optional false
					since "3.9"
				}
				column {
					name "entries"
					enclosedIn "params"
					desc "IP组"
					location "body"
					type "String"
					optional false
					since "3.9"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.9"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.9"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.9"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.9"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.9"
				}
			}
		}

		response {
			clz APIAddAccessControlListEntryEvent.class
		}
	}
}