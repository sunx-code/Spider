package com.sunx.utils;

import com.alibaba.fastjson.JSONObject;
import com.sunx.constant.Configuration;
import com.sunx.constant.Constant;
import com.sunx.downloader.Downloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;
import com.sunx.moudle.proxy.IProxy;
import com.sunx.moudle.proxy.ProxyManager;
import com.sunx.parser.Page;
import com.sunx.storage.DBFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sunxing on 2016/12/19.
 */
public class Helper {

    /**
     * 返回通用的方法
     * @param adultNum
     * @param childNum
     * @return
     */
    public static String people(int adultNum,int childNum){
        if(adultNum == 2 && childNum == 1){
            return "2成人1儿童";
        }else if(adultNum == 2 && childNum == 0){
            return "2成人";
        }else if(adultNum == 1 && childNum == 1){
            return "1成人1儿童";
        }
        return null;
    }

    /**
     * 清洗字符串
     * @param str
     * @param child
     * @param replace
     * @return
     */
    public static String clean(String str,String child,String replace){
        if(str == null)return null;
        return str.replaceAll(child,replace);
    }

    /**
     * 转成int
     * @param str
     * @return
     */
    public static int toInt(String str){
        try{
            return Integer.parseInt(str);
        } catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    /**
     *
     * @param bean
     * @return
     */
    public static double toDouble(String bean){
        try{
            return Double.parseDouble(bean);
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    /**
     *
     * @param str
     * @param pattern
     * @param index
     * @return
     */
    public static String[] find(String str, Pattern pattern, int... index){
        String[] result = new String[index.length];
        Matcher matcher = pattern.matcher(str);
        if(matcher.find()){
            for(int i=0;i<index.length && i < matcher.groupCount();i++){
                result[i] = matcher.group(index[i]);
            }
        }
        return result;
    }

    /**
     *
     * @param str
     * @param pattern
     * @param index
     * @return
     */
    public static String find(String str, Pattern pattern, int index){
        Matcher matcher = pattern.matcher(str);
        if(matcher.find()){
            if(index >= 0)return matcher.group(index);
            return matcher.group();
        }
        return null;
    }
    /**
     *
     * @param str
     * @param pattern
     * @return
     */
    public static String find(String str, Pattern pattern){
        return find(str,pattern,-1);
    }

    /**
     * 下载数据
     * @param downloader
     * @param request
     * @param site
     * @return
     */
    public static String downlaoder(Downloader downloader, Request request,Site site){
        return downlaoder(downloader,request,site,true);
    }

    /**
     * 下载数据
     * @param downloader
     * @param request
     * @param site
     * @return
     */
    public static String downlaoder(long cid,Downloader downloader, Request request,Site site){
        return downlaoder(cid,downloader,request,site,true);
    }

    /**
     * 具体的是否使用代理进行网页源码的下载
     * @param downloader
     * @param request
     * @param site
     * @param flag
     * @return
     */
    public static String downlaoder(Downloader downloader, Request request,Site site,boolean flag){
        return downlaoder(-1l,downloader,request,site,flag);
    }
    /**
     * 具体的是否使用代理进行网页源码的下载
     * @param downloader
     * @param request
     * @param site
     * @param flag
     * @return
     */
    public static String downlaoder(long cid,Downloader downloader, Request request,Site site,boolean flag){
        String src = null;
        //将每个渠道,下载的链接,使用的ip存储到数据库中
        DBFactory factory = DBFactory.me();
        try{
            int j = 0;
            while(j <= 3){
                j++;
                //获取代理
                IProxy proxy = null;
                if(flag){
                    int i = 0;
                    while(proxy == null){
                        proxy = ProxyManager.me().poll(cid);
                        if(proxy == null){
                            i++;
                            //获取代理超过一定的次数,直接跳出
                            if(i >= 10)break;
                            try{
                                System.out.println(Thread.currentThread().getName() + " -> 获取代理失败,线程需要休眠1.5s后继续....");
                                Thread.sleep(1500);
                            }catch ( Exception e){
                                e.printStackTrace();
                            }
                            continue;
                        }
                        break;
                    }
                }
                if(proxy == null){
                    proxy = new IProxy();
                }else if(cid != -1){
                    try{
                        long current = System.currentTimeMillis();
                        long last = proxy.get(cid);
                        long sleep = Configuration.me().getLong(Constant.PROXY_LOAD_DURATION_KEY) - (current - last);
                        if(sleep > 0){
                            System.err.println("渠道" + cid + "使用代理(" + proxy.getHost() + "," + proxy.getPort() + "),使用太频繁,需要休眠" + sleep + "ms后继续...");
                            Thread.sleep(sleep);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                Map<String,Object> map = new HashMap<>();
                map.put("cid",cid);
                map.put("url",request.getUrl());
                map.put("host",proxy.getHost());
                map.put("port",proxy.getPort());

                src = downloader.downloader(request,site,proxy.getHost(),proxy.getPort());
                proxy.put(cid,System.currentTimeMillis());
                if(src == null && flag){
                    proxy.setFlag(false);

                    map.put("status",-1);
                    factory.insert("localhost","t_proxy",map);
                    continue;
                }
                map.put("status",1);
                factory.insert("localhost","t_proxy",map);
                break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return src;
    }

    /**
     * 格式化
     *
     * @param jsonStr
     * @return
     */
    public static String format(String jsonStr) {
        if (null == jsonStr || "".equals(jsonStr))
            return "";
        StringBuilder sb = new StringBuilder();
        char last = '\0';
        char current = '\0';
        int indent = 0;
        boolean isInQuotationMarks = false;
        for (int i = 0; i < jsonStr.length(); i++) {
            last = current;
            current = jsonStr.charAt(i);
            switch (current) {
                case '"':
                    if (last != '\\'){
                        isInQuotationMarks = !isInQuotationMarks;
                    }
                    sb.append(current);
                    break;
                case '{':
                case '[':
                    sb.append(current);
                    if (!isInQuotationMarks) {
                        sb.append('\n');
                        indent++;
                        addIndentBlank(sb, indent);
                    }
                    break;
                case '}':
                case ']':
                    if (!isInQuotationMarks) {
                        sb.append('\n');
                        indent--;
                        addIndentBlank(sb, indent);
                    }
                    sb.append(current);
                    break;
                case ',':
                    sb.append(current);
                    if (last != '\\' && !isInQuotationMarks) {
                        sb.append('\n');
                        addIndentBlank(sb, indent);
                    }
                    break;
                default:
                    sb.append(current);
            }
        }

        return sb.toString();
    }

    /**
     * 添加space
     *
     * @param sb
     * @param indent
     */
    public static void addIndentBlank(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append('\t');
        }
    }

    /**
     *
     * @param source
     * @return
     */
    public static String toHtml(String source){
        Page page = Page.me().bind(source);
        page.remove("meta[http-equiv=Content-Type]");
        page.append("head","<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">");
        return page.html();
    }

    /**
     *
     * @param map
     * @return
     */
    public static String toJSON(Map<String,String> map){
        JSONObject bean = new JSONObject();
        for(Map.Entry<String,String> entry : map.entrySet()){
            bean.put(entry.getKey(),entry.getValue());
        }
        return bean.toJSONString();
    }
}