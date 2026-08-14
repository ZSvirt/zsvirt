package org.zstack.header.resourceattribute.api

import org.zstack.header.resourceattribute.api.APICreateResourceAttributeValueEvent

doc {
	title "CreateResourceAttributeValue"

	category "resourceAttribute"

	desc """创建自定义属性值"""

	rest {
		request {
			url "POST /v1/resource-attributes/{keyUuid}/resources"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateResourceAttributeValueMsg.class

			desc """"""

			params {

				column {
					name "keyUuid"
					enclosedIn "params"
					desc "自定义属性键 UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "value"
					enclosedIn "params"
					desc "值"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "resourceUuids"
					enclosedIn "params"
					desc "资源 UUID"
					location "body"
					type "List"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
			}
		}

		response {
			clz APICreateResourceAttributeValueEvent.class
		}
	}
}