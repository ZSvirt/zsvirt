package org.zstack.header.acl

import org.zstack.header.acl.APIDeleteAccessControlListEvent

doc {
	title "DeleteAccessControlList"

	category "acl"

	desc """删除访问控制策略组"""

	rest {
		request {
			url "DELETE /v1/access-control-lists/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAccessControlListMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.9"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.9"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.9"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.9"
				}
			}
		}

		response {
			clz APIDeleteAccessControlListEvent.class
		}
	}
}