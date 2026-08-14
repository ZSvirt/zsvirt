package org.zstack.sdk.identity.imports.header;

import org.zstack.sdk.identity.imports.header.ImportStage;
import org.zstack.sdk.identity.imports.header.CleanStage;

public class SyncTaskResult  {

    public java.lang.String sourceUuid;
    public void setSourceUuid(java.lang.String sourceUuid) {
        this.sourceUuid = sourceUuid;
    }
    public java.lang.String getSourceUuid() {
        return this.sourceUuid;
    }

    public java.lang.String sourceType;
    public void setSourceType(java.lang.String sourceType) {
        this.sourceType = sourceType;
    }
    public java.lang.String getSourceType() {
        return this.sourceType;
    }

    public ImportStage importStage;
    public void setImportStage(ImportStage importStage) {
        this.importStage = importStage;
    }
    public ImportStage getImportStage() {
        return this.importStage;
    }

    public CleanStage cleanStage;
    public void setCleanStage(CleanStage cleanStage) {
        this.cleanStage = cleanStage;
    }
    public CleanStage getCleanStage() {
        return this.cleanStage;
    }

}
