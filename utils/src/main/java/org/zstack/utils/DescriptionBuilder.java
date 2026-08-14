package org.zstack.utils;

import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.lang.reflect.Field;


public class DescriptionBuilder {
    private final StringBuilder _builder;
    private Object _obj = null;
    private boolean _isAscend = true;
    private static final CLogger _logger = CLoggerImpl.getLogger(DescriptionBuilder.class);
    
    public DescriptionBuilder() {
       _builder = new StringBuilder(); 
    }
    
    public DescriptionBuilder(String str) {
       _builder = new StringBuilder(str); 
    }
    
    public DescriptionBuilder append(String str) {
       _builder.append(str);
       return this;
    }
    
    public DescriptionBuilder setDescribedObject(Object obj) {
        _obj = obj;
        return this;
    }
    
    public DescriptionBuilder setIsAscend(boolean is) {
        _isAscend = is;
        return this;
    }
    
    public String build() {
       if (_obj != null) {
           Class<?> currClass = _obj.getClass();
           _builder.append(" with [");
           do {
                try {
                    for (Field f : currClass.getDeclaredFields()) {
                        Object val = f.get(_obj).toString();
                        if (val != null) {
                            _builder.append(String.format(" %$1s = %2$s,", f.getName(), val.toString()));
                        } else {
                            _builder.append(String.format(" %$1s = null,", f.getName()));
                        }
                    }

                    if (!_isAscend) {
                        break;
                    }
                } catch (Exception e) {
                    _logger.warn("Exception when describing object: " + _obj.getClass().getCanonicalName(), e);
                    continue;
                }
              currClass = currClass.getSuperclass();
           } while (currClass != Object.class && currClass != null);
           _builder.append("]");
       }
       
       return _builder.toString();
    }
}
