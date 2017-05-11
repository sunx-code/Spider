package com.sunx.moudle.js;

import com.sunx.utils.FileUtil;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * js执行引擎
 */
public class ScriptManager {
    //构造函数初始化js执行引擎
    private ScriptEnginePool pool = new ScriptEnginePool();
    //构造解析器
    private static class SingleClass{
        private static ScriptManager manager = new ScriptManager();
    }

    /**
     * 构造单利对象
     * @return
     */
    public static ScriptManager me(){
        return SingleClass.manager;
    }

    /**
     * 分装对象
     * 开始执行js脚本,进行解析,并将结果写入到json对象中
     * @param script   解析脚本
     * @param param    参数集合
     */
    public void runScript(String script,Map<String,Object> param) {
        ScriptEngine engine = null;
        CompiledScript compiled = null;
        try {
            engine = pool.get();
            compiled = ((Compilable) engine).compile(script);
            SimpleBindings bindings = new SimpleBindings(param);
            compiled.eval(bindings);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(engine != null){
                pool.recycle(engine);
            }
        }
    }

    public static void main(String[] args){
        String script = FileUtil.readerByFile(new File("conf/encrypt/qunar.js"),"utf-8");

        String css ="c5639d4f005f666877f6e84bcca5f64ffac3c8de6c3aa7a21c804782b46ea42dd01b6634091827dece8106b3e165dea920aa0cfd0c26c41f1f2d02f1a7f73f36a281d7c418133607be21096d58c7ee732224a48ce4bc1c28cc2b4a1aa65585faee273e647cf9340f29b5149f45e3a267da081a3deaa042e609ef4e8b1ee8d14122bab2987a2f76d94315cc0496eef2193c4fd2b0fe7849a275c48bd1b9ab4683f079668c6ed60d859f47ca7c511e8d9f00f356f4a72afed7cbb433f7a7e61558320b41894abe53ed100f6249e5d8f95286990bb21fbc39a10fc1f4b14f922b9395bc7431003cd5408a798d61483155826b5ea317e50a9adbd229106a02366c40f4dad6ce7984537fae236ae718d1bcd993da3230647b59bddf6dfd3da728d73e141f6d774df6374ec89d9183e4742485a4c3222c2fd6103596abf786f66b34dd7a7941e2a5a4ad0aafd317eadc2847584e2b0094ce60e5b108b68414a635c3b6524c0f5e1efe906cdcc82e93d4f24157f652fee2ebdd408a112810671df6a793143fa69c7562bb15fe2fec370a75dc57e5c143d0f2f39c3e3a9fb2ffd75f9cbc813fbb6bb73f3f5b74e234cf0a4e4dbcbb7608629de60658af2bb8cb61d1a2a36a4f47ec7f8fe380f6348b4fac927f70c80d7c3dfcfe4f5f72b1014c1830863bdc2894409aba88d65cb96abaecfc3b83c4e4ed456dcf7301dc958d9babf794075fce048d560a80441d6018833e9687cf195e9d8332ddd45a21bfeb862208866c03fb59935b3c8a49678da10aaebdb71ab342c6b794eba81d4a19f1fd7de2e8efc9ee535d9e7c50475a1f74d4c1c0c71e9ed4e7e872c5b0b1b4e1b777d5ef2e15f67ddc70ca6e69f7a4f1f1b306e8292eab58501a6de8ec03527ff06fdbba6d235bd0a58821b430b694582ad2454612896663f44353ce3447ccb8398a6225e9e1530fef04fae1bfcf8606fcaa1f505e6a963d429c8c148d1de988cdba78e61ccbce53eb4d93ff6bb2560b6fafbf429f45efe10082b072079823048c44e80c855bea5e60b96281e141b80f8eef714ff937e9a0d0b3f99b759cee22e70f261b48bac8b15c14f1c966a73f336f48ad713900eee13dd2a7ec52463c2161be5a5ab3c17a6be0059544d3738f8885c794f4318af7c9091e269fc41e2b67cd90393b7941ea57d7a7e31cc6166e140fb68b6c47f4464f8dcffac43a47313abc910bc3df107be9c4549deab9e8d7ce1da336a8a5a14c44fe35cf19439878b6884415bde3a66bf061e371344be237173144e3413b3f83950cb522415e1433e43aa7a3dd62330ad8771890e05dc2fa90a0badb1095a3ba48d29f9c52750b67b7ee39202d8ba6c2a6dd90f4988e77bfd1467c3269f1d2c5bb5018281d068b0961fe75689c3eeb69c22b29247717a99e6ed5d65f4a9e0fc0f77ee2e710e8c44ab5a76f055ea6387c2bd95415530c81d469e09844ee624d0b045b901121d2b3d5060201f20c9174f86eb87426776ac403275d6059ac624fbb0a250bf61b50152f9e6994ce6877f175591908a0c1f919c6d591461560d502762f5439891fde913e5dfc109714a2dd3cfb78e23b335c5f8b94bf697adbfaba7cd55459fa6bc727232deaa9f73264179384142215b60fed22f6c61397d18d3278ab1ee372b821bc7fe3b0fb02ec1cf1ba997d57f4eddfa94a5878062ebde7ebb98f7a0920b8368e7cc88b3845436f09555dc23fd842cefc22cb9b3b60c2c8e7b66ee3eff7623c227853c21ca4fa79db5d462cf1a2549da5c80176d8649d200f1571efe08fcc517bfd17a045b4d3fbfa96d8ebef16de8ab3a9835459f7b2f14989bc059fc6b1d8142beb152fe781dfc9f068a266b3cf37ecc450ea3abe3ed17aefce8a91662f956fad5e34e799ae348452eb45a95e037200a41831c612f4954d4508a3aa26a78f199600eac06c5a40d20f555969550ff79ae497b7ca234369670337a6b74db3ea936435f2dbc9bdd659711f375ab087de9eda36d94d2e471f293e7f6adda02039bea3a53efa7a2dea7433a15c41dfcc6eb508ec37082f66bcc32097667e1cdc12ec89ec322f186d58e9290f160ce15b62ecafce7fa30402b1610431a004fab7d4f111b77274675adc9f1cd9c9c4751cdaf3cae0651f84c13be94bee2bd823b83f27da5646b2cb4e6c9a261e5e8af9368c84814dc82ad6c7a2296e68f79fb31d3d745b4a55e5f6d6e1fb84634e3fff71e68f3a91e17454145486b57210a5847172060cdb2ddf29a668ecfba0536c111d81521e801fa4bb5a71d9ca24de483ca1a9c5eabeea99f9c7e2a891b6aa96707ae7274c57ec7f68750f842ed7ec7a17edd7ab40f9327aa36e0e8a879001dc0ec8e2d041cc01d2cc81de1842024701c73531cff846adca9e6ea437cbeb49b26b87049d17b5ba7fcb34190d03f59a35fe9015d20a4dcb8d0886039d69c5de89f7bfb03d7b4b22865fdee252b40ca785dfe93bf09022b899dbc6ee5960b10912d44c851edd5cda9bfa6e7616db2b8d2f1990fc8068f51c3300b3e178e972b2b098a5a0c1783abfe471c39caf7656429a45c4a4d1b9b42e828e1326150122c0503c773be8577207fe1a2eadee40c46b82675f60fb734d5362d39f6a35f712e38b9ffeb2e0db1cdb2e832bd865cda5ed30a6058623e8d7805f8f2d143b0b88b000305ad983cd041d63703cd3353a540ee25826f8a27d35006c833f6f7811b6c6e43fbf9ae41c4d1ec9abec38c107e7e8a632379762774c8b9c93773a391656e05662d7f5d98284485d22daf981caba9e62f5521f4f7dc5b2aa0ee755f096746e2240494d507c4a2dac0dff7568e28960a0a3f883c5c0a73cb3ce5de93246b9e9d1e218bd00f185e0a0b917f7759d60ffa826ef15752a4937b95a371a26f3e3a7bc8fef4d61a280d018cfb3681673cf0da6113a536d5ffb3ba34b15e83cebf456d7f96dc078b21f8145ad6146e963d20cc341d889a1188b14222b6b66e8b52abb1aac5b173585b74357b4da0d5fcf73bd9932075c5721b09973bc237754b6d5d0de353c4efb711ff1b5f4b6d299f727ac03f54307ffc8ff0b23fd87fd40eda66c5c0bcaf97e";

        StringBuffer str = new StringBuffer();
        Map<String,Object> map = new HashMap<>();
        map.put("buffer",str);
        map.put("css",css);
        map.put("out",System.out);

        ScriptManager.me().runScript(script,map);
        System.out.println(str.toString());
    }

}
