package mitb.util;

import jdk.internal.dynalink.beans.StaticClass;
import mitb.TitanBot;
import mitb.event.*;

import javax.script.ScriptEngine;

public class ScriptingHelper {
    public void register(ScriptEngine engine, String method, StaticClass event) {
        EventHandler.register(engine, method, (Class<? extends Event>) event.getRepresentedClass());
    }

    public void respond(ProxyEvent event, String msg) {
        TitanBot.sendReply(event.getSource(), msg);
    }

    public void respond(ProxyEvent event, String msg, String suffix) {
        TitanBot.sendReply(event.getSource(), msg, suffix);
    }

    public void respond(ProxyEvent event, String msg, boolean ignoreRate) {
        TitanBot.sendReply(event.getSource(), msg, ignoreRate);
    }
}
