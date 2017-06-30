package com.sunx.moudle.css;

import com.sunx.entity.CNode;
import com.sunx.utils.FileUtil;
import com.sunx.utils.Helper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 处理css的逻辑
 * 1 根据不同的方式来处理不同的css
 *
 * 位移:
    -webkit-transform:translate(x,y)

    左右偏移：
    left,margin-left,padding-left

    左右缩进：
    text-indent:

    上下层叠:
    z-index

    放大缩小:
    line-height


 去哪儿的价格问题：
 1：
 .qunar_mix span.price i.qmacf0943b{left: -100px; padding-left:300px;background:none;}
 .qunar_mix span.price i.qmc1c11156{left: 11px; margin-left:22px; padding-left:11px; background:none;}
 .qunar_mix span.price i.qmca340bd1{left: 11px; margin-left:11px; padding-left:11px; background:none;}
 .qunar_mix span.price i.qmbd09efa2{left: 0px; margin-left:11px; padding-left:11px; background:none;}
 .qunar_mix span.price i.qm18d6dd7e{left: -33px; margin-left:11px; padding-left:33px; background:none;}
 .qunar_mix span.price i.qm11251921{left: -22px; margin-left:11px; padding-left:11px; background:none;}
 .qunar_mix span.price i.qmcf5711c8{left: -100px; margin-left:22px; padding-left:22px; background:none;}
 .qunar_mix span.price i.qm5c6d111c{left: -100px; margin-left:300px;background:none;}

 =>
 i	v	f	index
 1 	6	0	- 	qmacf0943b	left: -100px; padding-left:300px;background:none;}
 2 	-	-	-	qmc1c11156	left: 11px; margin-left:22px; padding-left:11px; background:none;}
 3	0 	1	4	qmca340bd1	left: 11px; margin-left:11px; padding-left:11px; background:none;}
 4	8 	1 	3 	qmbd09efa2	left: 0px; margin-left:11px; padding-left:11px; background:none;}
 5	6	1 	2 	qm18d6dd7e	left: -33px; margin-left:11px; padding-left:33px; background:none;}
 6	1 	1 	1 	qm11251921	left: -22px; margin-left:11px; padding-left:11px; background:none;}
 7	- 	-	-	qmcf5711c8	left: -100px; margin-left:22px; padding-left:22px; background:none;}
 8	4 	0 	- 	qm5c6d111c	left: -100px; margin-left:300px;background:none;}


 ==>


 周四：


 周五:
 left: 0px;
 text-indent: 0.5em;
 display: none;
 background: none;

 index = text-indent * 2 + 1

 周六：


 周日：
 text-indent: 1em;
 z-index: 20;
 background: none;

 过滤：display: none;

 index = text-indent * 2 + 1

 周一：
 left: 11px;
 line-height: 4;
 background: none;

 过滤：Line-height:4,display: none;

 index = left / 11 + 1

 周二：
 left: 11px;
 line-height: 1;
 background: none;

 过滤：Line-height:4,display: none;

 index = left / 11 + 1

 *
 */
public class CssMode {
    //查找clss对应德 正则表达式
    private Pattern clazzPattern = Pattern.compile("(?:i\\.)(.*?)(?:\\{)");
    //用于验证集合是否已经重新初始化了
    private int DEFAULT_PRICE_ONE_KEY_WIDTH = 11;
    //抽取数字内容,包含负号,点
    private static Pattern pattern = Pattern.compile("-?\\d+\\.?\\d*");
    //对应的tanslate的抽取内容
    private static Pattern translate = Pattern.compile("(-?\\d+)(?:(px)?)");

    private static class SingleClass{
        private static CssMode mode = new CssMode();
    }

    public static CssMode me(){
        return SingleClass.mode;
    }

    /**
     *
     * @param css
     */
    public Map<Integer,CNode> mode(String css){
        //切割css字符串
        //检验数据
        if(css == null || css.length() <= 0)return null;
        //开始处理数据
        String[] cssList = css.split("\n");
        if(cssList == null || cssList.length <= 0)return null;
        Map<String,CNode> nodes = new HashMap<String, CNode>();
        for(String line : cssList) {
            //过滤掉隐藏起来的数据
            if(line.contains("display") || line.contains("visibility"))continue;
            //暂时只处理针对特定class的css内容
            if (!line.contains(".qunar_mix span.price")) continue;
            //抽取clzz
            String clzz = Helper.find(line,clazzPattern,1);
            if(nodes.containsKey(clzz))continue;
            //下面开始根据不同的类型来进行初始化对应的数据内容
            String str = line.substring(line.indexOf("{") + 1,line.indexOf("}"));
            //打印出具体的数据内容
            toMap(nodes,clzz,str);
        }
        //开始进行下标计算
        return toIndex(nodes);
    }

    /**
     * 解析css的内容
     * @param clazz
     * @param css
     */
    public void toMap(Map<String,CNode> nodes,String clazz,String css){
        //开始处理css,根据分号来进行数据的切分
        if(css == null || css.length() <= 0)return;
        String[] ts = css.split(";");
        if(ts == null || ts.length <= 0)return;
        //构造对象用于存储当前的结果
        CNode node = new CNode();
        node.setClazz(clazz);

        //开始遍历数组,同时对每一个数据进行切割,将切割的结果进行再切割
        for(String s : ts){
            String[] tmps = s.split("\\:");
            if(tmps == null || tmps.length != 2)continue;

            //开始给相应的内容添值
            witch(node,tmps[0].trim(),tmps[1].trim());
        }
        //将处理好的node添加到集合中
        nodes.put(clazz,node);
    }

