package org.zstack.resourceconfig

import org.zstack.resourceconfig.APIUpdateResourceConfigEvent

doc {
	title "UpdateResourceConfig"

	category "resourceConfig"

	desc """更新资源高级设置"""

	rest {
		request {
			url "PUT /v1/resource-configurations/{category}/{name}/{resourceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateResourceConfigMsg.class

			desc """"""

			params {

				column {
					name "category"
					enclosedIn "updateResourceConfig"
					desc "设置类型"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "name"
					enclosedIn "updateResourceConfig"
					desc "设置名称"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "updateResourceConfig"
					desc "设置的资源UUID"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "value"
					enclosedIn "updateResourceConfig"
					desc "设置的值"
					location "body"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIUpdateResourceConfigEvent.class
		}
	}
}