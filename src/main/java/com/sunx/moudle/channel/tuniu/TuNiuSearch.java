//package com.fosun.fonova.moudle.channel.tuniu;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.sunx.downloader.Downloader;
//import com.sunx.downloader.HttpClientDownloader;
//import com.sunx.downloader.Request;
//import com.sunx.downloader.Site;
//import com.sunx.moudle.channel.IMonitor;
//import com.sunx.moudle.channel.ali.AliHotels;
//import com.sunx.moudle.proxy.ProxyManager;
//
//import org.apache.commons.lang3.exception.ExceptionUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import java.text.SimpleDateFormat;
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//
///** */
//public class TuNiuSearch implements IMonitor {
//    //日志记录
//    private static final Logger logger = LoggerFactory.getLogger(AliHotels.class);
//    //下载器
//    private Downloader downloader = new HttpClientDownloader();
//    //站点对象
//    private Site site = new Site();
//    //请求对象
//    private Request request = new Request();
//
//    //格式化日期
//    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//    //格式化日期数据
//    private SimpleDateFormat fs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//
//        /**
//         * 开始种子采集
//         */
//        public void start() {
//            try {
//                //用于存储结果
//                if(flag == 1){
//                    //处理开始数据
//                    offer();
//                }
//
//                //开始进行调度
//                long current = System.currentTimeMillis();
//                long duration = period(current);
//                start(duration,duration);
//
//                Thread.sleep(2000);
//                //开始具体数据的采集
//                logger.info("开始启动线程,进行数据采集...");
//                for(int i=0;i<THREAD_SIZE;i++){
//                    Exture exe = new Exture();
//                    exe.start();
//                }
//            } catch (Exception e) {
//                logger.error(ExceptionUtils.getStackTrace(e));
//            }
//        }
//
//        /**
//         * 获取距离第二天00:30多长时间
//         *
//         * @param current
//         * @return
//         */
//        public long period(long current) {
//            final GregorianCalendar calendar = new GregorianCalendar();
//            calendar.add(GregorianCalendar.DATE, 1);
//            calendar.set(Calendar.HOUR_OF_DAY, 0);
//            calendar.set(Calendar.SECOND, 0);
//            calendar.set(Calendar.MINUTE, 30);
//            calendar.set(Calendar.MILLISECOND, 0);
//
//            return calendar.getTime().getTime() - current;
//        }
//
//        /**
//         * 重新拉取出数据
//         * @return
//         */
//        private SoureEntity poll(){
//            return queue.poll();
//        }
//
//        private void start(long delay,long period){
//            Timer timer = new Timer();
//            TaskDurad taskDurad = new TaskDurad();
//            timer.schedule(taskDurad,delay,period);
//        }
//
//        private void offer(){
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210217731","桂林"));
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210342923","桂林"));
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210420840","桂林"));
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210217771","桂林"));
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210176933","三亚"));
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210177868","三亚"));
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210318802","三亚"));
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210373477","东澳岛"));
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210174025","亚布力"));
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210218068","亚布力"));
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210178054","亚布力"));
//            queue.offer(new SoureEntity("http://www.tuniu.com/tours/210173808","亚布力"));
//        }
//
//        private class TaskDurad extends TimerTask{
//            @Override
//            public void run() {
//                offer();
//            }
//        }
//
//        /**
//         * 抓取线程
//         * 1 获取到链接地址,解析出商品id
//         * -- 2 请求接口 http://www.tuniu.com/yii.php?r=detail/tourV3Ajax/calendar&id=210352730&backCityCode=0&departCityCode=1808&refreshFileCache=0&type=json&bookCityCode=2500
//         * 2 封装日期,格式化最近30天数据
//         * 3 获取一个日期的数据,开始请求酒店的数据
//         * 4 获取酒店价格,抽取出来每天的房型
//         * 5 抽取卡券价格
//         * 6 计算价格
//         * 7 存储数据
//         */
//        private class Exture extends Thread{
//            private Pattern pattern = Pattern.compile("\\d+");
//            //获取日期数据,根据商品id(PRO_ID)
//            private String DAY_SEARCH_URL = "http://www.tuniu.com/yii.php?r=detail/tourV3Ajax/calendar&id=PRO_ID&backCityCode=2500&departCityCode=2500&refreshFileCache=0&type=json&bookCityCode=2500";
//            //获取酒店数据
//            private String HOTEL_SEARCH_URL = "http://www.tuniu.com/yii.php?r=order/tourV3DriveOrder/getDefaultHotelNRTRoom&productId=PRO_ID&departCityCode=2500&backCityCode=0&bookCityCode=2500&adultNum=ADULT_NUM&childNum=CHILD_NUM&departDate=START_DAY";
//            //卡券数据
//            private String KAQUAN_SEARCH_URL = "http://www.tuniu.com/yii.php?r=order/DiyV3Order/GetDiyV3AltAdditionalItemResources";
//            //时间格式化
//            private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            private SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd");
//
//            @Override
//            public void run() {
//                init();
//
//                while(true){
//                    try {
//                        //根据链接抽取出相对应的productId
//                        SoureEntity source = poll();
//                        if (source == null || source.getUrl().length() <= 0) {
//                            logger.info("当前集合可能为空...线程休眠一定时间后接续...");
//                            Thread.sleep(3000);
//                            continue;
//                        }
//                        //抽取其中的productId
//                        String pro = find(pattern, source.getUrl());
//                        //获取到productId,后需要去请求日期数据
//                        List<String> list = findDays(pro);
//
//                        //循环遍历日期数据,开始处理每天的数据请求
//                        for (int i = 0; i < list.size(); i++) {
//                            try {
//                                //线程休眠
//                                Thread.sleep(1500);
//
//                                String day = list.get(i);
//                                //处理这一天的数据
//                                dealData(day,source,pro,2,1);
//
//                                Thread.sleep(1500);
//                                //线程休眠一定时间后继续
//                                dealData(day,source,pro,2,0);
//
//                                Thread.sleep(1500);
//                                //线程休眠一定时间后继续
//                                dealData(day,source,pro,1,1);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }catch (Exception e){
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            /**
//             * 处理结果数据
//             * @param day
//             * @param pro
//             */
//            private void dealData(String day,SoureEntity source,String pro,int adultNum,int childNum){
//                try{
//                    //请求酒店数据
//                    String link = find(HOTEL_SEARCH_URL,day,pro,adultNum,childNum);
//                    //下载数据
//                    Page page = download(request.setUrl(link),site);
//                    //处理当前酒店数据
//                    if (page == null || page.getHtml() == null || page.getHtml().length() <= 0) {
//                        logger.info("当前集合可能为空...线程休眠一定时间后接续...");
//                        return;
//                    }
//                    //hotel数据
//                    JSONObject hotel = findHotel(page.getHtml());
//                    //格式化数据
//                    JSONArray bean = findRoomes(hotel);
//                    if(bean == null)return;
//                    //入住晚数
//                    int sleep = hotel.getIntValue("journeyDays");
//                    //获取卡券的数据
//                    JSONObject tec = findTec(hotel,day,pro,adultNum,childNum);
//                    //获取卡券的价格
//                    int tecPrice = getPrice(tec,day,adultNum,childNum);
//
//                    List<Item> items = new ArrayList<Item>();
//                    //循环获取数据
//                    for(int i=0;i<bean.size();i++){
//                        try{
//                            JSONObject node = bean.getJSONObject(i);
//                            //房间标题
//                            String resName = node.getString("resName");
//                            //maxAdultNum
//                            int maxAdultNum = node.getIntValue("maxAdultNum");
//                            //判断房间数
//                            int roomNum = adultNum / maxAdultNum;
//                            //获取价格
//                            int totalPrice = getPrice(node);
//                            //获取房型
//                            String houseType = type(resName);
//
//                            //打印数据  那一天  那个店铺  店铺链接   成人数   儿童数   酒店价格   卡券价格
//                            Item item = new Item();
//                            item.setId(Long.parseLong(pro));
//                            item.setAdultNum(adultNum);
//                            item.setChildNum(childNum);
//                            item.setUrl(source.getUrl());
//                            item.setSleep(sleep);
//                            item.setCheckInDay(day);
//                            item.setHouseDetail(resName);
//                            item.setHouseType(houseType);
//                            item.setRoomNum(roomNum);
//                            item.setTecPrice(tecPrice);
//                            item.setSinglePrice(totalPrice);
//                            item.setRegion(source.getRegion());
//                            item.setChannel(channel);
//                            item.setVday(sdf.format(new Date()));
//
//                            items.add(item);
//                            System.out.println(day + "\t" + pro + "\t" + resName + "\t" + adultNum + "\t" + childNum + "\t" + totalPrice * roomNum + "\t" + tecPrice);
//                        }catch (Exception e){
//                            e.printStackTrace();
//                        }
//                    }
//                    //提交数据的到数据库中
//                    factory.insert("localhost","item_data",items,Item.class);
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//
//            /**
//             * 冲标题中抽取出房型
//             * @param title
//             * @return
//             */
//            private String type(String title){
//                try{
//                    if(title == null || title.length() <= 0 || !title.contains("--"))return null;
//                    String[] tmp = title.split("--");
//                    if(tmp == null || tmp.length <= 0)return null;
//                    return tmp[0];
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//                return null;
//            }
//
//            /**
//             * 找到哪天的价格
//             * @param tec
//             * @param day
//             * @param adultNum
//             * @param childNum
//             * @return
//             */
//            private int getPrice(JSONObject tec,String day,int adultNum,int childNum){
//                if(tec == null)return 0;
//                if(tec.toJSONString().contains("暂无附加项目数据"))return 0;
//                try{
//                    JSONObject data = tec.getJSONObject("data");
//                    JSONObject type = data.getJSONObject("5");
//                    JSONArray array = type.getJSONArray("resList");
//                    if(array == null || array.size() < 0)return 0;
//                    JSONObject bean = array.getJSONObject(0);
//                    JSONArray tmp = bean.getJSONArray("resourceDatePrices");
//                    //获取当前日期的价格
//                    int price = 0;
//                    for(int i=0;i<tmp.size();i++){
//                        JSONObject node = tmp.getJSONObject(i);
//
//                        //获取当前日期
//                        String current = node.getString("departDate");
//
//                        if(day.contains(current)){
//                            //获取价格
//                            int childPrice = node.getIntValue("childPrice");
//                            int adultPrice = node.getIntValue("price");
//
//                            price = adultPrice * adultNum + childPrice * childNum;
//
//                            break;
//                        }
//                    }
//                    return price;
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//                return 0;
//            }
//
//            /**
//             * 获取价格数据
//             * @param node
//             * @return
//             */
//            private int getPrice(JSONObject node){
//                JSONObject bean = node.containsKey("price")?node.getJSONObject("price"):null;
//                if(bean == null)return 0;
//                int total = 0;
//                for(String key : bean.keySet()){
//                    total = total + bean.getIntValue(key);
//                }
//                return total;
//            }
//
//            /**
//             * 解析数据
//             * @param json
//             * @return
//             */
//            private JSONObject findHotel(String json){
//                JSONObject bean = JSON.parseObject(json);
//                JSONObject data = bean.containsKey("data")?bean.getJSONObject("data"):null;
//                JSONObject hd = data.getJSONObject("hotel");
//                JSONObject hotels = null;
//                for(String key : hd.keySet()){
////                JSONObject h = hd.getJSONObject(key);
////                //抽取数据
////                hotels = h.containsKey("hotels")?h.getJSONObject("hotels"):null;
////                break;
//                    hotels = hd.getJSONObject(key);
//                    break;
//                }
//                return hotels;
//            }
//
//            private JSONArray findRoomes(JSONObject hotels){
//                JSONObject data = hotels.getJSONObject("hotels");
//                JSONArray obj = null;
//                for(String key : data.keySet()){
//                    JSONObject h = data.getJSONObject(key);
//                    //抽取数据
//                    obj = h.containsKey("rooms")?h.getJSONArray("rooms"):null;
//                    break;
//                }
//                return obj;
//            }
//
//            /**
//             * 获取数据
//             * @param day
//             * @param pro
//             * @return
//             */
//            private JSONObject findTec(JSONObject hotels,String day,String pro,int adultNum,int childNum){
//                try{
//                    //获取对应的journeyId
//                    String jid = hotels.getString("journeyId").replaceAll("j_","");
//
//                    //设置请求方式
//                    request.setMethod("POST")
//                            .addPostData("postData[productId]",pro)
//                            .addPostData("postData[departCityCode]","2500")
//                            .addPostData("postData[backCityCode]","2500")
//                            .addPostData("postData[adultNum]","" + adultNum)
//                            .addPostData("postData[childNum]","" + childNum)
//                            .addPostData("postData[departDate]",day)
//                            .addPostData("postData[journeyId]",jid);
//                    //线程休眠1s后进行下载
//                    Thread.sleep(1000);
//                    //下载数据,并判断数据
//                    Page page = download(request.setUrl(KAQUAN_SEARCH_URL),site);
//                    if(page == null || page.getHtml() == null || page.getHtml().length() <= 0){
//                        logger.info("下载数据异常,需要处理异常数据..");
//                        return null;
//                    }
//                    //开始解析数据
//                    return JSON.parseObject(page.getHtml());
//                }   catch (Exception e){
//                    e.printStackTrace();
//                }finally{
//                    request.setMethod("GET");
//                }
//                return null;
//            }
//
//            /**
//             * 写入月份日期
//             * @param prodId
//             * @return
//             */
//            private List<String> findDays(String prodId){
//                List<String> days = new ArrayList<String>();
//                try{
//                    //下载数据,并判断数据
//                    String link = DAY_SEARCH_URL.replaceAll("PRO_ID",prodId);
//                    Page page = download(request.setUrl(link),site);
//                    if(page == null || page.getHtml() == null || page.getHtml().length() <= 0){
//                        logger.info("下载日期数据出现错误....");
//                        return days;
//                    }
//                    //开始解析数据
//                    JSONObject bean = JSON.parseObject(page.getHtml());
//                    JSONArray array = bean.containsKey("planDateArr")?bean.getJSONArray("planDateArr"):null;
//                    //将数据写入到集合中
//                    wrtie(days,array);
//                }   catch (Exception e){
//                    e.printStackTrace();
//                }
//                return days;
//            }
//
//            private Page download(Request request,Site site){
//                try{
//                    IProxy proxy = getProxy();
//                    if(proxy == null){
//                        proxy = new IProxy();
//                    }
//                    return downloader.download(request,site,proxy.getHost(),proxy.getPort());
////                return downloader.download(request,site,null,-1);
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//                return null;
//            }
//
//            /**
//             * 获取代理
//             *
//             * @return
//             */
//            public IProxy getProxy() {
//                return ProxyManager.me().poll();
//            }
//
//            private void wrtie(List<String> days,JSONArray array){
//                if(array == null || array.size() <= 0){
//                    array = init();
//                }
//                //当前日期
//                long max = System.currentTimeMillis() + 86400l * 1000l * 30;
//                //遍历集合,拉去途牛的数据
//                for(int i=0;i<array.size();i++){
//                    String day = array.getString(i);
//
//                    //判断日期是否超过30天
//                    try{
//                        Date end = form.parse(day);
//                        long endUnix = end.getTime();
//
//                        if(endUnix >= max)continue;
//                    }catch (Exception e){}
//
//                    logger.info("添加的日期数据为:" + day);
//                    //将数据添加到缓存中
//                    days.add(day);
//                }
//            }
//
//            /**
//             *
//             * @return
//             */
//            private JSONArray init(){
//                JSONArray array = new JSONArray();
//                SimpleDateFormat dfs = new SimpleDateFormat("yyyy-MM-dd");
//                for (int i = 0; i <= 30; i++) {
//                    //获取其实天
//                    long day = System.currentTimeMillis() + 86400l * 1000 * i;
//                    String start = dfs.format(new Date(day));
//
//                    array.add(start);
//                }
//                return array;
//            }
//
//            /**
//             *
//             * @param url
//             * @param day
//             * @param proid
//             * @param adultNum
//             * @param childNum
//             * @return
//             */
//            private String find(String url,String day,String proid,int adultNum,int childNum){
//                return url.replaceAll("PRO_ID",proid)
//                        .replaceAll("ADULT_NUM","" + adultNum)
//                        .replaceAll("START_DAY",day)
//                        .replaceAll("CHILD_NUM","" + childNum);
//            }
//
//            /**
//             * 获取数据
//             * @param pattern
//             * @param src
//             * @return
//             */
//            private String find(Pattern pattern,String src){
//                Matcher matcher = pattern.matcher(src);
//                if(matcher.find())return matcher.group();
//                return null;
//            }
//        }
//    }
