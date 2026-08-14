package org.zstack.header.acl

import org.zstack.header.acl.APIRemoveAccessControlListEntryEvent

doc {
	title "RemoveAccessControlListEntry"

	category "acl"

	desc """删除访问控制策略的IP组"""

	rest {
		request {
			url "DELETE /v1/access-control-lists/{aclUuid}/ipentries/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveAccessControlListEntryMsg.class

			desc """"""

			params {

				column {
					name "aclUuid"
					enclosedIn ""
					desc "访问控制策略组的唯一标识"
					location "url"
					type "String"
					optional false
					since "3.9"
				}
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
			clz APIRemoveAccessControlListEntryEvent.class
		}
	}
}