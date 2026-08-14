package org.zstack.storage.device.localRaid

import org.zstack.storage.device.localRaid.APIRefreshLocalRaidEvent

doc {
	title "RefreshLocalRaid"

	category "storageDevice"

	desc """刷新物理机Raid信息"""

	rest {
		request {
			url "PUT /v1/storage-devices/local-raid/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRefreshLocalRaidMsg.class

			desc """"""

			params {

				column {
					name "hostUuid"
					enclosedIn "refreshLocalRaid"
					desc "物理机UUID"
					location "body"
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
			clz APIRefreshLocalRaidEvent.class
		}
	}
}