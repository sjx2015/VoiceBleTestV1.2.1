package com.actions.voicebletest.dfu;

/**
 * Created by chang on 2018/6/14.
 */

public class CancelByUserException extends Exception {

    //无参构造方法
    public CancelByUserException(){

        super();
    }

    //有参的构造方法
    public CancelByUserException(String message){
        super(message);

    }

    // 用指定的详细信息和原因构造一个新的异常
    public CancelByUserException(String message, Throwable cause){

        super(message,cause);
    }

    //用指定原因构造一个新的异常
    public CancelByUserException(Throwable cause) {

        super(cause);
    }

}

