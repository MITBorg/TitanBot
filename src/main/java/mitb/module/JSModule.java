package mitb.module;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.Invocable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JSModule implements InvocationHandler {
    private final ScriptObjectMirror scriptObjectMirror;
    private final Invocable proxy;
    private final String name;

    public JSModule(Invocable proxy, ScriptObjectMirror scriptObjectMirror, String name) {
        this.proxy = proxy;
        this.scriptObjectMirror = scriptObjectMirror;
        this.name = name;
    }

    public ScriptModule proxy() {
        return (ScriptModule) Proxy.newProxyInstance(ScriptModule.class.getClassLoader(), new Class[]{ ScriptModule
                .class }, this);
    }

    public ScriptCommandModule commandProxy() {
        return (ScriptCommandModule) Proxy.newProxyInstance(ScriptModule.class.getClassLoader(), new Class[]{
                ScriptCommandModule.class }, this);
    }

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("getName")) {
            return this.name;
        }

        return this.proxy.invokeMethod(scriptObjectMirror, method.getName(), args);
    }
}
