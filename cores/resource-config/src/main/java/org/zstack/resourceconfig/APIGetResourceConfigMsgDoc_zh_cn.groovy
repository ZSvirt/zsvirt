package org.zstack.resourceconfig

import org.zstack.resourceconfig.APIGetResourceConfigReply

doc {
	title "GetResourceConfig"

	category "resourceConfig"

	desc """获取资源的高级设置"""

	rest {
		request {
			url "GET /v1/resource-configurations/{resourceUuid}/{category}/{name}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetResourceConfigMsg.class

			desc """"""

			params {

				column {
					name "category"
					enclosedIn ""
					desc "设置类型"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "name"
					enclosedIn ""
					desc "设置名称"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "resourceUuid"
					enclosedIn ""
					desc "资源UUID"
					location "url"
					type "String"
					optional false
					since "3.4.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.4.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIGetResourceConfigReply.class
		}
	}
}