package org.zstack.zsv.telemetry.entity;

public class TelemetryUpdateInfoView {
    private String version;
    private String releaseNotesZh;
    private String releaseNotesEn;
    private String currentVersion;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getReleaseNotesZh() {
        return releaseNotesZh;
    }

    public void setReleaseNotesZh(String releaseNotesZh) {
        this.releaseNotesZh = releaseNotesZh;
    }

    public String getReleaseNotesEn() {
        return releaseNotesEn;
    }

    public void setReleaseNotesEn(String releaseNotesEn) {
        this.releaseNotesEn = releaseNotesEn;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public static TelemetryUpdateInfoView __example__() {
        TelemetryUpdateInfoView view = new TelemetryUpdateInfoView();
        view.setVersion("5.1.0");
        view.setReleaseNotesZh("1. 修复若干问题");
        view.setReleaseNotesEn("1. Bug fixes");
        view.setCurrentVersion("5.0.0");
        return view;
    }
}
