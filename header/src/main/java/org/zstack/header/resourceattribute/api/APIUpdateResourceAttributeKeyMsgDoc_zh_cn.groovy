package org.zstack.header.resourceattribute.api

import org.zstack.header.resourceattribute.api.APIUpdateResourceAttributeKeyEvent

doc {
	title "UpdateResourceAttributeKey"

	category "resourceAttribute"

	desc """更新自定义属性键"""

	rest {
		request {
			url "PUT /v1/resource-attributes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateResourceAttributeKeyMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateResourceAttributeKey"
					desc "自定义属性键 UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.10.16"
				}
				column {
					name "name"
					enclosedIn "updateResourceAttributeKey"
					desc "键名称"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "description"
					enclosedIn "updateResourceAttributeKey"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "resourceTypes"
					enclosedIn "updateResourceAttributeKey"
					desc "该属性支持的资源类型列表"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "createConstraints"
					enclosedIn "updateResourceAttributeKey"
					desc "新创建的限制"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "updateConstraints"
					enclosedIn "updateResourceAttributeKey"
					desc "更新的限制"
					location "body"
					type "List"
					optional true
					since "zsv 4.10.16"
				}
				column {
					name "deleteConstraintIds"
					enclosedIn "updateResourceAttributeKey"
					desc "删除的限制的 ID 列表"
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
			clz APIUpdateResourceAttributeKeyEvent.class
		}
	}
}