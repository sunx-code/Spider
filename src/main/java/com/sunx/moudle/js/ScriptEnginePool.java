package com.sunx.moudle.js;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

/**
 * phantomjs渠道对象池
 */
public class ScriptEnginePool {
    //构造对象数
    private static int OBJECT_POOL_MAX_NUM = 50;
    //ScriptEngine对象池
    private GenericObjectPool<ScriptEngine> pool = null;
    //对象构建工厂
    private ScriptEngineFactory scriptEngineFactory = null;

    /**
     * 构造函数初始化对象池
     */
    public ScriptEnginePool(){
        pool = initPool();
    }

    /**
     * 构造对象池
     * @return
     */
    private GenericObjectPool<ScriptEngine> initPool(){
        scriptEngineFactory = new ScriptEngineFactory();
		//对象工厂
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(OBJECT_POOL_MAX_NUM); //整个池最大值
        config.setMaxWaitMillis(1000); //获取不到永远不等待
        config.setTimeBetweenEvictionRunsMillis(5 * 60 * 1000L); //-1不启动。默认1min一次
        config.setMinEvictableIdleTimeMillis(10 * 60000L); //可发呆的时间,10mins

        return new GenericObjectPool<>(scriptEngineFactory,config);
	}

    /**
     * 获取数据
     * @return
     */
    public ScriptEngine get(){
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 回收数据
     * @param engine
     */
    public void recycle(ScriptEngine engine){
        engine.getBindings(ScriptContext.ENGINE_SCOPE).clear();
        pool.returnObject(engine);
    }
    /**
     * 关闭对象池
     */
    public void close(){
        pool.close();
    }
    /**
     * 清空对象池
     */
    public void clear(){
        pool.clear();
    }
}
