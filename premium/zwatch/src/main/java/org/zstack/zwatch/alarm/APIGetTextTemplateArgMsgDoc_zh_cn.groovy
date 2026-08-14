package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APIGetTextTemplateArgReply

doc {
	title "GetTextTemplateArg"

	category "zwatch"

	desc """查询文本模板支持的参数"""

	rest {
		request {
			url "GET /v1/zwatch/textTemplateArg"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetTextTemplateArgMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
			}
		}

		response {
			clz APIGetTextTemplateArgReply.class
		}
	}
}