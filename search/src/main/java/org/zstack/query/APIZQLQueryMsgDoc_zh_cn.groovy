package org.zstack.query

import org.zstack.query.APIZQLQueryReply

doc {
	title "ZQLQuery"

	category "query"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/zql"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIZQLQueryMsg.class

			desc """"""

			params {

				column {
					name "zql"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIZQLQueryReply.class
		}
	}
}