package org.zstack.storage.device.fibreChannel

import org.zstack.storage.device.fibreChannel.APIRefreshFiberChannelStorageEvent

doc {
	title "RefreshFiberChannelStorage"

	category "storage.device"

	desc """刷新FC SAN信息"""

	rest {
		request {
			url "POST /v1/storage-devices/fiber-channel/controllers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRefreshFiberChannelStorageMsg.class

			desc """"""

			params {

				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "scsiLunUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIRefreshFiberChannelStorageEvent.class
		}
	}
}