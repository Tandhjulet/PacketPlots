package net.dashmc.plots.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTReadLimiter;
import net.minecraft.server.v1_8_R3.NBTTagCompound;

public class NBTHelper {

	public static void writePayload(DataOutput os, NBTTagCompound tag) throws IOException {
		NBTCompressedStreamTools.a(tag, os);
	}

	public static NBTTagCompound loadPayload(DataInput input, int i, NBTReadLimiter readLimiter) throws IOException {
		return NBTCompressedStreamTools.a(input, readLimiter);
	}
}
