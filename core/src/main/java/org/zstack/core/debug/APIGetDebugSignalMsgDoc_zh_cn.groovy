package org.zstack.core.debug

import org.zstack.core.debug.APIGetDebugSignalReply

doc {
	title "GetDebugSignal"

	category "debug"

	desc """Get available debug signals"""

	rest {
		request {
			url "GET /v1/debug"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetDebugSignalMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.6.0"
				}
			}
		}

		response {
			clz APIGetDebugSignalReply.class
		}
	}
}