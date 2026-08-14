package org.zstack.ovf.api

import org.zstack.ovf.api.APIParseOvfReply

doc {
	title "ParseOvf"

	category "ovf"

	desc """解析OVF模板信息"""

	rest {
		request {
			url "POST /v1/ovf/parse"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIParseOvfMsg.class

			desc """"""

			params {

				column {
					name "xmlBase64"
					enclosedIn "params"
					desc "Base64编码的OVF文件内容"
					location "body"
					type "String"
					optional false
					since "3.14.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.14.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.14.6"
				}
			}
		}

		response {
			clz APIParseOvfReply.class
		}
	}
}