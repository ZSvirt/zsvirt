package org.zstack.header.vm

import org.zstack.header.vm.APISetVmClockTrackEvent

doc {
	title "SetVmClockTrack"

	category "vmInstance"

	desc """设置云主机时钟同步"""

	rest {
		request {
			url "PUT /v1/vm-instances/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISetVmClockTrackMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "setVmClockTrack"
					desc "vmUuid"
					location "url"
					type "String"
					optional false
					since "zsv 3.11.0"
				}
				column {
					name "track"
					enclosedIn "setVmClockTrack"
					desc "BIOS时钟同步策略"
					location "body"
					type "String"
					optional false
					since "zsv 3.11.0"
					values ("guest","host")
				}
				column {
					name "syncAfterVMResume"
					enclosedIn "setVmClockTrack"
					desc "暂停到恢复是否同步"
					location "body"
					type "Boolean"
					optional true
					since "zsv 3.14.12"
				}
				column {
					name "intervalInSeconds"
					enclosedIn "setVmClockTrack"
					desc "定期同步时间间隔"
					location "body"
					type "Integer"
					optional true
					since "zsv 3.14.12"
					values ("0","60","600","1800","3600","7200","21600","43200","86400")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 3.11.0"
				}
			}
		}

		response {
			clz APISetVmClockTrackEvent.class
		}
	}
}