package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIDecodeStackTemplateReply

doc {
	title "DecodeStackTemplate"

	category "cloudformation"

	desc """从资源编排模板解析成资源关系图"""

	rest {
		request {
			url "POST /v1/cloudformation/stack/preview/resource"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDecodeStackTemplateMsg.class

			desc """"""

			params {

				column {
					name "type"
					enclosedIn "params"
					desc "类型"
					location "body"
					type "String"
					optional true
					since "3.0.0"
					values ("zstack")
				}
				column {
					name "templateContent"
					enclosedIn "params"
					desc "资源编排内容(Json)"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "uuid"
					enclosedIn "params"
					desc "资源编排模板的UUID，唯一标示该资源"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "parameters"
					enclosedIn "params"
					desc "参数列表(Json)"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "preparameters"
					enclosedIn "params"
					desc "预渲染参数列表(Json)"
					location "body"
					type "String"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIDecodeStackTemplateReply.class
		}
	}
}