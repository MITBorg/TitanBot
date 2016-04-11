package mitb.module;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.Invocable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JSModule implements InvocationHandler {
    private final ScriptObjectMirror scriptObjectMirror;
    private final Invocable proxy;

    public JSModule(Invocable proxy, ScriptObjectMirror scriptObjectMirror) {
        this.proxy = proxy;
        this.scriptObjectMirror = scriptObjectMirror;
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
        return this.proxy.invokeMethod(scriptObjectMirror, method.getName(), args);
    }
}
