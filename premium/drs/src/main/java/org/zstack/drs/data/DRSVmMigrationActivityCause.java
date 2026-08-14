package org.zstack.drs.data;

/**
 * Created by lining on 2019/12/26.
 */
public enum DRSVmMigrationActivityCause {
    ApplyAdvice("ApplyAdvice"),
    Automatic("Automatic");

    private String name;

    DRSVmMigrationActivityCause(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
