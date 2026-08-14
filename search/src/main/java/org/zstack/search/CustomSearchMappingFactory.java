package org.zstack.search;

import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.cfg.*;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.search.SearchConstant;
import org.zstack.search.schema.AnalyzerDefType;
import org.zstack.search.schema.FilterType;
import org.zstack.search.schema.IndexType;
import org.zstack.search.schema.Indexes;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.lang.annotation.ElementType;
import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 14:48 2020/11/19
 */
public class CustomSearchMappingFactory {

    public CustomSearchMappingFactory() {
    }

    private String defaultAnalyzerName;

    @Factory
    public SearchMapping getSearchMapping() {
        SearchMapping mapping = new SearchMapping();
        try {
            JAXBContext context = JAXBContext.newInstance("org.zstack.search.schema");
            File cfg = PathUtil.findFileOnClassPath(SearchConstant.INDEX_CONFIG_PATH, true);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Indexes schema = (Indexes) unmarshaller.unmarshal(cfg);
            List<IndexType> indexes = schema.getIndex();
            List<AnalyzerDefType> analyzerDefs = schema.getAnalyzerDef();
            defaultAnalyzerName = schema.getDefaultAnalyzerName();

            for (AnalyzerDefType analyzerDefType : analyzerDefs) {
                constructAnalyzerDef(mapping, analyzerDefType);
            }

            for (IndexType index : indexes) {
                Class entityClass = Class.forName(index.getName());
                if (index.isBaseClass()) {
                    constructBaseIndex(mapping, entityClass, index.getProp());
                } else {
                    constructIndex(mapping, entityClass, index.getProp());
                }
            }
        } catch (JAXBException e) {
            throw new CloudRuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException(String.format("build search mapping failed, %s", e));
        }

        return mapping;
    }

    private void constructAnalyzerDef(SearchMapping mapping, AnalyzerDefType analyzerDefType) throws ClassNotFoundException {
        AnalyzerDefMapping analyzerDefMapping = mapping.analyzerDef(analyzerDefType.getName(),
                (Class<? extends TokenizerFactory>) Class.forName(analyzerDefType.getAnalyzer().getFactory()));
        for (FilterType type : analyzerDefType.getFilter()) {
            TokenFilterDefMapping filterDefMapping = analyzerDefMapping.filter((Class<? extends TokenFilterFactory>) Class.forName(type.getFactory()));
            for (FilterType.Param param : type.getParam()) {
                filterDefMapping.param(param.getName(), param.getValue());
            }
        }
    }

    private void constructBaseIndex(SearchMapping mapping, Class entity, List<IndexType.Prop> props) {
        EntityMapping entityMapping = mapping.entity(entity);
        for (IndexType.Prop prop : props) {
            entityMapping.property(prop.getName(), ElementType.METHOD)
                    .field()
                        .analyzer(prop.getAnalyzer() == null? defaultAnalyzerName : prop.getAnalyzer());
        }
    }

    private void constructIndex(SearchMapping mapping, Class entity, List<IndexType.Prop> props) {
        IndexedMapping indexedMapping = mapping.entity(entity).indexed();
        for (IndexType.Prop prop : props) {
            indexedMapping.property(prop.getName(), ElementType.METHOD)
                    .field()
                    .analyzer(prop.getAnalyzer() == null? defaultAnalyzerName : prop.getAnalyzer());
        }
    }
}
