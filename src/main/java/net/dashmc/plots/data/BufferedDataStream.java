package net.dashmc.plots.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufProcessor;

public class BufferedDataStream extends ByteBuf {
	private ByteBuf buffer;

	public BufferedDataStream(ByteBuf buf) {
		this.buffer = buf;
	}

	public UUID readUUID() {
		return new UUID(readLong(), readLong());
	}

	public void writeUUID(UUID uuid) {
		this.writeLong(uuid.getMostSignificantBits());
		this.writeLong(uuid.getLeastSignificantBits());
	}

	public int capacity() {
		return this.buffer.capacity();
	}

	public ByteBuf capacity(int i) {
		return this.buffer.capacity(i);
	}

	public int maxCapacity() {
		return this.buffer.maxCapacity();
	}

	public ByteBufAllocator alloc() {
		return this.buffer.alloc();
	}

	public ByteOrder order() {
		return this.buffer.order();
	}

	public ByteBuf order(ByteOrder byteorder) {
		return this.buffer.order(byteorder);
	}

	public ByteBuf unwrap() {
		return this.buffer.unwrap();
	}

	public boolean isDirect() {
		return this.buffer.isDirect();
	}

	public int readerIndex() {
		return this.buffer.readerIndex();
	}

	public ByteBuf readerIndex(int i) {
		return this.buffer.readerIndex(i);
	}

	public int writerIndex() {
		return this.buffer.writerIndex();
	}

	public ByteBuf writerIndex(int i) {
		return this.buffer.writerIndex(i);
	}

	public ByteBuf setIndex(int i, int j) {
		return this.buffer.setIndex(i, j);
	}

	public int readableBytes() {
		return this.buffer.readableBytes();
	}

	public int writableBytes() {
		return this.buffer.writableBytes();
	}

	public int maxWritableBytes() {
		return this.buffer.maxWritableBytes();
	}

	public boolean isReadable() {
		return this.buffer.isReadable();
	}

	public boolean isReadable(int i) {
		return this.buffer.isReadable(i);
	}

	public boolean isWritable() {
		return this.buffer.isWritable();
	}

	public boolean isWritable(int i) {
		return this.buffer.isWritable(i);
	}

	public ByteBuf clear() {
		return this.buffer.clear();
	}

	public ByteBuf markReaderIndex() {
		return this.buffer.markReaderIndex();
	}

	public ByteBuf resetReaderIndex() {
		return this.buffer.resetReaderIndex();
	}

	public ByteBuf markWriterIndex() {
		return this.buffer.markWriterIndex();
	}

	public ByteBuf resetWriterIndex() {
		return this.buffer.resetWriterIndex();
	}

	public ByteBuf discardReadBytes() {
		return this.buffer.discardReadBytes();
	}

	public ByteBuf discardSomeReadBytes() {
		return this.buffer.discardSomeReadBytes();
	}

	public ByteBuf ensureWritable(int i) {
		return this.buffer.ensureWritable(i);
	}

	public int ensureWritable(int i, boolean flag) {
		return this.buffer.ensureWritable(i, flag);
	}

	public boolean getBoolean(int i) {
		return this.buffer.getBoolean(i);
	}

	public byte getByte(int i) {
		return this.buffer.getByte(i);
	}

	public short getUnsignedByte(int i) {
		return this.buffer.getUnsignedByte(i);
	}

	public short getShort(int i) {
		return this.buffer.getShort(i);
	}

	public int getUnsignedShort(int i) {
		return this.buffer.getUnsignedShort(i);
	}

	public int getMedium(int i) {
		return this.buffer.getMedium(i);
	}

	public int getUnsignedMedium(int i) {
		return this.buffer.getUnsignedMedium(i);
	}

	public int getInt(int i) {
		return this.buffer.getInt(i);
	}

	public long getUnsignedInt(int i) {
		return this.buffer.getUnsignedInt(i);
	}

