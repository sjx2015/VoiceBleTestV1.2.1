package com.actions.voicebletest.dfu;

import com.actions.voicebletest.utils.LittleEndian;

/**
 * Created by chang on 2018/6/5.
 */

public class PartitionTable {
    private long magic;
    private int version;
    private int tale_size;
    private int part_cnt;
    private int part_entry_size;
    private short []reserved1 = new short[4];

    private Partition []parts = new Partition[15];
    private short []reserved2 = new short[4];
    private long table_crc;

    public PartitionTable(){

    }

    public  PartitionTable(byte bytes[]){
        if (bytes.length < 384)
            return;
        int index = 0;
        magic = LittleEndian.ByteArrayToLong(bytes, index);
        index += 4;
        version = LittleEndian.ByteArray16ToInt(bytes,index);
        index += 2;
        tale_size = LittleEndian.ByteArray16ToInt(bytes,index);
        index += 2;
        part_cnt = LittleEndian.ByteArray16ToInt(bytes,index);
        index += 2;
        part_entry_size = LittleEndian.ByteArray16ToInt(bytes,index);
        index += 2;
        for (int i=0; i < 4; i++){
            reserved1[i] = LittleEndian.ByteToShort(bytes[index++]);
        }
        for (int i=0; i < 15; i++){
            byte []p = new byte[24];
            System.arraycopy(bytes, index, p, 0, 24);
            parts[i] = new Partition(p,24);
            index += 24;
        }
        for (int i=0; i < 4; i++){
            reserved2[i] = LittleEndian.ByteToShort(bytes[index++]);
        }
        table_crc = LittleEndian.ByteArrayToLong(bytes, index);
    }

    public byte[] toLEByteArray(){
        byte []buffer = new byte[384];
        int index = 0;
        LittleEndian.fillByteArrayLong(buffer, index, magic);
        index += 4;
        LittleEndian.fillByteArrayShort(buffer, index, version);
        index += 2;
        LittleEndian.fillByteArrayShort(buffer, index, tale_size);
        index += 2;
        LittleEndian.fillByteArrayShort(buffer, index, part_cnt);
        index += 2;
        LittleEndian.fillByteArrayShort(buffer, index, part_entry_size);
        index += 2;
        for (int i=0; i < 4; i++) {
            buffer[index++] = LittleEndian.ShortToByte(reserved1[i]);
        }
        for (int i=0; i < 15; i++){
            byte []p = parts[i].toLEByteArray();
            System.arraycopy(p, 0, buffer, index, 24);
            index += 24;
        }
        for (int i=0; i < 4; i++) {
            buffer[index++] = LittleEndian.ShortToByte(reserved2[i]);
        }
        LittleEndian.fillByteArrayLong(buffer, index, table_crc);
        index += 4;
        return buffer;
    }

    public long getMagic() {
        return magic;
    }

    public void setMagic(long magic) {
        this.magic = magic;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getTale_size() {
        return tale_size;
    }

    public void setTale_size(int tale_size) {
        this.tale_size = tale_size;
    }

    public int getPart_cnt() {
        return part_cnt;
    }

    public void setPart_cnt(int part_cnt) {
        this.part_cnt = part_cnt;
    }

    public int getPart_entry_size() {
        return part_entry_size;
    }

    public void setPart_entry_size(int part_entry_size) {
        this.part_entry_size = part_entry_size;
    }

    public short[] getReserved1() {
        return reserved1;
    }

    public void setReserved1(short[] reserved1) {
        this.reserved1 = reserved1;
    }

    public Partition[] getParts() {
        return parts;
    }

    public void setParts(Partition[] parts) {
        this.parts = parts;
    }

    public short[] getReserved2() {
        return reserved2;
    }

    public void setReserved2(short[] reserved2) {
        this.reserved2 = reserved2;
    }

    public long getTable_crc() {
        return table_crc;
    }

    public void setTable_crc(long table_crc) {
        this.table_crc = table_crc;
    }
}
