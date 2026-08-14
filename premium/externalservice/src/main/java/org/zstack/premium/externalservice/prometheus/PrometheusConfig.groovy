package org.zstack.premium.externalservice.prometheus

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.NodeTuple
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import org.zstack.utils.gson.JSONObjectUtil

import java.util.function.Supplier

class PrometheusConfig {
    private transient List<Supplier<Boolean>> rebootJudgers = []

    boolean yarmRuleConfig = false

    static class Global {
        String scrape_interval
        String scrape_timeout
        String evaluation_interval
        Map<String, String> external_labels

        void externalLabel(String k, String v) {
            if (external_labels == null) {
                external_labels = [:]
            }

            external_labels[k] = v
        }

        void update(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Global.class) Closure c) {
            c.delegate = this
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()
        }
    }

    static class RemoteURL {
        String url
    }

    private Global global
    private List<RemoteURL> remote_write
    private List<RemoteURL> remote_read

    PrometheusConfig() {
        global = new Global()
        global.scrape_interval = "10s"
        global.evaluation_interval = "10s"
        global.scrape_timeout = "5s"
        if (PrometheusGlobalProperty.SUPPORT_THANOS) {
            global.externalLabel("cluster", "thanos")
            global.externalLabel("monitor", "zstack")
            global.externalLabel("replica", "A")
        }

        scrapeConfigs = [:]
    }

    static class FileSDConfig {
        public Set<String> files
        public String refresh_interval = "10s"

        void file(String v) {
            if (files == null) {
                files = []
            }

            files.add(v)
        }

        void update(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = FileSDConfig.class) Closure c) {
            c.delegate = this
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()
        }
    }

    static class StaticConfig {
        public Set<String> targets
        public Map<String, String> labels

        void target(String v) {
            if (targets == null) {
                targets = []
            }

            targets.add(v)
        }

        void label(String k, String v) {
            if (labels == null) {
                labels = [:]
            }

            labels[k] = v
        }

        void update(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = StaticConfig.class) Closure c) {
            c.delegate = this
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()
        }
    }

    boolean isYarmRuleConfig() {
        return yarmRuleConfig
    }

    void setYarmRuleConfig(boolean yarmRuleConfig) {
        this.yarmRuleConfig = yarmRuleConfig
    }

    static class RelabelConfig {
        public Set<String> source_labels
        public String separator = ";"
        public String target_label
        public String regex = "(.*)"
        public Long modulus
        public String replacement = "\$1"
        public String action = "replace"

        void sourceLabel(String v) {
            if (source_labels == null) {
                source_labels = []
            }

            source_labels.add(v)
        }

        void update(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RelabelConfig.class) Closure c) {
            c.delegate = this
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()
        }
    }

    static class BasicAuth {
        public String username
        public String password
    }

    static class TLSConfig {
        public String ca_file
        public String cert_file
        public String key_file
        public String server_name
        public Boolean insecure_skip_verify
    }

    static class ScrapeConfig {
        public String job_name
        public String scrape_interval = "10s"
        public String scrape_timeout = "5s"
        public String metrics_path
        public Boolean honor_labels
        public String scheme
        public Set<String> params
        public BasicAuth basic_auth
        public String bearer_token
        public String bearer_token_file
        public TLSConfig tls_config
        public List<FileSDConfig> file_sd_configs
        public List<StaticConfig> static_configs
        public List<RelabelConfig> relabel_configs
        public RelabelConfig metric_relabel_config
        public Integer sample_limit

        void tlsConfig(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = TLSConfig.class) Closure c) {
            tls_config = new TLSConfig()
            c.delegate = tls_config
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()
        }

        void basicAuth(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = BasicAuth.class) Closure c) {
            basic_auth = new BasicAuth()

            c.delegate = basic_auth
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()
        }

        void update(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ScrapeConfig.class) Closure c) {
            c.delegate = this
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()
        }

        void metricRelabelConfig(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RelabelConfig.class) Closure c) {
            metric_relabel_config = new RelabelConfig()
            c.delegate = metric_relabel_config
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()
        }

        void relabelConfig(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RelabelConfig.class) Closure c) {
            c.delegate = new RelabelConfig()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            if (relabel_configs == null) {
                relabel_configs = []
            }

            relabel_configs.add(c.delegate)
        }

        void staticConfig(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = StaticConfig.class) Closure c) {
            c.delegate = new StaticConfig()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            if (static_configs == null) {
                static_configs = []
            }

            static_configs.add(c.delegate)
        }

        void fileSDConfig(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = FileSDConfig.class) Closure c) {
            c.delegate = new FileSDConfig()
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            if (file_sd_configs == null) {
                file_sd_configs = []
            }

            file_sd_configs.add(c.delegate)
        }

        void param(String v) {
            if (params == null) {
                params = []
            }

            params.add(v)
        }

        @Override
        int hashCode() {
            return job_name.hashCode()
        }

        @Override
        boolean equals(Object sc) {
            return sc instanceof ScrapeConfig && job_name == (((ScrapeConfig)sc).job_name)
        }
    }

    private Set<String> ruleFiles

    void ruleFile(String v) {
        if (v == null) {
            return
        }

        if (ruleFiles == null) {
            ruleFiles = []
        }

        ruleFiles.add(v)
    }

    void update(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = PrometheusConfig.class) Closure c) {
        c.delegate = this
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
    }

    private Map<String, ScrapeConfig> scrapeConfigs

    void scrapeConfig(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ScrapeConfig.class) Closure c) {
        def sc = new ScrapeConfig()
        c.delegate = sc
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        if (scrapeConfigs == null) {
            scrapeConfigs = [:]
        }

        scrapeConfigs[sc.job_name] = sc
    }

    private class YamlObject {
        Global global
        List<String> rule_files
        List<ScrapeConfig> scrape_configs
        List<RemoteURL> remote_write
        List<RemoteURL> remote_read
    }

    private static class MyRepresenter extends Representer {
        MyRepresenter(DumperOptions options) {
            super(options)
        }

        @Override
        protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
            if (propertyValue == null) {
                return null
            } else if (propertyValue instanceof Map && ((Map)property).isEmpty()) {
                return null
            } else if (property instanceof Collection && ((Collection)propertyValue).isEmpty()) {
                return null
            } else {
                return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag)
            }
        }

        @Override
        protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
            if (!classTags.containsKey(javaBean.getClass())) {
                addClassTag(javaBean.getClass(), Tag.MAP)
            }

            return super.representJavaBean(properties, javaBean)
        }
    }

    @Override
    String toString() {
        YamlObject obj = new YamlObject()
        obj.global = global
        obj.remote_read = remote_read
        obj.remote_write = remote_write

        if (ruleFiles != null && !ruleFiles.isEmpty()) {
            obj.rule_files = []
            obj.rule_files.addAll(ruleFiles)
        }

        if (scrapeConfigs != null && !scrapeConfigs.isEmpty()) {
            obj.scrape_configs = []
            obj.scrape_configs.addAll(scrapeConfigs.values())
        }

        DumperOptions opts = new DumperOptions()
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
        opts.setCanonical(false)
        opts.setIndent(2)
        opts.setPrettyFlow(true)
        return new Yaml(new MyRepresenter(opts), opts).dump(JSONObjectUtil.rehashObject(obj, LinkedHashMap.class))
    }

    void rebootPrometheusWhenManagmentNodeStartIf(Supplier<Boolean> r) {
        rebootJudgers.add(r)
    }

    List<Supplier<Boolean>> getRebootJugers() {
        return rebootJudgers
    }

    Global getGlobal() {
        return global
    }

    void remoteWrite(String url) {
        remote_write = [new RemoteURL(url: url)]
    }

    void remoteRead(String url) {
        remote_read = [new RemoteURL(url: url)]
    }
}
