package org.zstack.header.resourceattribute.api

import org.zstack.header.resourceattribute.api.APICreateResourceAttributeKeyEvent

doc {
	title "CreateResourceAttributeKey"

	category "resourceAttribute"

	desc """创建自定义属性键"""

	rest {
		request {
			url "POST /v1/resource-attributes"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateResourceAttributeKeyMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "自定义属性键名称"
					location "body"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "resourceTypes"
					enclosedIn "params"
					desc "该属性支持的资源类型列表, 默认支持所有可支持的属性"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "constraints"
					enclosedIn "params"
					desc "属性限制列表"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源 UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签 UUID 列表"
					location "body"
					type "List"
					optional true
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
			clz APICreateResourceAttributeKeyEvent.class
		}
	}
}