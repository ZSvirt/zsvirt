package org.zstack.identity.imports.header;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public class SyncTaskResult {
    private String sourceUuid;
    private String sourceType;

    public static class ImportStage {
        private int total;
        private int success;
        private int fail;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getSuccess() {
            return success;
        }

        public void setSuccess(int success) {
            this.success = success;
        }

        public int getFail() {
            return fail;
        }

        public void setFail(int fail) {
            this.fail = fail;
        }
    }
    protected ImportStage importStage = new ImportStage();

    public static class CleanStage {
        private int total;
        private int success;
        private int skip;
        private int fail;

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getSuccess() {
            return success;
        }

        public void setSuccess(int success) {
            this.success = success;
        }

        public int getSkip() {
            return skip;
        }

        public void setSkip(int skip) {
            this.skip = skip;
        }

        public int getFail() {
            return fail;
        }

        public void setFail(int fail) {
            this.fail = fail;
        }
    }
    protected CleanStage cleanStage = new CleanStage();

    public String getSourceUuid() {
        return sourceUuid;
    }

    public void setSourceUuid(String sourceUuid) {
        this.sourceUuid = sourceUuid;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public ImportStage getImportStage() {
        return importStage;
    }

    public void setImportStage(ImportStage importStage) {
        this.importStage = importStage;
    }

    public CleanStage getCleanStage() {
        return cleanStage;
    }

    public void setCleanStage(CleanStage cleanStage) {
        this.cleanStage = cleanStage;
    }
}
