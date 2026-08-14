
package org.zstack.search.schema;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.zstack.search.schema package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.zstack.search.schema
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link FilterType }
     * 
     */
    public FilterType createFilterType() {
        return new FilterType();
    }

    /**
     * Create an instance of {@link IndexType }
     * 
     */
    public IndexType createIndexType() {
        return new IndexType();
    }

    /**
     * Create an instance of {@link AnalyzerDefType }
     * 
     */
    public AnalyzerDefType createAnalyzerDefType() {
        return new AnalyzerDefType();
    }

    /**
     * Create an instance of {@link Indexes }
     * 
     */
    public Indexes createIndexes() {
        return new Indexes();
    }

    /**
     * Create an instance of {@link FilterType.Param }
     * 
     */
    public FilterType.Param createFilterTypeParam() {
        return new FilterType.Param();
    }

    /**
     * Create an instance of {@link IndexType.Prop }
     * 
     */
    public IndexType.Prop createIndexTypeProp() {
        return new IndexType.Prop();
    }

    /**
     * Create an instance of {@link AnalyzerDefType.Analyzer }
     * 
     */
    public AnalyzerDefType.Analyzer createAnalyzerDefTypeAnalyzer() {
        return new AnalyzerDefType.Analyzer();
    }

}
