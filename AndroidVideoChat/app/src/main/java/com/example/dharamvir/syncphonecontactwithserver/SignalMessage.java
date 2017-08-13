package com.example.dharamvir.syncphonecontactwithserver;

/**
 * Created by dharamvir on 25/07/2017.
 */

public class SignalMessage {

    private int type;
    private String data;
    private String name = null;
    private String code = null;

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

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
