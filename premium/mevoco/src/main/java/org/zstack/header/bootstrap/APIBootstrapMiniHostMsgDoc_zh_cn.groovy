package org.zstack.header.bootstrap

import org.zstack.header.bootstrap.APIBootstrapMiniHostEvent

doc {
	title "BootstrapMiniHost"

	category "mevoco"

	desc """初始化Mini一体机"""

	rest {
		request {
			url "POST /v1/mini-clusters/hosts"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIBootstrapMiniHostMsg.class

			desc """"""

			params {

				column {
					name "local"
					enclosedIn "params"
					desc ""
					location "body"
					type "MiniHostInfo"
					optional false
					since "3.6.0"
				}
				column {
					name "peer"
					enclosedIn "params"
					desc ""
					location "body"
					type "MiniHostInfo"
					optional false
					since "3.6.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.6.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.6.0"
				}
			}
		}

		response {
			clz APIBootstrapMiniHostEvent.class
		}
	}
}