	public long getLong(int i) {
		return this.buffer.getLong(i);
	}

	public char getChar(int i) {
		return this.buffer.getChar(i);
	}

	public float getFloat(int i) {
		return this.buffer.getFloat(i);
	}

	public double getDouble(int i) {
		return this.buffer.getDouble(i);
	}

	public ByteBuf getBytes(int i, ByteBuf bytebuf) {
		return this.buffer.getBytes(i, bytebuf);
	}

	public ByteBuf getBytes(int i, ByteBuf bytebuf, int j) {
		return this.buffer.getBytes(i, bytebuf, j);
	}

	public ByteBuf getBytes(int i, ByteBuf bytebuf, int j, int k) {
		return this.buffer.getBytes(i, bytebuf, j, k);
	}

	public ByteBuf getBytes(int i, byte[] abyte) {
		return this.buffer.getBytes(i, abyte);
	}

	public ByteBuf getBytes(int i, byte[] abyte, int j, int k) {
		return this.buffer.getBytes(i, abyte, j, k);
	}

	public ByteBuf getBytes(int i, ByteBuffer bytebuffer) {
		return this.buffer.getBytes(i, bytebuffer);
	}

	public ByteBuf getBytes(int i, OutputStream outputstream, int j) throws IOException {
		return this.buffer.getBytes(i, outputstream, j);
	}

	public int getBytes(int i, GatheringByteChannel gatheringbytechannel, int j) throws IOException {
		return this.buffer.getBytes(i, gatheringbytechannel, j);
	}

	public ByteBuf setBoolean(int i, boolean flag) {
		return this.buffer.setBoolean(i, flag);
	}

	public ByteBuf setByte(int i, int j) {
		return this.buffer.setByte(i, j);
	}

	public ByteBuf setShort(int i, int j) {
		return this.buffer.setShort(i, j);
	}

	public ByteBuf setMedium(int i, int j) {
		return this.buffer.setMedium(i, j);
	}

	public ByteBuf setInt(int i, int j) {
		return this.buffer.setInt(i, j);
	}

	public ByteBuf setLong(int i, long j) {
		return this.buffer.setLong(i, j);
	}

	public ByteBuf setChar(int i, int j) {
		return this.buffer.setChar(i, j);
	}

	public ByteBuf setFloat(int i, float f) {
		return this.buffer.setFloat(i, f);
	}

	public ByteBuf setDouble(int i, double d0) {
		return this.buffer.setDouble(i, d0);
	}

	public ByteBuf setBytes(int i, ByteBuf bytebuf) {
		return this.buffer.setBytes(i, bytebuf);
	}

	public ByteBuf setBytes(int i, ByteBuf bytebuf, int j) {
		return this.buffer.setBytes(i, bytebuf, j);
	}

	public ByteBuf setBytes(int i, ByteBuf bytebuf, int j, int k) {
		return this.buffer.setBytes(i, bytebuf, j, k);
	}

	public ByteBuf setBytes(int i, byte[] abyte) {
		return this.buffer.setBytes(i, abyte);
	}

	public ByteBuf setBytes(int i, byte[] abyte, int j, int k) {
		return this.buffer.setBytes(i, abyte, j, k);
	}

	public ByteBuf setBytes(int i, ByteBuffer bytebuffer) {
		return this.buffer.setBytes(i, bytebuffer);
	}

	public int setBytes(int i, InputStream inputstream, int j) throws IOException {
		return this.buffer.setBytes(i, inputstream, j);
	}

	public int setBytes(int i, ScatteringByteChannel scatteringbytechannel, int j) throws IOException {
		return this.buffer.setBytes(i, scatteringbytechannel, j);
	}

	public ByteBuf setZero(int i, int j) {
		return this.buffer.setZero(i, j);
	}

	public boolean readBoolean() {
		return this.buffer.readBoolean();
	}