    /**
     * 开始进行切分
     * @param node
     * @param key
     * @param value
     */
    public void witch(CNode node,String key,String value){
        //找到具体对应的css内容
        ModeType type = witch(key);
        //根据不同的类型,抽取value中的值
        double x = type.toValue(value);
        //根据不同的乐行,来进行数据的基本的处理
        value(node,key,value,x);
    }

    /**
     * 更新对应的下标
     * @param nodes
     */
    public Map<Integer,CNode> toIndex(Map<String,CNode> nodes){
        Map<Integer,CNode> result = new HashMap<>();
        for(Map.Entry<String,CNode> entry : nodes.entrySet()){
            CNode node = entry.getValue();

            int index = toIndex(node);
            if(index <= 0){
                System.err.println("css解析出现错误. -> " + node.toString());
                continue;
            }
            //处理正确的数据内容
            if(result.containsKey(index)){
                //出现重复位置内容,需要比对一下内容:
                //1. z-index        是不是浮层
                //2. line-hight     是不是放大缩小
                CNode first = result.get(index);
                //表示first在上层,当前节点直接丢弃掉
                if(first.getZindex() > node.getZindex() && first.getZindex() > 0)continue;
                //表示现在待处理的节点是放大的数据,因此不需要存储,直接过滤掉
                if(first.getLine() < node.getLine() && node.getLine() > 0)continue;
                //比对的结果是当前的数据已经不适合了,需要更新
            }
            result.put(index,node);
        }
        return result;
    }

    /**
     * 找下标问题
     * @param node
     * @return
     */
    public int toIndex(CNode node){
        int l = (int)(node.getLeft() / DEFAULT_PRICE_ONE_KEY_WIDTH);
        int indent = (int)(node.getIndent() * 2);
        if(indent > 0)return indent + 1;
        if(l > 0)return l + 1;
        if(l == 0 && indent == 0)return 1;
        return -1;
    }

    /**
     * 根据给定的key进行分类
     *
     * @param key
     * @return
     */
    public ModeType witch(String key){
        switch (key){
            case "-webkit-transform":return ModeType.TRANSFORM;
            case "left":
            case "margin-left":
            case "padding-left":
            case "text-indent":
            case "z-index":
            case "line-height":return ModeType.NUMBER;//该模式为对应的值为单个数字内容可能包含非数字字符
            default: return ModeType.UNKNOW;
        }
    }

    /**
     * 根据给定的key,找到对应的字段
     * @param key
     * @return
     */
    public void value(CNode node,String key,String value,double x){
        ValueType type = ValueType.UNKNOW;
        switch (key){
            case "-webkit-transform":
            case "left":
            case "margin-left":
            case "padding-left":type = ValueType.LEFT;break;
            case "text-indent":type = ValueType.INDENT;break;
            case "z-index":type = ValueType.ZINDEX;break;
            case "line-height":type = ValueType.LINEHIGHT;break;
        }
        //处理数据
        type.value(node,key,value,x);
    }

    /**
     * css标签状态值
     */
    public enum ModeType{
        //位移状态
        TRANSFORM{
            @Override
            public double toValue(String value) {
                //translate(1px,2px),抽取出第一个数据内容来进行判定
                String[] result = Helper.find(value,translate,1,2);
                if(result == null || result.length <= 0)return -1;
                //找到横向偏移的位置
                double x = Helper.toDouble(result[0]);
//                double y = Helper.toDouble(result[1]);

                return x;
            }
        },
        NUMBER{
            @Override
            public double toValue(String value) {
                String numStr = Helper.find(value,pattern);
                if(numStr == null)return -1;
                return Helper.toDouble(numStr);
            }
        },
        UNKNOW{
            @Override
            public double toValue(String value) {
                return -1;
            }
        };

        public abstract double toValue(String value);
    }

    /**
     * 具体值对应的枚举类
     */
    public enum ValueType{
        LEFT{
            @Override
            public void value(CNode node, String key, String value, double x) {
                node.setLeft(node.getLeft() + x);
            }
        },
        INDENT{
            @Override
            public void value(CNode node, String key, String value, double x) {
                node.setIndent(node.getIndent() + x);
            }
        },
        ZINDEX{
            @Override
            public void value(CNode node, String key, String value, double x) {
                node.setZindex(node.getZindex() + x);
            }
        },
        LINEHIGHT{
            @Override
            public void value(CNode node, String key, String value, double x) {
                node.setLine(node.getLine() + x);
            }
        },
        UNKNOW{
            @Override
            public void value(CNode node, String key, String value, double x) {
                System.out.println("丢弃掉的 -> " + key + ":" + value);
            }
        }
        ;
        public abstract void value(CNode node,String key,String value,double x);
    }


    public static void main(String[] args){
//        String str = ".qunar_mix span.price i.qm1490a6ac {left: 0px;text-indent: 0.5em;background: none;}";

        String str = FileUtil.readerByFile(new File("conf/css/qunar-css.css"),"UTF-8");
        System.out.println(str);

        Map<Integer,CNode> index = CssMode.me().mode(str);
        for(Map.Entry<Integer,CNode> i : index.entrySet()){
            System.out.println(i.getKey() + "\t" + i.getValue().getClazz());
        }
    }
}