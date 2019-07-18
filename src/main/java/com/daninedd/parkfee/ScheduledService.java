package com.daninedd.parkfee;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.daninedd.parkfee.parkfee.FeeDetailData;
import com.daninedd.parkfee.parkfee.FeeDetailDataRepository;
import com.daninedd.parkfee.parkfee.QFeeDetailData;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@EnableScheduling
@Component
public class ScheduledService {

    private Logger logger = LoggerFactory.getLogger(ScheduledService.class);

    private final static String start_date = "2019-07-15";
    private final static String cron = "0 0 3 * * ?";//每天三点执行
    private final static String cron1 = "0/3 * * * * ?";//每三秒执行

    private final static String URL = "http://api.goseek.cn/Tools/holiday?date=";

    @Resource
    private FeeDetailDataRepository feeDetailDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Scheduled(cron = cron)
    public void scheduled(){

        //查看是否需要更新
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        QFeeDetailData qFeeDetailData = QFeeDetailData.feeDetailData;
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);

        while (true){

            try {
                FeeDetailData feeDetailData = jpaQueryFactory.selectFrom(qFeeDetailData).where(qFeeDetailData.date.eq(new SimpleDateFormat("yyyy-MM-dd").parse(date))).fetchOne();
                if (feeDetailData == null){//当前日期没有数据。新增
                    double parkFee = 0, oilFee = 0;
                    if(isPayParkFee(date)){//需要支付车费
                        parkFee = 35;
                    }
                    if (isWeekend(date)) {
                        oilFee = 200;
                    }
                    FeeDetailData feeDetailData1 = new FeeDetailData();
                    feeDetailData1.setCreateTime(new Date());
                    feeDetailData1.setDate(new SimpleDateFormat("yyyy-MM-dd").parse(date));
                    feeDetailData1.setParkFee(parkFee);
                    feeDetailData1.setOilFee(oilFee);
                    feeDetailDataRepository.save(feeDetailData1);
                    date = getLastDay(date);
                    //每次保存后休息3秒，减少数据库IO
                    try {
                        Thread.sleep(3000);
                    }
                    catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    //先查询今天是否存在数据，如果存在，则跳过，否则往前一直递归到7月15号，因为我从7月15号开始骑电瓶车
                    if (date.equals(start_date)){
                        break;
                    }
                }else {
                    break;
                }
            }
            catch (java.text.ParseException e){
                logger.error(e.getMessage());
            }
        }
    }


    /**
     * 是否需要支付停车费
     * */
    private Boolean isPayParkFee(String date){
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet(URL + date);
        CloseableHttpResponse httpResponse = null;
        try {
            // 由客户端执行(发送)Get请求
            httpResponse = httpClient.execute(httpGet);
            HttpEntity httpEntity = httpResponse.getEntity();
            System.out.println("响应状态为：" + httpResponse.getStatusLine());

            if (httpEntity != null){
                System.out.println(httpEntity);
                String en = EntityUtils.toString(httpEntity);
                JSONObject jsonObject = JSON.parseObject(en);
                String code = jsonObject.getString("code");
                String data = jsonObject.getString("data");
                //正常工作日对应结果为 0, 法定节假日对应结果为 1, 节假日调休补班对应的结果为 2，休息日对应结果为 3
                if(code.equals("10000")){
                    return (data.equals("0") || data.equals("2"));
                }
                else {
                    logger.error("api调用失败");
                    throw new RuntimeException("api调用失败!");
                }
            }
        }
        catch (ClientProtocolException e){
            logger.error(e.getMessage());
        }
        catch (IOException e){
            logger.error(e.getMessage());
        }
        catch (ParseException e){
            logger.error(e.getMessage());
        }
        finally {
            //释放资源
            try {
                if (httpClient != null){
                    httpClient.close();
                }
                if (httpResponse != null){
                    httpResponse.close();
                }
            }
            catch (IOException e){
                logger.error(e.getMessage());
            }
        }
        return true;
    }

    private static String getLastDay(String time){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        Date date=null;
        try {
            date = sdf.parse(time);
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        calendar.setTime(date);
        int day=calendar.get(Calendar.DATE);
        //                      此处修改为+1则是获取后一天
        calendar.set(Calendar.DATE,day-1);

        String lastDay = sdf.format(calendar.getTime());
        return lastDay;
    }

    private static Boolean isWeekend(String dateStr) {
        boolean isWeekend = false;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            isWeekend = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return isWeekend;
    }
}
