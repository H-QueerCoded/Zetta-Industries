package com.bymarcin.zettaindustries.mods.battery.tileentity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.bymarcin.zettaindustries.mods.battery.Battery;
import com.bymarcin.zettaindustries.mods.battery.erogenousbeef.core.multiblock.MultiblockControllerBase;
import com.bymarcin.zettaindustries.mods.battery.erogenousbeef.core.multiblock.MultiblockValidationException;
import com.bymarcin.zettaindustries.mods.battery.gui.PowerTapContener;
import com.bymarcin.zettaindustries.mods.battery.gui.PowerTapUpdatePacket;
import com.bymarcin.zettaindustries.registry.ZIRegistry;
import com.bymarcin.zettaindustries.utils.WorldUtils;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import net.minecraftforge.common.util.ForgeDirection;

import cofh.api.energy.IEnergyHandler;


public class TileEntityPowerTap extends BasicRectangularMultiblockTileEntityBase implements IEnergyHandler{
	int transferMax = 0;
	int transferCurrent = 0;
	private Set<EntityPlayer> updatePlayers = new HashSet<EntityPlayer>();
	String label = "";
	
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public void beginUpdatingPlayer(EntityPlayer playerToUpdate) {
		updatePlayers.add(playerToUpdate);
		sendIndividualUpdate(playerToUpdate);
	}	

	protected void sendIndividualUpdate(EntityPlayer player) {
		if(this.worldObj.isRemote) { return; }
		ZIRegistry.packetHandler.sendTo(getUpdatePacket(), (EntityPlayerMP) player);
	}
	
	protected PowerTapUpdatePacket getUpdatePacket(){
	     return new PowerTapUpdatePacket(this,transferCurrent,true,getLabel());
	}
	
	public void stopUpdatingPlayer(EntityPlayer playerToRemove) {
		updatePlayers.remove(playerToRemove);
	}
	
	public int getTransferCurrent(){
		return transferCurrent;
	}
	
	@Override
	public void isGoodForFrame() throws MultiblockValidationException {
		throw new MultiblockValidationException(String.format("%d, %d, %d - Power tap may not be placed in the battery's frame", this.xCoord, this.yCoord, this.zCoord));
	}

	@Override
	public void isGoodForSides() throws MultiblockValidationException {
		throw new MultiblockValidationException(String.format("%d, %d, %d - Power tap may not be placed in the battery's sides", this.xCoord, this.yCoord, this.zCoord));
	}

	@Override
	public void isGoodForTop() throws MultiblockValidationException {
		TileEntity entityBelow = this.worldObj.getTileEntity(this.xCoord,  this.yCoord - 1, this.zCoord);
		if ((entityBelow instanceof TileEntityElectrode)) {
			return;
		}
		throw new MultiblockValidationException(String.format("%d, %d, %d - Power tap must be placed on electrode", this.xCoord, this.yCoord, this.zCoord));
	}

	@Override
	public void isGoodForBottom() throws MultiblockValidationException {
		throw new MultiblockValidationException(String.format("%d, %d, %d - Power tap may not be placed in the battery's bottom", this.xCoord, this.yCoord, this.zCoord));
	}

	@Override
	public void isGoodForInterior() throws MultiblockValidationException {
		throw new MultiblockValidationException(String.format("%d, %d, %d - Power tap may not be placed in the battery's interior", this.xCoord, this.yCoord, this.zCoord));
	}

	private static ForgeDirection[] dirs;

	{
		dirs = Arrays.copyOf(WorldUtils.flatDirections, 5);
		dirs[4] = ForgeDirection.DOWN;
	}

	@Override
	public void onMachineAssembled(MultiblockControllerBase controller) {
		super.onMachineAssembled(controller);
		transferMax = 0;
		int i = 1;
		Block b;
		while (true) {
			b = worldObj.getBlock(this.xCoord, this.yCoord - i, this.zCoord);
			if (b != Battery.blockBigBatteryElectrode) {
				break;
			}

			for (ForgeDirection d : dirs) {
				if (BatteryController.checkElectrolyte(worldObj, xCoord + d.offsetX, yCoord + d.offsetY - i, zCoord + d.offsetZ) != 0) {
					transferMax += Battery.electrodeTransferRate;
				}
			}
			i++;
		}

		if(transferCurrent > transferMax)
			transferCurrent=transferMax;
	}

	@Override
	public void onMachineActivated() {
	}

	@Override
	public void onMachineDeactivated() {
	}


	
	public int onTransferEnergy(){
			if(WorldUtils.isClientWorld(worldObj) || isOutput() || getMultiblockController()==null) return 0;
			TileEntity tile = WorldUtils.getAdjacentTileEntity(this, ForgeDirection.UP);
			int energyGet=0;
			if (WorldUtils.isEnergyHandlerFromSide(tile,ForgeDirection.VALID_DIRECTIONS[(1 ^ 0x1)])){
				energyGet = ((IEnergyHandler)tile).receiveEnergy(ForgeDirection.VALID_DIRECTIONS[(1 ^ 0x1)], Math.min(transferCurrent, ((BatteryController)getMultiblockController()).getStorage().getEnergyStored()), false); 
			}  
			((BatteryController)getMultiblockController()).getStorage().modifyEnergyStored(-energyGet);
			return energyGet;
	}

	public void setTransfer(int transfer){
		if(worldObj.isRemote){
			transferCurrent = transfer;
		}else{
			transferCurrent = Math.max(0,Math.min(transfer, transferMax));	
		}
	}
	
	public void updatePowerTap(){
		for(EntityPlayer p: updatePlayers)
			ZIRegistry.packetHandler.sendTo(getUpdatePacket(), (EntityPlayerMP) p);
	}
	
	public Container getContainer(EntityPlayer player){
		return new PowerTapContener(this, player);
	}
	
	/* IEnergyHandler */
	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
		if(getMultiblockController()!=null && isOutput() && getMultiblockController().isAssembled()){
			 int temp =((BatteryController)getMultiblockController()).getStorage().receiveEnergy(Math.min(maxReceive,transferCurrent), simulate);
			    if(!simulate){((BatteryController)getMultiblockController()).modifyLastTickBalance(temp);}
			 return temp;
		}
		return 0;
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
		if(getMultiblockController()!=null && !isOutput() && getMultiblockController().isAssembled()){
			int temp = ((BatteryController)getMultiblockController()).getStorage().extractEnergy(Math.min(maxExtract,transferCurrent), simulate);
            if(!simulate){((BatteryController)getMultiblockController()).modifyLastTickBalance(-temp);}
			return temp;
		}
		return 0;
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {
		if(getMultiblockController()!=null){
			return ((BatteryController)getMultiblockController()).getStorage().getEnergyStored();
		}
		return 0;
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		if(getMultiblockController()!=null){
			return ((BatteryController)getMultiblockController()).getStorage().getMaxEnergyStored();
		}
		return 0;
	}
	
	public boolean isOutput() {
		return worldObj.getBlockMetadata(xCoord, yCoord, zCoord)==0;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		transferCurrent = data.getInteger("transfer");
		if(data.hasKey("powertap_label")){
			label = data.getString("powertap_label");
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound data) {
		super.writeToNBT(data);
		data.setInteger("transfer",transferCurrent);
		data.setString("powertap_label", label);
	}
	
	public void setIn(){
		worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 2);
	}
	
	public void setOut(){
		worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 2);
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }
}
