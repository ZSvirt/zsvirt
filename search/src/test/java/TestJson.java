import org.json.JSONException;
import org.junit.Test;
import org.zstack.header.search.InventoryDoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TestJson {
    public class A {
        int a;
        List<Long> b;
    }
    
    public class B {
        int a;
        Object b;
    }
    
    private void pr(String str) {
        System.out.println(str);
    }
    
    @Test
    public void test() throws JSONException, IOException {
        A a = new A();
        a.a = 1;
        a.b = new ArrayList<Long>();
        a.b.add(10L);
        a.b.add(100L);
        
        String str = InventoryDoc.getGson().toJson(a);
        B b = InventoryDoc.getGson().fromJson(str, B.class);
        pr(b.b.toString());
    }
}
