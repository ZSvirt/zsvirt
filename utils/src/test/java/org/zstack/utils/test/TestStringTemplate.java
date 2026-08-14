package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.data.StringTemplate;

import java.util.HashMap;
import java.util.Map;

public class TestStringTemplate {

    @Test
    public void test() {
        String text = "{name} to {boy} is {xx}";
        String boy = "hello\n world\n";
        String name = "you";
        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("name", name);
        tokens.put("boy", boy);
        String res = StringTemplate.substitute(text, tokens);
        System.out.println(res);

        System.out.println("file:///mnt".replaceAll("file://", ""));
    }

}
