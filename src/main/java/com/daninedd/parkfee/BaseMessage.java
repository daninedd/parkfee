package com.daninedd.parkfee;

import lombok.Data;

@Data
public class BaseMessage {

    protected String ToUserName;
    protected String FromUserName;
    protected Long CreateTime;
    protected String MsgType;

    public BaseMessage(){
        super();
    }
}
