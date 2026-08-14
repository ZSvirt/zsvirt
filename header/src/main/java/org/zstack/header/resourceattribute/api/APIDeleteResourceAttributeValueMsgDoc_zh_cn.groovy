package org.zstack.header.resourceattribute.api

import org.zstack.header.resourceattribute.api.APIDeleteResourceAttributeValueEvent

doc {
	title "DeleteResourceAttributeValue"

	category "resourceAttribute"

	desc """删除自定义属性值"""

	rest {
		request {
			url "DELETE /v1/resource-attributes/{keyUuid}/resources"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteResourceAttributeValueMsg.class

			desc """"""

			params {

				column {
					name "keyUuid"
					enclosedIn ""
					desc "自定义属性键 UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "resourceUuids"
					enclosedIn ""
					desc "资源 UUID"
					location "query"
					type "List"
					optional false
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
			clz APIDeleteResourceAttributeValueEvent.class
		}
	}
}