	public byte readByte() {
		return this.buffer.readByte();
	}

	public short readUnsignedByte() {
		return this.buffer.readUnsignedByte();
	}

	public short readShort() {
		return this.buffer.readShort();
	}

	public int readUnsignedShort() {
		return this.buffer.readUnsignedShort();
	}

	public int readMedium() {
		return this.buffer.readMedium();
	}

	public int readUnsignedMedium() {
		return this.buffer.readUnsignedMedium();
	}

	public int readInt() {
		return this.buffer.readInt();
	}

	public long readUnsignedInt() {
		return this.buffer.readUnsignedInt();
	}

	public long readLong() {
		return this.buffer.readLong();
	}

	public char readChar() {
		return this.buffer.readChar();
	}

	public float readFloat() {
		return this.buffer.readFloat();
	}

	public double readDouble() {
		return this.buffer.readDouble();
	}

	public ByteBuf readBytes(int i) {
		return this.buffer.readBytes(i);
	}

	public ByteBuf readSlice(int i) {
		return this.buffer.readSlice(i);
	}

	public ByteBuf readBytes(ByteBuf bytebuf) {
		return this.buffer.readBytes(bytebuf);
	}

	public ByteBuf readBytes(ByteBuf bytebuf, int i) {
		return this.buffer.readBytes(bytebuf, i);
	}

	public ByteBuf readBytes(ByteBuf bytebuf, int i, int j) {
		return this.buffer.readBytes(bytebuf, i, j);
	}

	public ByteBuf readBytes(byte[] abyte) {
		return this.buffer.readBytes(abyte);
	}

	public ByteBuf readBytes(byte[] abyte, int i, int j) {
		return this.buffer.readBytes(abyte, i, j);
	}

	public ByteBuf readBytes(ByteBuffer bytebuffer) {
		return this.buffer.readBytes(bytebuffer);
	}

	public ByteBuf readBytes(OutputStream outputstream, int i) throws IOException {
		return this.buffer.readBytes(outputstream, i);
	}

	public int readBytes(GatheringByteChannel gatheringbytechannel, int i) throws IOException {
		return this.buffer.readBytes(gatheringbytechannel, i);
	}

	public ByteBuf skipBytes(int i) {
		return this.buffer.skipBytes(i);
	}

	public ByteBuf writeBoolean(boolean flag) {
		return this.buffer.writeBoolean(flag);
	}

	public ByteBuf writeByte(int i) {
		return this.buffer.writeByte(i);
	}

	public ByteBuf writeShort(int i) {
		return this.buffer.writeShort(i);
	}

	public ByteBuf writeMedium(int i) {
		return this.buffer.writeMedium(i);
	}

	public ByteBuf writeInt(int i) {
		return this.buffer.writeInt(i);
	}

	public ByteBuf writeLong(long i) {
		return this.buffer.writeLong(i);
	}

	public ByteBuf writeChar(int i) {
		return this.buffer.writeChar(i);
	}

	public ByteBuf writeFloat(float f) {
		return this.buffer.writeFloat(f);
	}

	public ByteBuf writeDouble(double d0) {
		return this.buffer.writeDouble(d0);
	}

	public ByteBuf writeBytes(ByteBuf bytebuf) {
		return this.buffer.writeBytes(bytebuf);
	}

	public ByteBuf writeBytes(ByteBuf bytebuf, int i) {
		return this.buffer.writeBytes(bytebuf, i);
	}

	public ByteBuf writeBytes(ByteBuf bytebuf, int i, int j) {
		return this.buffer.writeBytes(bytebuf, i, j);
	}

	public ByteBuf writeBytes(byte[] abyte) {
		return this.buffer.writeBytes(abyte);
	}

	public ByteBuf writeBytes(byte[] abyte, int i, int j) {
		return this.buffer.writeBytes(abyte, i, j);
	}

