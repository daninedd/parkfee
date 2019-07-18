package com.daninedd.parkfee;

import com.daninedd.parkfee.parkfee.FeeDetailData;
import com.daninedd.parkfee.parkfee.FeeDetailDataRepository;
import com.daninedd.parkfee.parkfee.QFeeDetailData;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.SneakyThrows;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Controller
@ResponseBody
public class TestController {

    private Logger logger = LoggerFactory.getLogger(TestController.class);

    private final static String TOKEN = "daninedd";

    @Resource
    private FeeDetailDataRepository feeDetailDataRepository;

    @PersistenceContext
    private EntityManager entityManager;


    @RequestMapping("/check_wx")
    public String check_wx(){
        logger.info("wx_come_in");
        return "1";
    }

//    @SneakyThrows
//    @RequestMapping("/check")
//    public String checkSignature(HttpServletRequest request)
//    {
//        request.setCharacterEncoding("UTF-8");
//        String signature = request.getParameter("signature");
//        String timestamp = request.getParameter("timestamp");
//        String nonce = request.getParameter("nonce");
//        String echostr = request.getParameter("echostr");
//
//        String res = "nonce="+nonce+"&signature="+signature+"&timestamp="+timestamp;
//        String sss = DigestUtils.sha1Hex(res);
//        if (sss.equals(signature)){
//            return echostr;
//        }
//        logger.info("sha1: " + res);
//        logger.info("echostr: " + nonce);
//        return echostr;
//    }

    @SneakyThrows
    @RequestMapping("/check")
    public void response(HttpServletRequest request, HttpServletResponse response)
    {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = null;
        //将微信请求xml转为map格式，获取所需的参数
        Map<String,String> map = MessageUtil.xmlToMap(request);
        String ToUserName = map.get("ToUserName");
        String FromUserName = map.get("FromUserName");
        String MsgType = map.get("MsgType");
        String Content = map.get("Content");

        String message = null;
        String re_content = "";
        //处理文本类型，实现输入1，回复相应的封装的内容
        if("text".equals(MsgType)){
            TextMessageUtil textMessage = new TextMessageUtil();
            if("1".equals(Content)){
                re_content = getReContent();
            }else if(Content.contains("reduce")){
                re_content = "修改失败！";
                String[] str = Content.split(" ");
                if (isValidDate(str[str.length-1])){
                    updateDate(str[str.length-1], str[str.length-2]);
                    re_content = "修改成功!";
                }
            }
            else{
                re_content = "张凯哈皮";
            }
            message = textMessage.initMessage(FromUserName, ToUserName, re_content);
        }
        try {
            out = response.getWriter();
            out.write(message);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        out.close();
    }


    private String getReContent(){
        QFeeDetailData qFeeDetailData = QFeeDetailData.feeDetailData;

        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        List<FeeDetailData> feeDetailDatas = jpaQueryFactory.selectFrom(qFeeDetailData).fetch();
        double parkFee = 0.0, oilFee = 0.0;
        for (FeeDetailData feeDetailData : feeDetailDatas
             ) {
            parkFee += feeDetailData.getParkFee();
            oilFee += feeDetailData.getOilFee();
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("截止");
        stringBuilder.append(simpleDateFormat.format(new Date()));
        stringBuilder.append("。您已节约停车费: ");
        stringBuilder.append(parkFee);
        stringBuilder.append("元,燃油费: ");
        stringBuilder.append(oilFee);
        stringBuilder.append("元。合计: ");
        stringBuilder.append(parkFee+oilFee);
        stringBuilder.append("元。再接再厉！");
        return stringBuilder.toString();
    }

    //根据日期修改停车费
    private void updateDate(String date, String type){
        try {
            Date date1 = new SimpleDateFormat("yyyyMMdd").parse(date);
            String date2 = new SimpleDateFormat("yyyy-MM-dd").format(date1);
            Date date3 = new SimpleDateFormat("yyyy-MM-dd").parse(date2);

            QFeeDetailData qFeeDetailData = QFeeDetailData.feeDetailData;
            JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
            FeeDetailData feeDetailData = jpaQueryFactory.selectFrom(qFeeDetailData).where(qFeeDetailData.date.eq(date3)).fetchOne();
            if (feeDetailData != null){
                switch (type){
                    case "停车费"://更新停车费
                        feeDetailData.setParkFee(0.0);
                        break;
                    case "油费"://更新油费
                        feeDetailData.setOilFee(0.0);
                        break;
                    case "全部"://一起更新
                        feeDetailData.setParkFee(0.0);
                        feeDetailData.setOilFee(0.0);
                        break;
                    default:
                        break;
                }
                feeDetailDataRepository.save(feeDetailData);
            }
        }
        catch (ParseException e){
            e.printStackTrace();
        }

    }


    //校验8位字符串是否为正确的日期格式
    private static boolean isValidDate(String str) {
        boolean result = true;
        //判断字符串长度是否为8位
        if(str.length() == 8){
            // 指定日期格式为四位年/两位月份/两位日期，注意yyyy/MM/dd区分大小写；
            //SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            try {
                // 设置lenient为false.
                // 否则SimpleDateFormat会比较宽松地验证日期，比如2007/02/29会被接受，并转换成2007/03/01
                format.setLenient(false);
                format.parse(str);
            } catch (ParseException e) {
                // e.printStackTrace();
                // 如果throw java.text.ParseException或者NullPointerException，就说明格式不对
                result = false;
            }
        }else{
            result = false;
        }

        return result;
    }
}
