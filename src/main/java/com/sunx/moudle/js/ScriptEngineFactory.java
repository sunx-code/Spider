package com.sunx.moudle.js;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * 对象构造工厂
 */
public class ScriptEngineFactory extends BasePooledObjectFactory<ScriptEngine> {
    //解析器对象
    private ScriptEngineManager manager = null;

    public ScriptEngineFactory(){
        manager = new ScriptEngineManager();
    }

    @Override
    public ScriptEngine create() throws Exception {
        return manager.getEngineByName("JavaScript");
    }

    @Override
    public PooledObject<ScriptEngine> wrap(ScriptEngine obj) {
        return new DefaultPooledObject<>(obj);
    }
}