	public ByteBuf writeBytes(ByteBuffer bytebuffer) {
		return this.buffer.writeBytes(bytebuffer);
	}

	public int writeBytes(InputStream inputstream, int i) throws IOException {
		return this.buffer.writeBytes(inputstream, i);
	}

	public int writeBytes(ScatteringByteChannel scatteringbytechannel, int i) throws IOException {
		return this.buffer.writeBytes(scatteringbytechannel, i);
	}

	public ByteBuf writeZero(int i) {
		return this.buffer.writeZero(i);
	}

	public int indexOf(int i, int j, byte b0) {
		return this.buffer.indexOf(i, j, b0);
	}

	public int bytesBefore(byte b0) {
		return this.buffer.bytesBefore(b0);
	}

	public int bytesBefore(int i, byte b0) {
		return this.buffer.bytesBefore(i, b0);
	}

	public int bytesBefore(int i, int j, byte b0) {
		return this.buffer.bytesBefore(i, j, b0);
	}

	public int forEachByte(ByteBufProcessor bytebufprocessor) {
		return this.buffer.forEachByte(bytebufprocessor);
	}

	public int forEachByte(int i, int j, ByteBufProcessor bytebufprocessor) {
		return this.buffer.forEachByte(i, j, bytebufprocessor);
	}

	public int forEachByteDesc(ByteBufProcessor bytebufprocessor) {
		return this.buffer.forEachByteDesc(bytebufprocessor);
	}

	public int forEachByteDesc(int i, int j, ByteBufProcessor bytebufprocessor) {
		return this.buffer.forEachByteDesc(i, j, bytebufprocessor);
	}

	public ByteBuf copy() {
		return this.buffer.copy();
	}

	public ByteBuf copy(int i, int j) {
		return this.buffer.copy(i, j);
	}

	public ByteBuf slice() {
		return this.buffer.slice();
	}

	public ByteBuf slice(int i, int j) {
		return this.buffer.slice(i, j);
	}

	public ByteBuf duplicate() {
		return this.buffer.duplicate();
	}

	public int nioBufferCount() {
		return this.buffer.nioBufferCount();
	}

	public ByteBuffer nioBuffer() {
		return this.buffer.nioBuffer();
	}

	public ByteBuffer nioBuffer(int i, int j) {
		return this.buffer.nioBuffer(i, j);
	}

	public ByteBuffer internalNioBuffer(int i, int j) {
		return this.buffer.internalNioBuffer(i, j);
	}

	public ByteBuffer[] nioBuffers() {
		return this.buffer.nioBuffers();
	}

	public ByteBuffer[] nioBuffers(int i, int j) {
		return this.buffer.nioBuffers(i, j);
	}

	public boolean hasArray() {
		return this.buffer.hasArray();
	}

	public byte[] array() {
		return this.buffer.array();
	}

	public int arrayOffset() {
		return this.buffer.arrayOffset();
	}

	public boolean hasMemoryAddress() {
		return this.buffer.hasMemoryAddress();
	}

	public long memoryAddress() {
		return this.buffer.memoryAddress();
	}

	public String toString(Charset charset) {
		return this.buffer.toString(charset);
	}

	public String toString(int i, int j, Charset charset) {
		return this.buffer.toString(i, j, charset);
	}

	public int hashCode() {
		return this.buffer.hashCode();
	}

	public boolean equals(Object object) {
		return this.buffer.equals(object);
	}

	public int compareTo(ByteBuf bytebuf) {
		return this.buffer.compareTo(bytebuf);
	}

	public String toString() {
		return this.buffer.toString();
	}

	public ByteBuf retain(int i) {
		return this.buffer.retain(i);
	}

	public ByteBuf retain() {
		return this.buffer.retain();
	}

	public int refCnt() {
		return this.buffer.refCnt();
	}

	public boolean release() {
		return this.buffer.release();
	}

	public boolean release(int i) {
		return this.buffer.release(i);
	}

}
