package org.zstack.resourceconfig

import org.zstack.resourceconfig.APIGetResourceConfigsReply

doc {
	title "GetResourceConfigs"

	category "resourceConfig"

	desc """查询多个资源级配置"""

	rest {
		request {
			url "GET /v1/resource-configurations/{resourceUuid}/{category}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetResourceConfigsMsg.class

			desc """"""

			params {

				column {
					name "category"
					enclosedIn ""
					desc "需要查询的资源级配置的category"
					location "url"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "names"
					enclosedIn ""
					desc "需要查询的资源级配置的名称"
					location "query"
					type "List"
					optional false
					since "3.17.0"
				}
				column {
					name "resourceUuid"
					enclosedIn ""
					desc "资源UUID"
					location "url"
					type "String"
					optional false
					since "3.17.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.17.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.17.0"
				}
			}
		}

		response {
			clz APIGetResourceConfigsReply.class
		}
	}
}