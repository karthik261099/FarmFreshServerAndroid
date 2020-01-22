package com.kbrosapp.farmerportal;

public interface MessageListener {


    void messageReceived(String phone,String msgBody,String time,String message);


}
