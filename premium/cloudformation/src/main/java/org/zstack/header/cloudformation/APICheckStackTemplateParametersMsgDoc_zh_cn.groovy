package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APICheckStackTemplateParametersReply

doc {
	title "CheckStackTemplateParameters"

	category "cloudformation"

	desc """查看模板的参数列表"""

	rest {
		request {
			url "POST /v1/cloudformation/stack/check"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICheckStackTemplateParametersMsg.class

			desc """"""

			params {

				column {
					name "type"
					enclosedIn "params"
					desc "模板类型，默认为zstack"
					location "body"
					type "String"
					optional true
					since "2.5.0"
					values ("zstack")
				}
				column {
					name "templateContent"
					enclosedIn "params"
					desc "模板内容，json字符串"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "uuid"
					enclosedIn "params"
					desc "模板的UUID"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.5.0"
				}
			}
		}

		response {
			clz APICheckStackTemplateParametersReply.class
		}
	}
}