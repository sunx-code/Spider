package com.sunx.moudle.channel.clubmed;

import com.sunx.constant.Constant;
import com.sunx.downloader.*;
import com.sunx.entity.ResultEntity;
import com.sunx.entity.TaskEntity;
import com.sunx.moudle.annotation.Service;
import com.sunx.moudle.channel.IParser;
import com.sunx.moudle.channel.Wait;
import com.sunx.moudle.dynamic.DriverManager;
import com.sunx.moudle.enums.ImageType;
import com.sunx.utils.FileUtil;
import com.sunx.common.encrypt.MD5;
import com.sunx.storage.DBFactory;
import com.sunx.utils.Helper;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service(id = 1, service = "com.sunx.moudle.channel.clubmed.ClubMedItem")
public class ClubMedItem implements IParser {
    //日志记录类
    private static final Logger logger = LoggerFactory.getLogger(ClubMedItem.class);
    //格式化日期
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat fs = new SimpleDateFormat("yyyyMMdd");

    /**
     *
     * @param factory
     * @param task
     * @return
     */
    public int parser(DBFactory factory, TaskEntity task){
        try{
            //获取到最简单的数据后,开始请求具体的数据内容
            return toSnapshot(factory,task,task.getUrl());
        }catch (Exception e){
            e.printStackTrace();
        }
        return Constant.TASK_FAIL;
    }

    /**
     *
     * @param factory
     * @param task
     * @param link
     * @return
     */
    public int toSnapshot(DBFactory factory,TaskEntity task,String link){
        WebDriver pageDriver = null;
        try{
            logger.info("开始请求数据:" + link);
            //获取浏览器对象
            pageDriver = DriverManager.me().get();
            //请求任务
            pageDriver.navigate().to(link);
            //休眠一定时间,等待页面渲染完毕
            Wait.wait(pageDriver,12,new Object[]{By.cssSelector(".cm-Accommodations-results")});
            //使页面滚动到最底部
            try{
                Actions action = new Actions(pageDriver);
                action.moveByOffset(10000,10000).perform();
            }catch (Exception e){
                e.printStackTrace();
            }

            //模拟点击查看更多房型
            try{
                WebElement ele = pageDriver.findElement(By.cssSelector("#js-AllaccommodationsResults"));
                ele.click();
                Wait.wait(pageDriver,3,new Object[]{});
            }catch (Exception e){}
            return save(factory,task,(RemoteWebDriver) pageDriver);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("渠道id:" + task.getChannelId() + ",任务id:" + task.getId() + ",对应的链接地址为:" + task.getUrl() + ",错误信息为:" + e.getMessage());
            return Constant.TASK_FAIL;
        }finally {
            DriverManager.me().recycle(pageDriver);
        }
    }

    /**
     *
     * @param factory
     * @param task
     * @param pageDriver
     * @return
     */
    public int save(DBFactory factory,TaskEntity task, RemoteWebDriver pageDriver){
        try{
            String html = Helper.toHtml(pageDriver.getPageSource());
            // ===================================
            Date date = new Date();
            String vday = fs.format(date);
            String now = sdf.format(date);
            String region = "Unknown";
            String id = vday + "," + task.getChannelName() + "," + region + "," + task.getCheckInDate() + "," + task.getUrl() + "," + task.getPeopleType();
            String md5 = MD5.md5(id);
            // ===================================
            logger.info("开始处理数据 > " + task.getChannelName() + "\t" + task.getCheckInDate() + " ...");

            // ===================================
            String txtPath = FileUtil.createPageFile(vday, task.getChannelName(), region, task.getCheckInDate(), md5, ImageType.HTML);
            FileUtils.writeStringToFile(new File(txtPath), html, "UTF8");

            // ===================================
            ResultEntity resultEntity = new ResultEntity();
            resultEntity.setId(md5);
            resultEntity.setCheckInDate(task.getCheckInDate());
            resultEntity.setChannelName(task.getChannelName());
            resultEntity.setHouseType(null);
            resultEntity.setPeopleType(task.getPeopleType());
            resultEntity.setRegion(region);
            resultEntity.setTid(task.getId());
            resultEntity.setUrl(task.getUrl());
            resultEntity.setVday(now);
            resultEntity.setPath(txtPath);
            resultEntity.setSleep(task.getSleep());

            factory.insert(Constant.DEFAULT_DB_POOL, resultEntity);
            return Constant.TASK_SUCESS;
        }catch (Exception e){
            e.printStackTrace();
        }
        return Constant.TASK_FAIL;
    }

