package org.zstack.storage.device.localRaid

import org.zstack.storage.device.localRaid.APISelfTestLocalRaidEvent

doc {
	title "SelfTestLocalRaid"

	category "storageDevice"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/storage-devices/local-raid/physical-drives/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISelfTestLocalRaidMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "selfTestLocalRaid"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.6"
				}
			}
		}

		response {
			clz APISelfTestLocalRaidEvent.class
		}
	}
}