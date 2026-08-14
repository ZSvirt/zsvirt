package org.zstack.header.storage.primary

import java.lang.Long

doc {

	title "存储预测容量报告"

	field {
		name "usedPhysicalCapacitiesForecast"
		desc ""
		type "List"
		since "zsv 3.17.21"
	}
	field {
		name "UsedPhysicalCapacitiesHistory"
		desc ""
		type "List"
		since "zsv 3.17.21"
	}
	field {
		name "TotalPhysicalCapacitiesHistory"
		desc ""
		type "List"
		since "zsv 3.17.21"
	}
	field {
		name "startTime"
		desc ""
		type "Long"
		since "zsv 3.17.21"
	}
	field {
		name "interval"
		desc ""
		type "Long"
		since "zsv 3.17.21"
	}
}
