package org.zstack.zsv.telemetry;

import org.zstack.header.description.PackageDescription;
import org.zstack.zsv.telemetry.api.APICheckTelemetryUpdateMsg;
import org.zstack.zsv.telemetry.api.APIGetTelemetryConsentMsg;
import org.zstack.zsv.telemetry.api.APIGetTelemetrySettingMsg;
import org.zstack.zsv.telemetry.api.APIUpdateTelemetryConsentMsg;

public class RBACInfo implements PackageDescription {
    public static final String TELEMETRY_ROLE_UUID = "a4f8bfce38cb4fc2ac145ca0039cd19d";

    @Override
    public String permissionName() {
        return "telemetry";
    }

    {
        permissionBuilder()
                .normalAPIs(
                        APIGetTelemetryConsentMsg.class,
                        APIGetTelemetrySettingMsg.class,
                        APIUpdateTelemetryConsentMsg.class,
                        APICheckTelemetryUpdateMsg.class
                )
                .communityAvailable()
                .zsvBasicAvailable()
                .zsvProAvailable()
                .build();

        roleBuilder()
                .uuid(TELEMETRY_ROLE_UUID)
                .permissionBaseOnThis()
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actions(
                        APIGetTelemetryConsentMsg.class,
                        APIGetTelemetrySettingMsg.class,
                        APICheckTelemetryUpdateMsg.class
                )
                .build();

        apis()
                .inPackage("org.zstack.zsv.telemetry.api")
                .toService(TelemetryConstant.SERVICE_ID)
                .build();
    }
}
