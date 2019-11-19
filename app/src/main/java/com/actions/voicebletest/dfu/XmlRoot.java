package com.actions.voicebletest.dfu;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chang on 2018/5/28.
 */

@Root(name = "partitions", strict = false)
public class XmlRoot {
    @Element(name="partitionsNum")
    private int partitionsNum;
    @Element(name="version")
    private String version;

    @ElementList(inline=true,required = false)
    private List<XmlPartition> mXmlPartitons;

    public XmlRoot(){
        mXmlPartitons = new ArrayList<>();
    }

    public int getPartitionsNum() {
        return partitionsNum;
    }

    public void setPartitionsNum(int partitionsNum) {
        this.partitionsNum = partitionsNum;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<XmlPartition> getmXmlPartitons() {
        return mXmlPartitons;
    }

    public void setmXmlPartitons(List<XmlPartition> mXmlPartitons) {
        this.mXmlPartitons = mXmlPartitons;
    }
}
