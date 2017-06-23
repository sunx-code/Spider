package com.sunx.utils;

import com.sunx.downloader.Downloader;
import com.sunx.downloader.Request;
import com.sunx.downloader.Site;
import com.sunx.moudle.proxy.IProxy;
import com.sunx.moudle.proxy.ProxyManager;

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
     * 具体的是否使用代理进行网页源码的下载
     * @param downloader
     * @param request
     * @param site
     * @param flag
     * @return
     */
    public static String downlaoder(Downloader downloader, Request request,Site site,boolean flag){
        String src = null;
        try{
            int j = 0;
            while(j <= 3){
                //获取代理
                IProxy proxy = null;
                if(flag){
                    int i = 0;
                    while(proxy == null || i < 5){
                        proxy = ProxyManager.me().poll();
                        if(proxy == null){
                            i++;
                            try{
                                Thread.sleep(1500);
                            }catch ( Exception e){
                                e.printStackTrace();
                            }
                            continue;
                        }
                        //获取到代理就直接丢回去,在重新获取
                        ProxyManager.me().offer(proxy);
                        break;
                    }
                }
                if(proxy == null){
                    proxy = new IProxy();
                }
                src = downloader.downloader(request,site,proxy.getHost(),proxy.getPort());
                if(src == null && flag){
                    ProxyManager.me().remove(proxy);
                    continue;
                }
                j++;
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
}