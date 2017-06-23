package com.sunx.spider.server;

import com.sunx.constant.Configuration;
import com.sunx.constant.Constant;
import com.sunx.entity.ServiceEntity;
import com.sunx.utils.TimerUtils;
import com.sunx.storage.DBConfig;
import com.sunx.storage.DBFactory;
import com.sunx.storage.pool.DuridPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 监控模块,导入种子链接地址,并将相应的数据写入到数据库中
 * 1 启动监控模块
 * 2 通过配置文件,拉去配置文件中的站点集合,通过反射来实例化相应的实例,进行调用
 * 3 将结果写入到数据库中
 * 4 记录日志到monitor的日志中
 */
public class Monitor {
    //日志记录类
    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);
    //数据库操作对象
    private DBFactory factory = null;
    //服务队列
    private ConcurrentLinkedQueue<ServiceEntity> queue = new ConcurrentLinkedQueue<ServiceEntity>();

    /**
     * 用于初始化任务类
     * @param args
     */
    public static void main(String[] args) {
        //初始化数据库连接池
        DBConfig config = new DBConfig(Constant.DB_CONFIG_FILE);
        DuridPool.me().build(config);
        //构造对象
        Monitor.me()
               .bind(DBFactory.me())
               .start()
               .monitor();
    }

    /**
     * 构建对象
     * @return
     */
    public static Monitor me(){
        return new Monitor();
    }

    /**
     * 绑定数据库操作对象
     * @param factory
     * @return
     */
    private Monitor bind(DBFactory factory){
        this.factory = factory;
        return this;
    }

    /**
     * 初始化服务数据
     */
    private Monitor start(){
        ServiceTask timer = new ServiceTask();
        timer.start();

        return this;
    }

    /**
     * 服务拉去线程
     */
    private class ServiceTask extends Thread{
        @Override
        public void run() {
            while(true){
                try{
                    logger.info("开始从数据库中拉去服务队列列表...");
                    ServiceEntity entity = new ServiceEntity();
                    entity.setStatus(1);

                    List<ServiceEntity> services = factory.select(Constant.DEFAULT_DB_POOL,entity);
                    //对从数据库中拉去到的结果进行判定...
                    if(services == null || services.size() <= 0){
                        logger.error("拉去数据封装到集合中为null,清查看...");
                    }else{
                        //将数据添加到集合中
                        for(ServiceEntity e : services){
                            queue.offer(e);
                        }
                        logger.info("当前队列中的大小为:" + queue.size());
                    }
                    //线程休眠一定时间后继续
                    long current = System.currentTimeMillis();
                    long sleep = TimerUtils.period(current);

                    logger.info("线程将要休眠一定时间后继续:" + sleep + "ms");
                    Thread.sleep(sleep);
                }catch (Exception e){
                    e.printStackTrace();
                    logger.error(e.getMessage());
                }
            }
        }
    }

    /**
     * 监控模块的列表
     * 1 启动多个线程,用于处理任务数
     * 2 从缓存中拉去一个服务
     * 3 反射调用服务的一个方法
     */
    private void monitor(){
        int threadNum = Configuration.me().getInt("thread.num");
        ExecutorService service = Executors.newFixedThreadPool(threadNum);
        for(int i=0;i<threadNum;i++){
            service.submit(new Exec());
        }
    }

    /**
     * 处理线程
     *
     */
    private class Exec implements Runnable{
        public void run() {
            while(true){
                try{
                    logger.info("从队列中获取一个服务...");
                    //从队列中获取一个服务
                    ServiceEntity entity = queue.poll();
                    if(entity == null){
                        logger.info("当前队列中为空,获取服务失败,线程等待一定时间后继续...");
                        Thread.sleep(5000);
                        continue;
                    }
                    //开始反射执行服务
                    Class clzz = Class.forName(entity.getService());
                    Method method =clzz.getMethod("monitor",DBFactory.class,Long.class,String.class);
                    method.invoke(clzz.newInstance(),factory,entity.getId(),entity.getName());
                }catch (Exception e){
                    e.printStackTrace();
                    logger.error(e.getMessage());
                }
            }
        }
    }
}
