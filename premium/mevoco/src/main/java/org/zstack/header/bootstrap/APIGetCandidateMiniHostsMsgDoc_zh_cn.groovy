package org.zstack.header.bootstrap

import org.zstack.header.bootstrap.APIGetCandidateMiniHostsReply

doc {
	title "GetCandidateMiniHosts"

	category "mevoco"

	desc """获取未添加的Mini物理机"""

	rest {
		request {
			url "GET /v1/mini-clusters/candidate-hosts"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateMiniHostsMsg.class

			desc """"""

			params {

				column {
					name "local"
					enclosedIn ""
					desc ""
					location "query"
					type "boolean"
					optional true
					since "3.6.0"
				}
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
				column {
					name "configure"
					enclosedIn ""
					desc ""
					location "query"
					type "boolean"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIGetCandidateMiniHostsReply.class
		}
	}
}