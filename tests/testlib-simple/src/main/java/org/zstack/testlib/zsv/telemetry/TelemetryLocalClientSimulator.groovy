package org.zstack.testlib.zsv.telemetry

import org.zstack.zsv.telemetry.client.TelemetryLocalClient

class TelemetryLocalClientSimulator extends TelemetryLocalClient {
    final TelemetryVirtualEndpointSpec parent

    TelemetryLocalClientSimulator(TelemetryVirtualEndpointSpec parent) {
        this.parent = parent
    }

    @Override
    String rootDir() {
        return parent.tempRootDir
    }
}
