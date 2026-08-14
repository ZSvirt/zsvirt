package org.zstack.header.volume

import org.zstack.header.volume.APIValidateVolumeSnapshotChainEvent

doc {
	title "ValidateVolumeSnapshotChain"

	category "mevoco"

	desc """验证云盘当前快照链"""

	rest {
		request {
			url "PUT /v1/volumes/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIValidateVolumeSnapshotChainMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "validateVolumeSnapshotChain"
					desc "资源的UUID，唯一标示该资源，验证这个云盘当前在使用的快照链"
					location "url"
					type "String"
					optional false
					since "3.13.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.13.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.13.0"
				}
			}
		}

		response {
			clz APIValidateVolumeSnapshotChainEvent.class
		}
	}
}