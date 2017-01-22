package com.sunx.moudle.proxy;

import com.sunx.constant.Configuration;
import com.sunx.downloader.Downloader;
import com.sunx.downloader.HttpClientDownloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */
public class ProxyManager {
    private String BASE = "http://dps.kuaidaili.com/api/getdps/?orderid=ORDER_NUM&num=100&ut=1&sep=1";
    private ConcurrentLinkedQueue<IProxy> queue = new ConcurrentLinkedQueue<IProxy>();
    private int DEFAULT_PROXY_TASK_DURATION = 1000 * 10;
    private Downloader downloader = new HttpClientDownloader();
    private Request request = new Request();
    private Site site = new Site();

    private static int MAX_SIZE = 1000;
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
        ProxyTask task = new ProxyTask();
        Timer timer = new Timer();
        timer.schedule(task,0,DEFAULT_PROXY_TASK_DURATION);
    }

    /**
     * @return
     */
    public IProxy poll(){
        return queue.poll();
    }

    /**
     * @param proxy
     */
    public void offer(IProxy proxy){
        this.queue.offer(proxy);
    }

    /**
     *
     * @param src
     */
    public void deal(String src){
        if(src.contains("没有找到符合条件的代理")){
            System.out.println(src);
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

                if(this.queue.size() <= MAX_SIZE){
                    this.queue.offer(proxy);
                }else{
                    //移除最早入队的数据
                    this.queue.poll();
                    offer(proxy);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        System.out.println("当前代理队列的大小为：" + this.queue.size());
    }

    /**
     */
    private class ProxyTask extends TimerTask{
        public ProxyTask(){
            request.setUrl(BASE.replaceAll("ORDER_NUM",Configuration.me().getString("proxy.order.num")));
        }
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

    public static void main(String[] args){
        ProxyManager.me();
    }
}
