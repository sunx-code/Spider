package com.sunx.spider.server;

import com.sunx.constant.Configuration;
import com.sunx.constant.Constant;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.dynamic.WebDriverPool;
import com.sunx.storage.DBConfig;
import com.sunx.storage.DBFactory;
import com.sunx.storage.DBUtils;
import com.sunx.storage.pool.DuridPool;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 爬虫主接口
 */
public class Spider {
    //日志记录类
    private static final Logger logger = LoggerFactory.getLogger(Spider.class);
    //绑定数据库链接处理对象
    private DBFactory factory = null;
    //缓存集合,用于存储待抓取的数据
    private List<TaskEntity> queue = new ArrayList<>();
    //缓存验证重复数据
    private Set<Long> cache = new HashSet<>();
    //缓存的最大大小
    private int QUEUE_MAX_SIZE = 1000;
    //缓存最小值,小于这个值会添加数据
    private int QUEUE_MIN_SIZE = 100;
    //缓存数据添加时间间隔
    private int QUEUE_ADD_DURATION = 10000;
    //启动线程数
    private int THREAD_SIZE = 1;
    //线程锁
    private Lock lock = new ReentrantLock();
    //基础类包
    private Map<Long,Class> clMap = new HashMap<>();

    /**
     * main函数
     * 启动爬虫
     * @param args
     */
    public static void main(String[] args){
        //初始化日志处理
//        PropertyConfigurator.configure(System.getProperty("user.dir") + File.separator + Constant.LO4J_CONFIG_PATH);
        //初始化数据库连接池
        DBConfig config = new DBConfig(Constant.DB_CONFIG_FILE);
        DuridPool.me().build(config);

        //启动爬虫
        Spider.me()
              .bind(DBFactory.me())
              .queue()
              .scan()
              .start();
    }

    /**
     * 构建对象
     * @return
     */
    public static Spider me(){
        return new Spider();
    }

    /**
     * 绑定数据库连接对象
     * @param factory
     * @return
     */
    public Spider bind(DBFactory factory){
        this.factory = factory;
        return this;
    }

    /**
     * 初始化queue
     * @return
     */
    public Spider queue(){
        QueueTask task = new QueueTask();
        task.start();

        return this;
    }

