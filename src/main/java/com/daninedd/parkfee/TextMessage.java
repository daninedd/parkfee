package com.daninedd.parkfee;

import lombok.Data;

public class TextMessage extends BaseMessage{

    private String Content;
    private String MsgId;

    public TextMessage(){
        super();
    }
}
