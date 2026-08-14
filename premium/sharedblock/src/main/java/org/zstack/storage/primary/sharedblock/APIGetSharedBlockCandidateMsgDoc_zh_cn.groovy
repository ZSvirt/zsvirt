package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.APIGetSharedBlockCandidateReply

doc {
	title "GetSharedBlockCandidate"

	category "sharedblock"

	desc """获取共享块设备候选清单"""

	rest {
		request {
			url "GET /v1/primary-storage/sharedblockgroup/sharedblock-candidates"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetSharedBlockCandidateMsg.class

			desc """"""

			params {

				column {
					name "clusterUuid"
					enclosedIn ""
					desc "集群UUID"
					location "query"
					type "String"
					optional false
					since "2.5.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "2.5.0"
				}
			}
		}

		response {
			clz APIGetSharedBlockCandidateReply.class
		}
	}
}