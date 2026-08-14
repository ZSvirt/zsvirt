package org.zstack.mevoco;

import org.zstack.core.config.GlobalConfig;

/**
 * Created by liangbo.zhou on 17-7-3.
 */
@Deprecated
public class PremiumGlobalConfig extends GlobalConfig{

    public PremiumGlobalConfig(String category, String name) {
        super(category, name);
    }

    @Override
    public void updateValue(Object val) {
        super.updateValue(val);
    }

    @Override
    public  <T> T value(Class<T> clz){
        return super.value(clz);
    }

    @Override
    public String value(){
        return super.value();
    }
}