    /**
     * 缓存初始化线程
     */
    private class QueueTask extends Thread{
        @Override
        public void run() {
            while(true){
                try{
                    logger.info("开始判断缓存是否需要添加数据...");
                    if(queue.size() <= QUEUE_MIN_SIZE){
                        init();
                    }
                    Thread.sleep(QUEUE_ADD_DURATION);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化缓存
     */
    private void init() throws Exception {
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setStatus(Constant.TASK_NEW);
        logger.info("缓存需要添加新的数据,现在开始从数据库中拉去新的数据添加到集合中...");
        List<TaskEntity> tasks = factory.select(Constant.DEFAULT_DB_POOL,taskEntity);
        if(tasks == null){
            logger.error("数据拉去异常...");
            throw new Exception("从数据库中拉去数据异常...");
        }
        try{
            lock.lock();
            //遍历集合,将数据添加到队列中
            for(TaskEntity task : tasks){
                if(cache.add(task.getId())){
                    queue.add(task);
                }
                if(queue.size() >= QUEUE_MAX_SIZE)break;
            }
            logger.info("当前缓存的大小为:" + queue.size());
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            lock.unlock();
        }
    }

    /**
     * 启动爬虫
     */
    public void start(){
        ExecutorService service = Executors.newFixedThreadPool(THREAD_SIZE);
        for(int i=0;i<THREAD_SIZE;i++){
            service.submit(new Execute());
        }
    }

    /**
     * 自动扫描包,将继承解析接口的类找寻出来
     *
     * 是现在东注入解析
     */
    public Spider scan(){
        String packageName  = IParser.class.getPackage().getName();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        logger.info(packageName + "\t" + path);
        try{
            URL current = null;
            Enumeration<URL> enumeration = classLoader.getResources(path);
            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                //获取到文件名
                current = url;
                break;
            }
            if(Configuration.me().getBoolean("is.test")){
                //获取当前页面的链接地址
                File[] files = new File(current.getFile()).listFiles();
                //拼接文件路径地址
                deal(IParser.class,packageName, files);
            }else{
                String filePath = current.getFile().replaceAll("!/"+ path, "").replaceAll("file:","");
                System.out.println(filePath);
                deal(IParser.class,new JarFile(filePath),path);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return this;
    }

    /**
     * 获取相对路径下的所有的类包
     * @param inter
     * @param packageName
     * @param files
     * @throws Exception
     */
    private void deal(Class inter,String packageName,File[] files) throws Exception{
        for(File f : files){
            if(f.isDirectory()){
                deal(inter,packageName + "." + f.getName(),f.listFiles());
            }else{
                String fileName = f.getName();
                if(fileName.contains("$"))continue;
                fileName = fileName.split("\\.")[0];
                //拼接类路径
                String clzzPath = packageName + "." + fileName;
                //类对象
                deal(inter,Class.forName(clzzPath));
            }
        }
    }

    /**
     *
     * @param inter
     * @param child
     */
    private void deal(Class inter,Class child){
        logger.info(inter.getCanonicalName() + "\t" + child.getCanonicalName());
        //判断类cl是否继承了类inter,同时将继承的子类添加到集合中(除去自己)
        if(inter.isAssignableFrom(child) && !child.equals(inter)){
            //查看这个类是否加入了注解
            Service service = (Service) child.getAnnotation(Service.class);
            if(service != null){
                clMap.put(service.id(),child);

                System.out.println(service.id() + " --> " + child.getCanonicalName());
            }
        }
    }

    /**
     * 从jar文件中读取指定目录下面的所有的class文件
     *
     * @param jarFile   jar文件
     * @param packageName  指定包路径
     * @return 所有的的class的对象
     */
    public void deal(Class inter,JarFile jarFile, String packageName) {
        List<JarEntry> jarEntryList = new ArrayList<>();
        Enumeration<JarEntry> ee = jarFile.entries();
        while (ee.hasMoreElements()) {
            JarEntry entry = ee.nextElement();
            // 过滤我们出满足我们需求的东西
            if (entry.getName().startsWith(packageName) && entry.getName().endsWith(".class")) {
                jarEntryList.add(entry);
            }
        }
        for (JarEntry entry : jarEntryList) {
            String className = entry.getName().replace('/', '.');
            className = className.substring(0, className.length() - 6);
            try {
                Class clz = Thread.currentThread().getContextClassLoader().loadClass(className);
                deal(inter,clz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 数据处理线程
     */
    private class Execute implements Runnable{
        @Override
        public void run() {
            while(true){
                //从缓存中读取一个任务对象
                TaskEntity task = null;
                RemoteWebDriver driver = null;
                try{
                    task = get();
                    if(task == null){
                        logger.info("当前任务队列中任务为空,需要的等待...");
                        Thread.sleep(3000);
                        continue;
                    }
                    //开始处理数据
                    if(!clMap.containsKey(task.getChannelId())){
                        throw new Exception("当前渠道没有对应的解析类,渠道对应的id为:" + task.getChannelId() + ",任务名称为:" + task.getId());
                    }
                    //获取解析类
                    Class clzz = clMap.get(task.getChannelId());
                    //获取指定方法
                    Method method = clzz.getMethod("parser",DBFactory.class,RemoteWebDriver.class,TaskEntity.class);
                    driver = (RemoteWebDriver)WebDriverPool.me().get();
                    if(driver == null){
                        logger.info("获取驱动对象失败......");
                        Thread.sleep(3000);
                        continue;
                    }
                    //开始执行数据处理
                    Integer status = (Integer)method.invoke(clzz.newInstance(),factory,driver,task);
                    //更新任务为成功状态
                    factory.update(Constant.DEFAULT_DB_POOL, DBUtils.table(task),
                            new String[]{"status"},
                            new Object[]{status},
                            new String[]{"id"},
                            new Object[]{task.getId()});
                }catch (Exception e){
                    e.printStackTrace();
                    logger.error(e.getMessage());
                    if(task != null){
                        //更新任务为失败状态
                        factory.update(Constant.DEFAULT_DB_POOL, DBUtils.table(task),
                                new String[]{"status"},
                                new Object[]{Constant.TASK_FAIL},
                                new String[]{"id"},
                                new Object[]{task.getId()});
                    }
                }finally{
                    //回收对象
                    if(driver != null){
                        WebDriverPool.me().recycle(driver);
                    }
                    //删除缓存中保留的对象id
                    if(task != null){
                        remove(task.getId());
                    }
                }
            }
        }
    }

    /**
     * 从队列中读取一个数据
     * @return
     */
    private TaskEntity get(){
        try{
            lock.lock();
            if(queue.isEmpty())return null;
            return queue.remove(0);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return null;
    }

    /**
     * 从队列中处理好数据后,开始删除掉缓存中的数据
     * @param id
     */
    private void remove(long id){
        try{
            lock.lock();
            cache.remove(id);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }
}