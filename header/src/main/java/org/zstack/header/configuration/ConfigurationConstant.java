package org.zstack.header.configuration;

public interface ConfigurationConstant {
    public static final String SERVICE_ID = "configuration";
    public static final String USER_VM_INSTANCE_OFFERING_TYPE = "UserVm";

    public static final String ACTION_CATEGORY = "configuration";

    public static enum GlobalConfig {
        publicKey("key.public"),
        privateKey("key.private");

        private final String name;

        @Override
        public String toString() {
            return name;
        }

        public String getName() {
            return name;
        }

        private GlobalConfig(String name) {
            this.name = name;
        }

        public String getCategory() {
            return "configuration";
        }
    }
}
