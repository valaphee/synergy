/*
 * Copyright (c) 2019-2022, GrieferGames, Valaphee.
 * All rights reserved.
 */

package network.ycc.raknet.packet;

import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;

public class ServerHandshake extends SimpleFramedPacket {

    private InetSocketAddress clientAddr;
    private long timestamp;
    private int nExtraAddresses;

    public ServerHandshake() {
        reliability = Reliability.RELIABLE;
    }

    public ServerHandshake(InetSocketAddress clientAddr, long timestamp) {
        this(clientAddr, timestamp, 20);
    }

    public ServerHandshake(InetSocketAddress clientAddr, long timestamp, int nExtraAddresses) {
        this();
        this.clientAddr = clientAddr;
        this.timestamp = timestamp;
        this.nExtraAddresses = nExtraAddresses;
    }

    @Override
    public void encode(ByteBuf buf) {
        writeAddress(buf, clientAddr);
        buf.writeShort(0);
        // The Hive Begin
        /*for (int i = 0; i < nExtraAddresses; i++) {
            writeAddress(buf);
        }*/
        buf.writeBytes(new byte[]{0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06});
        // The Hive End
        buf.writeLong(timestamp);
        buf.writeLong(System.currentTimeMillis());
    }

    @Override
    public void decode(ByteBuf buf) {
        clientAddr = readAddress(buf);
        buf.readShort();
        // The Hive Begin
        /*for (nExtraAddresses = 0; buf.readableBytes() > 16; nExtraAddresses++) {
            readAddress(buf);
        }*/
        buf.readerIndex(buf.readableBytes() - 0xF);
        // The Hive End
        timestamp = buf.readLong();
        timestamp = buf.readLong();
    }

    public InetSocketAddress getClientAddr() {
        return clientAddr;
    }

    public void setClientAddr(InetSocketAddress clientAddr) {
        this.clientAddr = clientAddr;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getnExtraAddresses() {
        return nExtraAddresses;
    }

    public void setnExtraAddresses(int nExtraAddresses) {
        this.nExtraAddresses = nExtraAddresses;
    }

}
