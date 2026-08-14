package org.zstack.header.acl

import org.zstack.header.acl.APICreateAccessControlListEvent

doc {
	title "CreateAccessControlList"

	category "acl"

	desc """创建访问控制策略组"""

	rest {
		request {
			url "POST /v1/access-control-lists"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAccessControlListMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
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
					name "ipVersion"
					enclosedIn "params"
					desc "IP协议版本"
					location "body"
					type "Integer"
					optional true
					since "3.9"
					values ("4","6")
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
			clz APICreateAccessControlListEvent.class
		}
	}
}