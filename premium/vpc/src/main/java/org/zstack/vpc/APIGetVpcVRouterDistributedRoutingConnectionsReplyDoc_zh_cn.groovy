package org.zstack.vpc

import org.zstack.header.errorcode.ErrorCode

doc {

	title "VPC云路由实时流量状态"

	ref {
		name "error"
		path "org.zstack.vpc.APIGetVpcVRouterDistributedRoutingConnectionsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "2.3"
		clz ErrorCode.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	field {
		name "inventories"
		desc "实时流量状态集合，结构为Map，Key是通信两端的地址，内容为一个Map，包含源网络和目的网络的类型、Vni，通信两端的Mac，上一次操作的时间以及目前的优化状态，其中ZSNP_DST_SUCC表示优化成功"
		type "Map"
		since "2.3"
	}
}
