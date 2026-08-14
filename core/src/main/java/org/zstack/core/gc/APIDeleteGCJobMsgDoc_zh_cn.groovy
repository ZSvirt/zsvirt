package org.zstack.core.gc

import org.zstack.core.gc.APIDeleteGCJobEvent

doc {
	title "DeleteGCJob"

	category "gc"

	desc """在这里填写API描述"""

	rest {
		request {
			url "DELETE /v1/gc-jobs/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteGCJobMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
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
			clz APIDeleteGCJobEvent.class
		}
	}
}