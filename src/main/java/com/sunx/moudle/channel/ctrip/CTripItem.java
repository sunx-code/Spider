package com.sunx.moudle.channel.ctrip;

import com.sunx.constant.Constant;
import com.sunx.entity.ResultEntity;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.channel.Wait;
import com.sunx.moudle.dynamic.WebDriverPool;
import com.sunx.moudle.enums.ImageType;
import com.sunx.parser.Page;
import com.sunx.utils.FileUtil;
import com.sunx.common.encrypt.MD5;
import com.sunx.storage.DBFactory;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service(id = 3, service = "com.fosun.fonova.moudle.channel.ctrip.CTripItem")
public class CTripItem implements IParser {
    private static final Logger logger = LoggerFactory.getLogger(CTripItem.class);
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    //格式化日期数据
    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 开始解析数据
     *
     * @param factory
     * @param task
     */
    public int parser(DBFactory factory, TaskEntity task) {
        WebDriver pageDriver = null;
        try{
            //获取浏览器对象
            pageDriver = WebDriverPool.me().get();
            //请求任务
            pageDriver.navigate().to(task.getUrl());
            //休眠一定时间,等待页面渲染完毕
            Wait.wait(pageDriver,3,2,() -> true);
            //判断是否需要进行店家展开更多房型
            String source = pageDriver.getPageSource();
            if(source != null && source.contains("展开全部房型")){
                source = source.replaceAll("unexpanded hidden","unexpanded");
            }
            //添加编码设定,让浏览器可以识别对应的编码
            source = toHtml(source);
            //保存截图
            save(factory,task,source);
            //关闭浏览器
            return Constant.TASK_SUCESS;
        }catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
            return Constant.TASK_FAIL;
        }finally {
            WebDriverPool.me().recycle(pageDriver);
        }
    }

    /**
     * 保存数据
     * @param factory
     * @param task
     * @param source
     */
    public void save(DBFactory factory,TaskEntity task,String source) throws Exception{
        Date date = new Date();
        String vday = fs.format(date);
        String now = sdf.format(date);

        String id = vday + "," + task.getChannelName() + "," + task.getRegion() + "," + task.getCheckInDate() + "," + task.getUrl() + "," + task.getSleep();
        String md5 = MD5.md5(id);

        String txtPath = FileUtil.createPageFile(now, task.getChannelName(), task.getRegion(), task.getCheckInDate(), md5, ImageType.HTML);
        FileUtils.writeStringToFile(new File(txtPath), source, "UTF8");

        // ===================================
        ResultEntity resultEntity = new ResultEntity();
        resultEntity.setId(md5);
        resultEntity.setCheckInDate(task.getCheckInDate());
        resultEntity.setChannelName(task.getChannelName());
        resultEntity.setRegion(task.getRegion());
        resultEntity.setTid(task.getId());
        resultEntity.setUrl(task.getUrl());
        resultEntity.setVday(vday);
        resultEntity.setPath(txtPath);
        resultEntity.setSleep(task.getSleep());

        factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
    }

    /**
     *
     * @param source
     * @return
     */
    public String toHtml(String source){
        Page page = Page.me().bind(source);
        page.append("head","<meta http-equiv=Content-Type content=\"text/html;charset=utf-8\">");
        return page.html();
    }

    /**
     * 测试main
     * @param args
     */
    public static void main(String[]args){
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(123l);
        taskEntity.setChannelName("携程");
        taskEntity.setRegion("三亚");
        taskEntity.setCheckInDate("2017-06-20");
        taskEntity.setSleep(3);
        taskEntity.setUrl("http://hotels.ctrip.com/hotel/4037056.html?startDate=2017-06-20&depDate=2017-06-23");

        CTripItem item = new CTripItem();
        item.parser(null,taskEntity);
    }
}
