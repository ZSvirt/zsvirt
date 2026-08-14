package org.zstack.core.debug

import org.zstack.core.debug.APIDebugSignalEvent

doc {
	title "DebugSignal"

	category "debug"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/debug"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDebugSignalMsg.class

			desc """"""

			params {

				column {
					name "signals"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDebugSignalEvent.class
		}
	}
}