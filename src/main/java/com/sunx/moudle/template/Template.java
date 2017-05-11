package com.sunx.moudle.template;

import com.sunx.constant.Constant;
import com.sunx.moudle.channel.qunar.QunarSearchItem;
import com.sunx.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 初始化快照末班的代码
 */
public class Template {
    //模板集合对象
    private Map<String,String> templates = new HashMap<String,String>();
    //日志记录
    private static final Logger logger = LoggerFactory.getLogger(QunarSearchItem.class);

    /**
     * 开始构造单例对象
     */
    private static class SingleClass{
        private static Template template = new Template();
    }

    /**
     * 返回当前单例对象
     * @return
     */
    public static Template me(){
        return SingleClass.template;
    }

    private Template(){
        init();
    }

    /**
     * 初始化快照末班
     */
    public void init(){
        try{
            //读取文件
            File file = new File(Constant.TEMPLATE_FILE_LIST_PATH);
            if(!file.exists())return;
            //找到文件夹下的所有文件
            File[] files = file.listFiles();
            if(files.length <= 0)return;
            //开始遍历集合
            for(File f : files){
                //找到文件名称
                String fileName = f.getName();
                fileName = fileName.replaceAll("\\.htm","");
                //文件内容
                String template = FileUtil.readerByFile(f,Constant.DEFAULT_ENCODING);
                if(template == null){
                    logger.error("配置末班有错误 -> " + fileName);
                    continue;
                }
                //将数据插入到集合,等待使用
                templates.put(fileName,template);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 获取模板
     * @param key
     */
    public String get(String key){
        return templates.get(key);
    }
}
