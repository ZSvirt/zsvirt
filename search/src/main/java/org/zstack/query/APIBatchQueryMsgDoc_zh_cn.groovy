package org.zstack.query

import org.zstack.query.APIBatchQueryReply

doc {
	title "BatchQuery"

	category "query"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/batch-queries"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIBatchQueryMsg.class

			desc """"""

			params {

				column {
					name "script"
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
			clz APIBatchQueryReply.class
		}
	}
}