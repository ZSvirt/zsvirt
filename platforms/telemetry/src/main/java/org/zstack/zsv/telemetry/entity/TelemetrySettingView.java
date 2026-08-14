package org.zstack.zsv.telemetry.entity;

import org.zstack.zsv.telemetry.TelemetryConstant;

public class TelemetrySettingView {
    private String descriptionKey;
    private String privacyPolicyUrl;

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public void setDescriptionKey(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public String getPrivacyPolicyUrl() {
        return privacyPolicyUrl;
    }

    public void setPrivacyPolicyUrl(String privacyPolicyUrl) {
        this.privacyPolicyUrl = privacyPolicyUrl;
    }

    public static TelemetrySettingView __example__() {
        TelemetrySettingView view = new TelemetrySettingView();
        view.setDescriptionKey(TelemetryConstant.SETTING_DESCRIPTION_I18N_KEY);
        view.setPrivacyPolicyUrl(TelemetryConstant.SETTING_PRIVACY_POLICY_URL);
        return view;
    }
}
