package org.zstack.utils.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class StringTrimAdapter extends XmlAdapter<String, String> {
    @Override
    public String unmarshal(String v) throws Exception {
        if (v == null) {
            return null;
        }
        return v.trim();
    }
    @Override
    public String marshal(String v) throws Exception {
        if (v == null) {
            return null;
        }
        return v.trim();
    }
}
