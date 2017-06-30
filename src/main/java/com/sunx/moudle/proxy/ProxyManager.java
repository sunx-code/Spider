package com.sunx.moudle.proxy;

import com.sunx.constant.Configuration;
import com.sunx.downloader.Downloader;
import com.sunx.downloader.HttpClientDownloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public class ProxyManager {
    //日志记录类
    private static final Logger logger = LoggerFactory.getLogger(ProxyManager.class);
    private PriorityBlockingQueue<IProxy> queue = new PriorityBlockingQueue<>(50,new MySort());
    private int DEFAULT_PROXY_TASK_DURATION = 1000 * 5;
    private Downloader downloader = new HttpClientDownloader();
    private Request request = new Request();
    private Site site = new Site();
    //每个代理可用的时间
    private static int PROXY_HAVING_TIME = 1000 * 30;

    private Lock lock = new ReentrantLock();

    private Map<Long, PriorityBlockingQueue<IProxy>> proxys = new HashMap<>();
    private AtomicInteger cnt = new AtomicInteger(0);

    /**
     */
    private static class SingleClass{
        private static ProxyManager manager = new ProxyManager();
    }

    public static ProxyManager me(){
        return SingleClass.manager;
    }

    /**
     */
    private ProxyManager(){
        request.setUrl(Configuration.me().getString("proxy.url"));
        ProxyTask task = new ProxyTask();
        Timer timer = new Timer();
        timer.schedule(task,0,DEFAULT_PROXY_TASK_DURATION);
    }

    /**
     * @return
     */
    public IProxy poll(long cid){
        try{
            lock.lock();
            IProxy proxy = queue.poll();
            if(proxy == null || !proxy.isFlag())return null;
            //是否已经使用超过30秒
            long current = System.currentTimeMillis() - proxy.getCreateAt();
            if(current < PROXY_HAVING_TIME && proxy.getCnt() > 180)return null;
            proxy.setCnt(proxy.getCnt() + 1);
            queue.offer(proxy);
            return proxy;
//            if(!proxys.containsKey(cid))return null;
//            PriorityBlockingQueue<IProxy> list = proxys.get(cid);
//            IProxy proxy = list.poll();
//            if(proxy == null || !proxy.isFlag()){
//                cnt.getAndAdd(-1);
//                return null;
//            }
//            //是否已经使用超过30秒
//            long current = System.currentTimeMillis() - proxy.getCreateAt();
//            if(current < PROXY_HAVING_TIME && proxy.getCnt() > 180){
//                cnt.getAndAdd(-1);
//                return null;
//            }
//            proxy.setCnt(proxy.getCnt() + 1);
//            list.offer(proxy);
//            return proxy;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return null;
    }

    /**
     *
     * @param src
     */
    public void deal(String src){
        if(src == null || src.length() <= 5){
            logger.info(src);
            return;
        }
        String[] lines = src.split("[\n\r]");
        if(lines == null)return;
        for(String line : lines){
            try{
                String[] tmps = line.split("\\:");
                if(tmps == null || tmps.length != 2)continue;
                String ip = tmps[0];
                int port = Integer.parseInt(tmps[1]);

                IProxy proxy = new IProxy();
                proxy.setHost(ip);
                proxy.setPort(port);
                proxy.setCnt(0);
                proxy.setCreateAt(System.currentTimeMillis());
                proxy.setFlag(true);

                if(queue.size() < 30){
                    this.queue.offer(proxy);
                }
//                for(long i=1;i<=10;i++){
//                    try{
//                        lock.lock();
//                        if(!proxys.containsKey(i)){
//                            proxys.put(i,new PriorityBlockingQueue<>(100,new MySort()));
//                        }
//                        //将最新的ip放置在最开始的位置
//                        proxys.get(i).offer(proxy);
//                    }catch (Exception e){
//                        e.printStackTrace();
//                    }finally {
//                        lock.unlock();
//                    }
//                }
//                cnt.getAndAdd(1);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        logger.info("当前代理队列的大小为：" + this.queue.size());
//        logger.info("当前代理队列的大小为：" + cnt.get());
    }

    /**
     */
    private class ProxyTask extends TimerTask{
        @Override
        public void run() {
            try{
                String src = downloader.downloader(request,site);
                if(src == null || src.length() <= 0)return;
                deal(src);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public class MySort implements Comparator<IProxy> {
        @Override
        public int compare(IProxy o1, IProxy o2) {
            return (int)(o1.getCreateAt() - o2.getCreateAt());
        }
    }

    public static void main(String[] args){
        ProxyManager.me();
//        logger.info("出现错误..");
//        logger.error("error...");
    }
}
