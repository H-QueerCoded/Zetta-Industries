package com.bymarcin.zettaindustries.mods.battery.tileentity;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.FluidRegistry;

import com.bymarcin.zettaindustries.mods.battery.AdvancedStorage;
import com.bymarcin.zettaindustries.mods.battery.Battery;
import com.bymarcin.zettaindustries.mods.battery.erogenousbeef.core.multiblock.IMultiblockPart;
import com.bymarcin.zettaindustries.mods.battery.erogenousbeef.core.multiblock.MultiblockControllerBase;
import com.bymarcin.zettaindustries.mods.battery.erogenousbeef.core.multiblock.MultiblockValidationException;
import com.bymarcin.zettaindustries.mods.battery.erogenousbeef.core.multiblock.rectangular.RectangularMultiblockControllerBase;
import com.bymarcin.zettaindustries.mods.battery.gui.BigBatteryContainer;
import com.bymarcin.zettaindustries.mods.battery.gui.EnergyUpdatePacket;
import com.bymarcin.zettaindustries.registry.ZIRegistry;

public class BatteryController extends RectangularMultiblockControllerBase {
	private Set<TileEntityPowerTap> powerTaps;
	private Set<TileEntityControler> controlers;
	private Set<EntityPlayer> updatePlayers;
	private TileEntityControler controler;
	private short lastUpdate = 0;
	private long lastTickBalance = 0;
	private long tickBalance = 0;
	private long electrolyte = 0;
	private AdvancedStorage storage = new AdvancedStorage(Long.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
	int i = 0;

	public BatteryController(World world) {
		super(world);
		powerTaps = new HashSet<TileEntityPowerTap>();
		controlers = new HashSet<TileEntityControler>();
		updatePlayers = new HashSet<EntityPlayer>();
	}

	public Container getContainer(EntityPlayer player) {
		return new BigBatteryContainer(controler, player);
	}

	public Set<TileEntityPowerTap> getPowerTaps() {
		return powerTaps;
	}

	public void beginUpdatingPlayer(EntityPlayer playerToUpdate) {
		updatePlayers.add(playerToUpdate);
		sendIndividualUpdate(playerToUpdate);
	}

	protected void sendIndividualUpdate(EntityPlayer player) {
		if (this.worldObj.isRemote) {
			return;
		}
		ZIRegistry.packetHandler.sendTo(getUpdatePacket(), (EntityPlayerMP) player);
	}

	protected EnergyUpdatePacket getUpdatePacket() {
		return new EnergyUpdatePacket(controler, storage.getRealEnergyStored(), storage.getRealMaxEnergyStored());
	}

	public void onPacket(long capacity, long storage) {
		electrolyte = capacity;
		getStorage().setCapacity(capacity);
		getStorage().setEnergyStored(storage);
	}

	public void stopUpdatingPlayer(EntityPlayer playerToRemove) {
		updatePlayers.remove(playerToRemove);
	}

	public AdvancedStorage getStorage() {
		return storage;
	}

	@Override
	protected void onMachinePaused() {

	}

	@Override
	protected void onMachineAssembled() {
		for (TileEntityControler c : controlers)
			controler = c;
		recalculate();
	}

	public static boolean isSourceFluid(World worldObj, int x, int y, int z) {
		BlockPos blockPos = new BlockPos(x,y,z);
		Block block = worldObj.getBlockState(blockPos).getBlock();

		if (block instanceof BlockFluidBase || block instanceof BlockFluidClassic || block instanceof BlockStaticLiquid || block instanceof BlockLiquid) {
			if(worldObj.getBlockState(blockPos).getValue(BlockFluidBase.LEVEL)==0) {
				return true;
			}
		}
		return false;
	}

	public static int checkElectrolyte(World worldObj, int x, int y, int z) {
		BlockPos blockPos = new BlockPos(x,y,z);
		Block block = worldObj.getBlockState(blockPos).getBlock();
		if (isSourceFluid(worldObj, x, y, z)) {
			if (Battery.getElectrolyteList().containsKey(FluidRegistry.lookupFluidForBlock(block))) {
				return Battery.getElectrolyteList().get(FluidRegistry.lookupFluidForBlock(block));
			}
		}
		return 0;
	}

	@Override
	protected void onMachineDisassembled() {

	}

	@Override
	protected void onMachineRestored() {
		recalculate();
	}
	
	public void recalculate(){
		electrolyte = 0;
		for (int x = getMinimumCoord().x; x < getMaximumCoord().x; x++) {
			for (int y = getMinimumCoord().y; y < getMaximumCoord().y; y++) {
				for (int z = getMinimumCoord().z; z < getMaximumCoord().z; z++) {
					electrolyte += checkElectrolyte(worldObj, x, y, z);
				}
			}
		}
		storage.setCapacity(electrolyte);
	}

	@Override
	protected void isMachineWhole() throws MultiblockValidationException {
		if (powerTaps.size() == 0) {
			throw new MultiblockValidationException("BigBattery must have power tap");
		}
		if (controlers.size() == 0) {
			throw new MultiblockValidationException("BigBattery must have controler");
		}
		if (controlers.size() > 1) {
			throw new MultiblockValidationException("BigBattery have too many controlers");
		}
		boolean foundElectrolyte = false;
		outer:
		for (int x = getMinimumCoord().x; x < getMaximumCoord().x; x++) {
			for (int y = getMinimumCoord().y; y < getMaximumCoord().y; y++) {
				for (int z = getMinimumCoord().z; z < getMaximumCoord().z; z++) {
					if(checkElectrolyte(worldObj, x, y, z)>0){
						foundElectrolyte = true;
						break outer;
					}
				}
			}
		}
		if (!foundElectrolyte) {
			throw new MultiblockValidationException("BigBattery must have electrolyte");
		}
		super.isMachineWhole();
	}

	@Override
	protected void isBlockGoodForInterior(World world, int x, int y, int z)
			throws MultiblockValidationException {
		BlockPos blockPos = new BlockPos(x,y,z);
		if (world.isAirBlock(blockPos)) {
			return;
		}
		Material material = world.getBlockState(blockPos).getMaterial();
		if (material instanceof MaterialLiquid) {
			return;
		}
		String blockName = world.getBlockState(blockPos).getBlock().getLocalizedName();
		throw new MultiblockValidationException(String.format("%d, %d, %d - Unrecognized block with ID %s, not valid for the reactor's interior", x, y, z, blockName));
	}

	@Override
	protected int getMinimumNumberOfBlocksForAssembledMachine() {
		return 3 * 3 * 4;
	}

	@Override
	protected int getMaximumXSize() {
		return 32;
	}

	@Override
	protected int getMaximumZSize() {
		return 32;
	}

	@Override
	protected int getMaximumYSize() {
		return 32;
	}

	public void modifyLastTickBalance(int energy) {
		tickBalance += energy;
	}

	@Override
	protected boolean updateServer() {
		if (electrolyte == 0)
			return false;
		for (TileEntityPowerTap powerTap : powerTaps) {
			powerTap.onTransferEnergy();
		}
		if (lastUpdate % 4 == 0) {
			EnergyUpdatePacket packet = getUpdatePacket();
			for (EntityPlayer p : updatePlayers) {
				ZIRegistry.packetHandler.sendTo(packet, (EntityPlayerMP) p);
			}
			lastUpdate = 0;
		}
		lastUpdate++;
		lastTickBalance = tickBalance;
		tickBalance = 0;
		return true;
	}

	public long getLastTickBalance() {
		return lastTickBalance;
	}

	@Override
	protected void onBlockAdded(IMultiblockPart newPart) {
		if (newPart instanceof TileEntityPowerTap) {
			powerTaps.add((TileEntityPowerTap) newPart);
		}
		if (newPart instanceof TileEntityControler) {
			controlers.add((TileEntityControler) newPart);
		}
	}

	@Override
	protected void onBlockRemoved(IMultiblockPart oldPart) {
		if (oldPart instanceof TileEntityPowerTap) {
			powerTaps.remove(oldPart);
		}
		if (oldPart instanceof TileEntityControler) {
			controlers.remove(oldPart);
		}
	}

	@Override
	protected void onAssimilate(MultiblockControllerBase assimilated) {
		BatteryController other = (BatteryController)assimilated;
		this.storage.merge(other.getStorage());
	}

	@Override
	protected void onAssimilated(MultiblockControllerBase assimilator) {
	}

	@Override
	protected void updateClient() {
	}

	@Override
	public void writeToNBT(NBTTagCompound data) {
		data.setLong("Electrolyte", electrolyte);
		storage.writeToNBT(data);
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		if (data.hasKey("Electrolyte")) {
			electrolyte = data.getLong("Electrolyte");
		}
		storage.readFromNBT(data);
	}

	@Override
	public void formatDescriptionPacket(NBTTagCompound data) {
		data.setLong("Electrolyte", electrolyte);
		storage.writeToNBT(data);
	}

	@Override
	public void decodeDescriptionPacket(NBTTagCompound data) {
		if (data.hasKey("Electrolyte")) {
			electrolyte = data.getLong("Electrolyte");
		}
		storage.readFromNBT(data);
	}

	@Override
	public void onAttachedPartWithMultiblockData(IMultiblockPart part, NBTTagCompound data) {
		readFromNBT(data);
	}
}
