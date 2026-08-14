package org.zstack.header.vm

import org.zstack.header.vm.APIGetCandidateVmForAttachingIsoReply

doc {
	title "获取ISO可加载云主机列表(GetCandidateVmForAttachingIso)"

	category "vmInstance"

	desc """获取一个ISO可以加载到的云主机列表"""

	rest {
		request {
			url "GET /v1/images/iso/{isoUuid}/vm-candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetCandidateVmForAttachingIsoMsg.class

			desc """"""

			params {

				column {
					name "isoUuid"
					enclosedIn ""
					desc "ISO UUID"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetCandidateVmForAttachingIsoReply.class
		}
	}
}