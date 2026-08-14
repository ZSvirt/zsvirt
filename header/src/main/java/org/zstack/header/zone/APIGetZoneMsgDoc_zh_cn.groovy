package org.zstack.header.zone

import org.zstack.header.zone.APIGetZoneReply

doc {
	title "GetZone"

	category "zone"

	desc """在这里填写API描述"""

	rest {
		request {
			url "GET /v1/zones/{uuid}/info"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetZoneMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional true
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
			clz APIGetZoneReply.class
		}
	}
}