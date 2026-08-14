package org.zstack.accessKey

import org.zstack.accessKey.APIDeleteAccessKeyEvent

doc {
	title "DeleteAccessKey"

	category "accessKey"

	desc """删除AccessKey"""

	rest {
		request {
			url "DELETE /v1/accesskeys/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAccessKeyMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "AccessKey UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "4.0.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "4.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "4.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "4.0.0"
				}
			}
		}

		response {
			clz APIDeleteAccessKeyEvent.class
		}
	}
}