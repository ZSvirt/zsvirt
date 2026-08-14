package org.zstack.header

import org.zstack.header.APIIsOpensourceVersionReply

doc {
	title "IsOpensourceVersion"

	category "identity"

	desc """判断ZStack是否为开源版本。开源版本不带企业版插件"""

	rest {
		request {
			url "GET /v1/meta-data/opensource"



			clz APIIsOpensourceVersionMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIIsOpensourceVersionReply.class
		}
	}
}