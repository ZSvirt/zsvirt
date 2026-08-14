import org.codehaus.groovy.control.CompilerConfiguration
import org.junit.Test
import org.kohsuke.groovy.sandbox.GroovyInterceptor
import org.kohsuke.groovy.sandbox.SandboxTransformer
import org.kohsuke.groovy.sandbox.impl.Super

class Test5 {

    static class MyBinding extends Binding {
        void sayHello() {
            println("hello world")
        }
    }

    static class SandBox extends GroovyInterceptor {
        static List<Class> RECEIVER_WHITE_LIST = [
                Number.class,
                long.class,
                int.class,
                short.class,
                double.class,
                float.class,
                String.class,
                Date.class,
                Map.class,
                Collection.class,
                Script.class
        ]

        static void checkReceiver(Object obj) {
            checkReceiver(obj.getClass())
        }

        static void checkReceiver(Class clz) {
            if (clz.name.startsWith("org.zstack")) {
                return
            }

            for (Class wclz : RECEIVER_WHITE_LIST) {
                if (wclz.isAssignableFrom(clz)) {
                    return
                }
            }

            throw new Exception("invalid operation on class[${clz.name}]")
        }

        Object onMethodCall(GroovyInterceptor.Invoker invoker, Object receiver, String method, Object... args) throws Throwable {
            checkReceiver(receiver)
            return super.onMethodCall(invoker, receiver, method, args)
        }

        Object onStaticCall(GroovyInterceptor.Invoker invoker, Class receiver, String method, Object... args) throws Throwable {
            checkReceiver(receiver)
            return super.onStaticCall(invoker, receiver, method, args)
        }

        Object onNewInstance(GroovyInterceptor.Invoker invoker, Class receiver, Object... args) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, (String)null, (Object[])args);
        }

        Object onSuperCall(GroovyInterceptor.Invoker invoker, Class senderType, Object receiver, String method, Object... args) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(new Super(senderType, receiver), method, (Object[])args);
        }

        void onSuperConstructor(GroovyInterceptor.Invoker invoker, Class receiver, Object... args) throws Throwable {
            checkReceiver(receiver)
            this.onNewInstance(invoker, receiver, args);
        }

        Object onGetProperty(GroovyInterceptor.Invoker invoker, Object receiver, String property) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, property);
        }

        Object onSetProperty(GroovyInterceptor.Invoker invoker, Object receiver, String property, Object value) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, property, value);
        }

        Object onGetAttribute(GroovyInterceptor.Invoker invoker, Object receiver, String attribute) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, attribute);
        }

        Object onSetAttribute(GroovyInterceptor.Invoker invoker, Object receiver, String attribute, Object value) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, attribute, value);
        }

        Object onGetArray(GroovyInterceptor.Invoker invoker, Object receiver, Object index) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, (String)null, (Object)index);
        }

        Object onSetArray(GroovyInterceptor.Invoker invoker, Object receiver, Object index, Object value) throws Throwable {
            checkReceiver(receiver)
            return invoker.call(receiver, (String)null, index, value);
        }
    }

    @Test
    void test1() {
        def cc = new CompilerConfiguration()
        cc.addCompilationCustomizers(new SandboxTransformer())
        def binding = new MyBinding()
        binding.setVariable("say", { println(it) })
        GroovyShell shell = new GroovyShell(binding, cc)
        def sandbox = new SandBox()

        sandbox.register()
        try {
            shell.evaluate("""import org.apache.commons.lang.StringUtils
import org.zstack.sdk.VmInstanceInventory
say("hi")
List lst = [1, 2, 3, 4]
lst.each { say(it) }
def vm = new VmInstanceInventory()
vm.name = "hi"
Class.forName('java.lang.System').exit(-1)
""")
        } finally {
            sandbox.unregister()
        }
    }
}
