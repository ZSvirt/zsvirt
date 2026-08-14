package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIGetAllEventMetadataReply

doc {
	title "GetAllEventMetadata"

	category "zwatch"

	desc """获取所有事件元数据"""

	rest {
		request {
			url "GET /v1/zwatch/events/meta-data"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetAllEventMetadataMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn ""
					desc "事件名称"
					location "query"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "namespace"
					enclosedIn ""
					desc "事件命名空间"
					location "query"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetAllEventMetadataReply.class
		}
	}
}