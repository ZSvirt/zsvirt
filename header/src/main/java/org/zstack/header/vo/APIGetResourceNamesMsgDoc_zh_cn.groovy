package org.zstack.header.vo

import org.zstack.header.vo.APIGetResourceNamesReply

doc {
	title "GetResourceNames"

	category "identity"

	desc """输入资源的UUID可以获得该资源的名称，例如知道虚拟机的UUID，用于获得虚拟机名称。该API的特点在在于无需知道某个UUID具体代表什么资源，易于UI用作UUID到名称的翻译"""

	rest {
		request {
			url "GET /v1/resources/names"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetResourceNamesMsg.class

			desc """"""

			params {

				column {
					name "uuids"
					enclosedIn ""
					desc "资源的UUID列表"
					location "query"
					type "List"
					optional false
					since "zsv 0.6"
				}
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
			clz APIGetResourceNamesReply.class
		}
	}
}