package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIGetSupportedCloudFormationResourcesReply

doc {
	title "GetSupportedCloudFormationResources"

	category "cloudformation"

	desc """获取zstack资源编排模板支持的资源清单列表"""

	rest {
		request {
			url "GET /v1/cloudformation/resources"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetSupportedCloudFormationResourcesMsg.class

			desc """"""

			params {

				column {
					name "version"
					enclosedIn ""
					desc "版本号，如v1"
					location "query"
					type "String"
					optional true
					since "2.6.0"
					values ("v1")
				}
				column {
					name "type"
					enclosedIn ""
					desc "类型，默认为zstack"
					location "query"
					type "String"
					optional true
					since "2.6.0"
					values ("zstack")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.6.0"
				}
			}
		}

		response {
			clz APIGetSupportedCloudFormationResourcesReply.class
		}
	}
}