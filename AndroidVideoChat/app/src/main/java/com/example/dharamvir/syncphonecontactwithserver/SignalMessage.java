package com.example.dharamvir.syncphonecontactwithserver;

/**
 * Created by dharamvir on 25/07/2017.
 */

public class SignalMessage {

    private int type;
    private String data;

    public void setType(int type)
    {
        this.type = type;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getType() {
        return type;
    }

    public String getData() {
        return data;
    }
}
