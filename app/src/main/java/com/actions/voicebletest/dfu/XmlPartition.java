package com.actions.voicebletest.dfu;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Created by chang on 2018/5/28.
 */

@Root(name = "partition")
public class XmlPartition {
    @Element(name="type")
    private String type;

    @Element(name="name")
    private String name;

    @Element(name="file")
    private String file;

    @Element(name="address")
    private String address;

    @Element(name="fw_id")

    private int fw_id;

    @Element(name="crc32")
    private String crc32;

    public XmlPartition(){

    }

    public XmlPartition(String type, String name, String file, String address, int fw_id, String crc32){
        this.type = type;
        this.name = name;
        this.file = file;
        this.address = address;
        this.fw_id = fw_id;
        this.crc32 = crc32;
    }

    @Override
    public String toString()
    {
        return "Order{" + "type=" + type + ", name=" + name + " file=" + file + ", address=" + address + " fw_id=" + fw_id + " crc32=" + crc32 + '}';
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getFw_id() {
        return fw_id;
    }

    public void setFw_id(int fw_id) {
        this.fw_id = fw_id;
    }

    public String getCrc32() {
        return crc32;
    }

    public void setCrc32(String crc32) {
        this.crc32 = crc32;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
