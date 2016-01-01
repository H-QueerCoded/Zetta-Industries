package com.bymarcin.zettaindustries.utils;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import net.minecraftforge.common.util.ForgeDirection;

import cofh.api.energy.IEnergyHandler;

import com.bymarcin.zettaindustries.ZettaIndustries;

public class WorldUtils {
	public static final ForgeDirection[] flatDirections = new ForgeDirection[]{ForgeDirection.EAST, ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST};

	public static TileEntity getTileEntity(int dimensionId, int x, int y, int z) {
		World world = ZettaIndustries.proxy.getWorld(dimensionId);
		if (world == null)
			return null;
		return world.getTileEntity(x, y, z);
	}
	
    public static void dropItem(ItemStack item, Random rand, int x, int y, int z, World w) {
        if (item != null && item.stackSize > 0) {
            float rx = rand.nextFloat() * 0.8F + 0.1F;
            float ry = rand.nextFloat() * 0.8F + 0.1F;
            float rz = rand.nextFloat() * 0.8F + 0.1F;
            EntityItem entityItem = new EntityItem(w,
                    x + rx, y + ry, z + rz,
                    new ItemStack(item.getItem(), item.stackSize, item.getItemDamage()));
            if (item.hasTagCompound()) {
                entityItem.getEntityItem().setTagCompound((NBTTagCompound) item.getTagCompound().copy());
            }
            float factor = 0.05F;
            entityItem.motionX = rand.nextGaussian() * factor;
            entityItem.motionY = rand.nextGaussian() * factor + 0.2F;
            entityItem.motionZ = rand.nextGaussian() * factor;
            w.spawnEntityInWorld(entityItem);
            item.stackSize = 0;
        }
    }
    
	public static TileEntity getTileEntityServer(int dimensionId, int x, int y, int z) {
		World world = MinecraftServer.getServer().worldServerForDimension(dimensionId);
		if (world == null)
			return null;
		return world.getTileEntity(x, y, z);
	}

	public static boolean isClientWorld(World paramWorld) {
		return paramWorld.isRemote;
	}

	public static boolean isServerWorld(World paramWorld) {
		return !paramWorld.isRemote;
	}

	public static TileEntity getAdjacentTileEntity(World paramWorld, int paramInt1, int paramInt2, int paramInt3, ForgeDirection paramForgeDirection)
	{
		return paramWorld == null ? null : paramWorld.getTileEntity(paramInt1 + paramForgeDirection.offsetX, paramInt2 + paramForgeDirection.offsetY, paramInt3 + paramForgeDirection.offsetZ);
	}

	public static TileEntity getAdjacentTileEntity(World paramWorld, int paramInt1, int paramInt2, int paramInt3, int paramInt4)
	{
		return paramWorld == null ? null : getAdjacentTileEntity(paramWorld, paramInt1, paramInt2, paramInt3, ForgeDirection.values()[paramInt4]);
	}

	public static TileEntity getAdjacentTileEntity(TileEntity paramTileEntity, ForgeDirection paramForgeDirection)
	{
		return paramTileEntity == null ? null : getAdjacentTileEntity(paramTileEntity.getWorldObj(), paramTileEntity.xCoord, paramTileEntity.yCoord, paramTileEntity.zCoord, paramForgeDirection);
	}

	public static TileEntity getAdjacentTileEntity(TileEntity paramTileEntity, int paramInt)
	{
		return paramTileEntity == null ? null : getAdjacentTileEntity(paramTileEntity.getWorldObj(), paramTileEntity.xCoord, paramTileEntity.yCoord, paramTileEntity.zCoord, ForgeDirection.values()[paramInt]);
	}

	public static boolean isEnergyHandlerFromSide(TileEntity paramTileEntity, ForgeDirection paramForgeDirection)
	{
		return (paramTileEntity instanceof IEnergyHandler) && ((IEnergyHandler) paramTileEntity).canConnectEnergy(paramForgeDirection);
	}

	public static Block getAdjencetBlock(TileEntity tile, ForgeDirection offset) {
		return tile.getWorldObj().getBlock(tile.xCoord + offset.offsetX, tile.yCoord + offset.offsetY, tile.zCoord + offset.offsetZ);
	}
}