    public static void main(String[] args) throws Exception{
        ClubMedItem ci = new ClubMedItem();
//        String url = "https://secure.clubmed.com/cm/callProposalSecure.do?PAYS=66&LANG=ZH&village=SAYC&dureeSejour=2&dateDeDebut=2016%2F12%2F29&nbParticipants=2&dateDeNaissance=2010%2F12%2F25&nbEnfants=1";
//        String result = ci.find(url);
//
//        System.out.println(result);

        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setId(123l);
        taskEntity.setChannelName("ClubMed");
        taskEntity.setRegion("三亚");
        taskEntity.setCheckInDate("2017-07-07");
        taskEntity.setSleep(3);
        taskEntity.setUrl("http://www.clubmed.com.cn/cm/callProposal.do?PAYS=66&LANG=ZH&village=SAYC&dateDeDebut=2017%2F07%2F07&dureeSejour=4&nbParticipants=2&nbEnfants=0&dateDeNaissance=");

//        ci.parser(null,taskEntity);

//        Map<String, String> cookies = new HashMap<>();
//
//        Connection connect = Jsoup.connect(taskEntity.getUrl());
//        connect.method(Connection.Method.GET);
//        connect.timeout(10000);
//        connect.cookies(cookies);
//        Connection.Response response = connect.execute();
//        cookies = response.cookies();
//
//        for(Map.Entry<String,String> entry : cookies.entrySet()){
//            System.out.println(entry.getKey() + "\t" + entry.getValue());
//        }

        Downloader downloader = new HttpClientDownloader();
        Site site = new Site();
//        site.addHeader("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
//        site.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
//        site.addHeader("Accept-Encoding","gzip, deflate, sdch");
//        site.addHeader("Accept-Language","zh-CN,zh;q=0.8");
//        site.addHeader("Content-Type","application/json");

        String postData = "{\"body\":{\"id\":58556757,\"dateCreation\":[2017,6,27,8,49,26,0],\"codeCanalDistribution\":\"NET\",\"numeroProposition\":1,\"numeroDossier\":null,\"robinetTransport\":false,\"statut\":\"TEMPORAIRE\",\"dateReservation\":[2017,6,27],\"codePeriodeCommerciale\":\"17E\",\"codePaysGeographique\":\"066\",\"acheminementParDefaut\":null,\"codeLangue\":\"ZH\",\"typeForfait\":\"HOTELIER\",\"montantBookNow\":null,\"montantDynamicPricing\":null,\"gestionnaire\":{\"codeGestionnaire\":\"CBD0238\"},\"recommandation\":{\"codeMiseFileAttente\":null,\"dateMiseFileAttente\":null,\"dossierRer\":false,\"numeroCorporate\":null,\"propositionETicket\":false,\"typeTarif\":null,\"typeTarifReponse\":null,\"tarificationGds\":[]},\"espoir\":null,\"sejourOrServiceOrCircuit\":[{\"@class\":\"com.clubmed.resa.context.v1_0.Accessoire\",\"id\":386131649,\"codeRessource\":null,\"typeRessource\":\"AI\",\"sousTypeRessource\":\"DE\",\"dateDebut\":[2017,7,7],\"dateFin\":[2017,7,11],\"duree\":4,\"codePackage\":null,\"numeroOrdrePackage\":0,\"cumulable\":false,\"ressourceInformative\":null,\"ressourceLieeAvant\":null,\"ressourceLieeApres\":null,\"typeAccessoire\":null,\"sumPrices\":170,\"minPrice\":85,\"logementCommercial\":[],\"prix\":[{\"id\":1933456745,\"referencePersonne\":\"A\",\"montant\":85},{\"id\":1933456746,\"referencePersonne\":\"B\",\"montant\":85}],\"tronconDate\":[]},{\"@class\":\"com.clubmed.resa.context.v1_0.Accessoire\",\"id\":386131648,\"codeRessource\":null,\"typeRessource\":\"AI\",\"sousTypeRessource\":\"CA\",\"dateDebut\":[2017,7,7],\"dateFin\":[2017,7,11],\"duree\":4,\"codePackage\":null,\"numeroOrdrePackage\":0,\"cumulable\":false,\"ressourceInformative\":null,\"ressourceLieeAvant\":null,\"ressourceLieeApres\":null,\"typeAccessoire\":null,\"sumPrices\":360,\"minPrice\":180,\"logementCommercial\":[],\"prix\":[{\"id\":1933456743,\"referencePersonne\":\"A\",\"montant\":180},{\"id\":1933456744,\"referencePersonne\":\"B\",\"montant\":180}],\"tronconDate\":[]},{\"@class\":\"com.clubmed.resa.context.v1_0.Sejour\",\"id\":386131646,\"codeRessource\":\"SAYC\",\"typeRessource\":\"SE\",\"sousTypeRessource\":\"CE\",\"dateDebut\":[2017,7,7],\"dateFin\":[2017,7,11],\"duree\":4,\"codePackage\":null,\"numeroOrdrePackage\":0,\"cumulable\":true,\"ressourceInformative\":null,\"ressourceLieeAvant\":null,\"ressourceLieeApres\":null,\"codeFormule\":\"OAS\",\"codeForfait\":\"AI\",\"robinetSejour\":false,\"typeFormule\":\"LIBRE\",\"sumPrices\":16872,\"minPrice\":8436,\"logementCommercial\":[{\"id\":70091772,\"codeLogementCommercial\":\"A2+\",\"codeOccupation\":2,\"typeVenteOccupation\":\"UNITE\",\"codeInfoDisponibiliteOccupation\":28,\"codeInfoDisponibiliteOccupationInitiale\":132,\"referencePersonne\":[\"A\",\"B\"],\"codeLogementCommercial1\":null,\"codeOccupation1\":null,\"codeLogementCommercial2\":null,\"codeOccupation2\":null,\"nombreLogement\":1,\"topGestionLitBB\":false}],\"prix\":[{\"id\":1933456739,\"referencePersonne\":\"A\",\"montant\":8436},{\"id\":1933456740,\"referencePersonne\":\"B\",\"montant\":8436}],\"tronconDate\":[]},{\"@class\":\"com.clubmed.resa.context.v1_0.Service\",\"id\":386131647,\"codeRessource\":\"SAYSAI\",\"typeRessource\":\"SV\",\"sousTypeRessource\":\"CB\",\"dateDebut\":[2017,7,7],\"dateFin\":[2017,7,11],\"duree\":4,\"codePackage\":null,\"numeroOrdrePackage\":0,\"cumulable\":false,\"ressourceInformative\":null,\"ressourceLieeAvant\":null,\"ressourceLieeApres\":{\"codeRessource\":\"SAYC\",\"typeRessource\":\"SE\",\"dateDebut\":[2017,7,7],\"codePackage\":null,\"numeroOrdrePackage\":0},\"inclusForfait\":true,\"natureService\":\"G\",\"nombreService\":null,\"codeInformationDisponibilite\":0,\"codeCategorieService\":\"CB1\",\"informationTransfertVv\":null,\"trancheHoraire\":\"  \",\"codeInformationDisponibiliteInitiale\":0,\"sumPrices\":0,\"minPrice\":null,\"logementCommercial\":[],\"prix\":[{\"id\":1933456741,\"referencePersonne\":\"A\",\"montant\":0},{\"id\":1933456742,\"referencePersonne\":\"B\",\"montant\":0}],\"tronconDate\":[]}],\"personne\":[{\"id\":161218523,\"dateNaissance\":null,\"reference\":\"A\",\"age\":null,\"numeroClient\":0,\"nom\":null,\"prenom\":null,\"civilite\":null,\"lettreCodeFidelite\":null,\"numeroFiliation\":null,\"partitionClient\":null,\"topContact\":null,\"dateDebutSejour\":[2017,7,7],\"isChildren\":false,\"isBebe\":false},{\"id\":161218524,\"dateNaissance\":null,\"reference\":\"B\",\"age\":null,\"numeroClient\":0,\"nom\":null,\"prenom\":null,\"civilite\":null,\"lettreCodeFidelite\":null,\"numeroFiliation\":null,\"partitionClient\":null,\"topContact\":null,\"dateDebutSejour\":[2017,7,7],\"isChildren\":false,\"isBebe\":false}],\"promotion\":[{\"id\":49029070,\"codePromotion\":\"4S7S35\",\"codeReduction\":\"RED-35\",\"numeroBon\":null,\"numeroTarif\":null,\"codeAjustement\":\"Y35X\",\"typePromotion\":\"TFO\",\"promotionCible\":false,\"codeMessage\":0,\"sumPrices\":-5906,\"minPrice\":-2953,\"logementCommercial\":[],\"prix\":[{\"id\":1933456737,\"referencePersonne\":\"A\",\"montant\":-2953,\"codeGroupeAjustement\":null,\"referencePersonneContributive\":[]},{\"id\":1933456738,\"referencePersonne\":\"B\",\"montant\":-2953,\"codeGroupeAjustement\":null,\"referencePersonneContributive\":[]}],\"tronconDate\":[]}],\"attributaire\":{\"codePaysCommercial\":\"370\",\"codeReseauDistribution\":\"WB\",\"codeAgent\":\"WEB37\",\"codeDevise\":\"CNY\"}}}";


        String url = "https://secure.clubmed.com.cn/cm/be/api/14146/accommodations/true";

//        Request request = new Request(url).setMethod(Method.POST).setSrt(postData);

        String data = downloader.downloader(new Request(taskEntity.getUrl()),
                site.setTimeOut(10000).setIsSave(true));

        System.out.println(data);
    }
}
