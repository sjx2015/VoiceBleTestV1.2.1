package com.actions.voicebletest.dfu;

import com.actions.voicebletest.utils.LittleEndian;

/**
 * Created by chang on 2018/5/30.
 */

public class Partition {
    private short[] name = new short[8];
    private int type;
    private int flag;
    private long offset;
    private short seq;
    private int reserve = 0;
    private long entry_offs = 0;

    public Partition(){

    }

    public Partition(byte[] bytes, int part_entry_size){
        if (bytes.length < part_entry_size)
            return;
        int index = 0;
        for (int i=0; i < 8; i++){
            name[i] = LittleEndian.ByteToShort(bytes[index++]);
        }
        type = LittleEndian.ByteArray16ToInt(bytes,index);
        index += 2;
        flag = LittleEndian.ByteArray16ToInt(bytes,index);
        index += 2;
        offset = LittleEndian.ByteArrayToLong(bytes, index);
        index += 4;
        seq = LittleEndian.ByteArrayToShort(bytes, index);
        index += 2;
        reserve = LittleEndian.ByteArray16ToInt(bytes,index);
        index += 2;
        entry_offs = LittleEndian.ByteArrayToLong(bytes, index);
    }

    public byte[] toLEByteArray(){
        byte []buffer = new byte[24];
        int index = 0;
        for (int i=0; i < 8; i++) {
            buffer[index++] = LittleEndian.ShortToByte(name[i]);
        }
        LittleEndian.fillByteArrayShort(buffer,index,type);
        index += 2;
        LittleEndian.fillByteArrayShort(buffer,index,flag);
        index += 2;
        LittleEndian.fillByteArrayLong(buffer, index, offset);
        index += 4;
        LittleEndian.fillByteArrayShort(buffer,index,seq);
        index += 2;
        LittleEndian.fillByteArrayShort(buffer,index,reserve);
        index += 2;
        LittleEndian.fillByteArrayLong(buffer, index, offset);
        index += 4;
        return buffer;
    }

    public short[] getName() {
        return name;
    }

    public void setName(short[] name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public short getSeq() {
        return seq;
    }

    public void setSeq(short seq) {
        this.seq = seq;
    }

    public int getReserve() {
        return reserve;
    }

    public void setReserve(int reserve) {
        this.reserve = reserve;
    }

    public long getEntry_offs() {
        return entry_offs;
    }

    public void setEntry_offs(long entry_offs) {
        this.entry_offs = entry_offs;
    }

}
