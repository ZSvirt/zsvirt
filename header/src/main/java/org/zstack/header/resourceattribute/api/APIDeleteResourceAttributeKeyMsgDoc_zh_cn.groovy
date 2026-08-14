package org.zstack.header.resourceattribute.api

import org.zstack.header.resourceattribute.api.APIDeleteResourceAttributeKeyEvent

doc {
	title "DeleteResourceAttributeKey"

	category "resourceAttribute"

	desc """删除自定义属性键"""

	rest {
		request {
			url "DELETE /v1/resource-attributes/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteResourceAttributeKeyMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "自定义属性键 UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
			}
		}

		response {
			clz APIDeleteResourceAttributeKeyEvent.class
		}
	}
}