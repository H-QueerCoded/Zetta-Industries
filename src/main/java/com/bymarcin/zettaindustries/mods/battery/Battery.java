package com.bymarcin.zettaindustries.mods.battery;

import java.util.HashMap;

import com.bymarcin.zettaindustries.ZettaIndustries;
import com.bymarcin.zettaindustries.basic.InformationalItemBlock;
import com.bymarcin.zettaindustries.modmanager.IMod;
import com.bymarcin.zettaindustries.mods.battery.block.BlockBigBatteryComputerPort;
import com.bymarcin.zettaindustries.mods.battery.block.BlockBigBatteryController;
import com.bymarcin.zettaindustries.mods.battery.block.BlockBigBatteryElectrode;
import com.bymarcin.zettaindustries.mods.battery.block.BlockBigBatteryGlass;
import com.bymarcin.zettaindustries.mods.battery.block.BlockBigBatteryPowerTap;
import com.bymarcin.zettaindustries.mods.battery.block.BlockBigBatteryWall;
import com.bymarcin.zettaindustries.mods.battery.block.BlockGraphite;
import com.bymarcin.zettaindustries.mods.battery.block.BlockSulfur;
import com.bymarcin.zettaindustries.mods.battery.erogenousbeef.core.multiblock.MultiblockClientTickHandler;
import com.bymarcin.zettaindustries.mods.battery.erogenousbeef.core.multiblock.MultiblockEventHandler;
import com.bymarcin.zettaindustries.mods.battery.erogenousbeef.core.multiblock.MultiblockServerTickHandler;
import com.bymarcin.zettaindustries.mods.battery.fluid.AcidFluid;
import com.bymarcin.zettaindustries.mods.battery.fluid.FluidBucket;
import com.bymarcin.zettaindustries.mods.battery.gui.BigBatteryContainer;
import com.bymarcin.zettaindustries.mods.battery.gui.EnergyUpdatePacket;
import com.bymarcin.zettaindustries.mods.battery.gui.GuiControler;
import com.bymarcin.zettaindustries.mods.battery.gui.GuiPowerTap;
import com.bymarcin.zettaindustries.mods.battery.gui.PowerTapUpdatePacket;
import com.bymarcin.zettaindustries.mods.battery.tileentity.BatteryController;
import com.bymarcin.zettaindustries.mods.battery.tileentity.TileEntityComputerPort;
import com.bymarcin.zettaindustries.mods.battery.tileentity.TileEntityControler;
import com.bymarcin.zettaindustries.mods.battery.tileentity.TileEntityElectrode;
import com.bymarcin.zettaindustries.mods.battery.tileentity.TileEntityGlass;
import com.bymarcin.zettaindustries.mods.battery.tileentity.TileEntityPowerTap;
import com.bymarcin.zettaindustries.mods.battery.tileentity.TileEntityWall;
import com.bymarcin.zettaindustries.registry.ZIRegistry;
import com.bymarcin.zettaindustries.registry.gui.IGUI;
import com.bymarcin.zettaindustries.registry.proxy.IProxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class Battery implements IMod, IGUI, IProxy{
	
	public static BlockBigBatteryElectrode  blockBigBatteryElectrode;
	public static BlockBigBatteryGlass  blockBigBatteryGlass;
	public static BlockBigBatteryPowerTap  blockBigBatteryPowerTap;
	public static BlockBigBatteryWall  blockBigBatteryWall;
	public static BlockBigBatteryComputerPort  blockBigBatteryComputerPort;
	public static BlockBigBatteryController  blockBigBatteryControler;
	public static FluidBucket itemAcidBucket;
	public static BlockGraphite blockGraphite;
	public double capacityMultiplier = 1;
	public static int electrodeTransferRate = 2500;
    public static BlockSulfur blockSulfur;
	public static AcidFluid acidFluid;
	static HashMap<Fluid,Integer> electrolyteList = new HashMap<Fluid,Integer>();
	
	public static Fluid acid = new Fluid("sulfurousacid").setLuminosity(0).setDensity(1200).setViscosity(1500).setTemperature(300).setRarity(EnumRarity.uncommon);
	/*Crafting items*/
	
	ItemStack obsidian;
	ItemStack sawDust;
	ItemStack redstone;
	ItemStack rfmeter;
	ItemStack enderFrame;
	ItemStack specialGlass;
	String electrum;
	ItemStack electrumFrame;
	String sulfur;
	ItemStack gunpowder;
	
	@Override
	public void preInit() {
		capacityMultiplier = ZettaIndustries.instance.config.get("BigBattery", "energyMultiplier", 1d).getDouble(1d);
		electrodeTransferRate = ZettaIndustries.instance.config.get("BigBattery", "electrodeTransferRate", 2500).getInt(2500);
	}
	
	@Override
	public void init() {
		
		FluidRegistry.registerFluid(acid);
		acidFluid = new AcidFluid(acid);
		GameRegistry.registerBlock(acidFluid,"sulfurousacid");
		acid.setBlock(acidFluid);
		
		itemAcidBucket = new FluidBucket(acidFluid);
		GameRegistry.registerItem(itemAcidBucket,"acidbucket");
		ZIRegistry.registerBucket(acidFluid, itemAcidBucket);
		
		
		blockSulfur = new BlockSulfur(acidFluid);
		GameRegistry.registerBlock(blockSulfur, InformationalItemBlock.class, "sulfurblock");
		OreDictionary.registerOre("blockSulfur", blockSulfur);
		
		FluidContainerRegistry.registerFluidContainer(
				FluidRegistry.getFluidStack(acid.getName(), FluidContainerRegistry.BUCKET_VOLUME),
				new ItemStack(itemAcidBucket),
				new ItemStack(Items.bucket));

		blockBigBatteryWall = new BlockBigBatteryWall();
		GameRegistry.registerBlock(blockBigBatteryWall, InformationalItemBlock.class, "BatteryWall");
		GameRegistry.registerTileEntity(TileEntityWall.class, "BatteryTileEntityWall");
		
		blockBigBatteryPowerTap = new BlockBigBatteryPowerTap();
		GameRegistry.registerBlock(blockBigBatteryPowerTap, InformationalItemBlock.class, "BatteryPowerTap");
		GameRegistry.registerTileEntity(TileEntityPowerTap.class, "BatteryTileEntityPowerTap");
		
		blockBigBatteryGlass = new BlockBigBatteryGlass();
		GameRegistry.registerBlock(blockBigBatteryGlass, InformationalItemBlock.class, "BatteryGlass");
		GameRegistry.registerTileEntity(TileEntityGlass.class, "BatteryTileEntityGlass");
		
		blockBigBatteryElectrode = new BlockBigBatteryElectrode();
		GameRegistry.registerBlock(blockBigBatteryElectrode, InformationalItemBlock.class, "BatteryElectrode");
		GameRegistry.registerTileEntity(TileEntityElectrode.class, "BatteryTileEntityElectrode");
		
		blockBigBatteryControler = new BlockBigBatteryController();
		GameRegistry.registerBlock(blockBigBatteryControler, InformationalItemBlock.class, "BatteryControler");
		GameRegistry.registerTileEntity(TileEntityControler.class, "BatteryTileEntityControler");
		
		blockBigBatteryComputerPort = new BlockBigBatteryComputerPort();
		GameRegistry.registerBlock(blockBigBatteryComputerPort, InformationalItemBlock.class, "BatteryComputerPort");
		GameRegistry.registerTileEntity(TileEntityComputerPort.class, "BatteryTileEntityComputerPort");
		
		blockGraphite = new BlockGraphite();
		GameRegistry.registerBlock(blockGraphite, "BlockGraphite");
		OreDictionary.registerOre("blockGraphite", blockGraphite);

		
		if(Loader.isModLoaded("ComputerCraft")){
			IntegrationComputerCraft.computercraftInit();
		}
		
		ZIRegistry.registerPacket(1, EnergyUpdatePacket.class, Side.CLIENT);
		ZIRegistry.registerPacket(2, PowerTapUpdatePacket.class, Side.SERVER);
		ZIRegistry.registerPacket(3, PowerTapUpdatePacket.class, Side.CLIENT);
		ZIRegistry.registerGUI(this);
		ZIRegistry.registerProxy(this);
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(new MultiblockEventHandler());
	
	}

    @Override
	public void postInit() {

		registerElectrolyte("redstone", (int)Math.floor(75000000*capacityMultiplier));
		registerElectrolyte("ender", (int)Math.floor(100000000*capacityMultiplier));
		registerElectrolyte("sulfurousacid", (int)Math.floor(150000000*capacityMultiplier));
		
		redstone = new ItemStack(Items.redstone,1);
		obsidian = new ItemStack(Blocks.obsidian,1);
		gunpowder = new ItemStack(Items.gunpowder,1);
		
		electrum = "ingotElectrum";
		sawDust = GameRegistry.findItemStack("ThermalExpansion","dustWoodCompressed",1);
		specialGlass = GameRegistry.findItemStack("ThermalExpansion","frameIlluminator",1);
		
		rfmeter =GameRegistry.findItemStack("ThermalExpansion","multimeter",1);
		enderFrame =GameRegistry.findItemStack("ThermalExpansion","frameTesseractEmpty",1);
		electrumFrame =GameRegistry.findItemStack("ThermalExpansion","frameCellReinforcedEmpty",1);
		
		sulfur = "dustSulfur";
		
		GameRegistry.addSmelting(CharcoalBlockMod.charcoalblock, new ItemStack(blockGraphite), 0F);

		if(electrum != null && sawDust != null && specialGlass != null && rfmeter != null &&
		    enderFrame != null && electrumFrame != null && sulfur!=null && gunpowder!=null){

			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockBigBatteryWall,16), "ODE","OFE","ODE",
					'O',obsidian, 'D', sawDust, 'E', electrum, 'F', enderFrame));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(blockBigBatteryControler, "ODE","MRE","ODE",
					'O', obsidian, 'D', sawDust, 'E', electrum, 'M', rfmeter, 'R', electrumFrame));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockBigBatteryElectrode,2), "WGW","WGW","WWW",
					'W', "blockGraphite", 'G', "blockGold"));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockBigBatteryGlass,4), "GGG","GFG","GGG",
					'G',specialGlass,'F', enderFrame));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(blockBigBatteryPowerTap, "ORO","DFD","EEE",
					'O', obsidian, 'D', sawDust, 'R', redstone, 'E', electrum, 'F', electrumFrame));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(blockBigBatteryComputerPort,"ODE","RMF","ODE",
					'O', obsidian, 'D', sawDust, 'E', electrum, 'M', rfmeter, 'R', redstone, 'F', enderFrame));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockSulfur,1), "SGS","SSS","SSS",
					'S',sulfur, 'G', gunpowder));
			
		}else{
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockBigBatteryWall,4), "OGB","ODB","OGB",
					'O', Blocks.obsidian, 'D', Items.diamond, 'G', Blocks.gold_block, 'B', Items.blaze_powder));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(blockBigBatteryControler, "CLR","XTX","BBB", 'B', Items.blaze_powder,
					'C', Items.comparator, 'L', Blocks.redstone_lamp, 'R', Items.repeater, 'X', blockBigBatteryWall, 'T', Blocks.redstone_block));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockBigBatteryElectrode,2), "WGW","WGW","WWW",
					'W', "blockGraphite", 'G', "blockGold"));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockBigBatteryGlass,1), "GGG","GFG","GGG",
					'G',Blocks.glass,'F', blockBigBatteryWall));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(blockBigBatteryPowerTap, "OCO","RHR","BGB", 'G', blockGraphite,
					'O', Blocks.obsidian, 'C', Items.comparator, 'R', Blocks.redstone_block, 'H', blockBigBatteryWall, 'B', Items.blaze_powder));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(blockBigBatteryComputerPort,"RCR","OHO","BRB",
					'R', Blocks.redstone_block, 'C', Items.comparator, 'O', Blocks.obsidian, 'H', blockBigBatteryWall, 'B', Items.blaze_rod));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockSulfur,1), "SGS","SSS","SSS",
					'S', Items.blaze_powder, 'G', Items.gunpowder));
			
			GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(blockSulfur,1), "SGS","SSS","SSS",
					'S', "dustSulfur", 'G', Items.gunpowder));
		}
	}
	
	private void registerElectrolyte(String name, int valuePerBlock){
		Fluid temp = FluidRegistry.getFluid(name);
		if(temp!=null){
			electrolyteList.put(temp,valuePerBlock);
		}else{
			ZettaIndustries.logger.warn("Try add fluid" + name + "as electrolyte, but fluid not found!");
		}
	}

	@Override
	public Object getServerGuiElement(int id, TileEntity blockEntity,
			EntityPlayer player, World world, int x, int y, int z) {
   	 	if(blockEntity instanceof TileEntityControler){
   	 		return new BigBatteryContainer((TileEntityControler) blockEntity, player);
   	 	}
   	 	
   	 	if(blockEntity instanceof TileEntityPowerTap){
   	 		return ((TileEntityPowerTap) blockEntity).getContainer(player);
   	 	}
   	 	
		return null;
	}

	@Override
	public Object getClientGuiElement(int id, TileEntity blockEntity,
			EntityPlayer player, World world, int x, int y, int z) {
        if(blockEntity instanceof TileEntityControler){
        	return new GuiControler((BatteryController) ((TileEntityControler)blockEntity).getMultiblockController(),
        			((BatteryController) ((TileEntityControler)blockEntity).getMultiblockController()).getContainer(player));
        }
        
        if(blockEntity instanceof TileEntityPowerTap){
        	return new GuiPowerTap(((TileEntityPowerTap) blockEntity).getContainer(player),(TileEntityPowerTap) blockEntity);
        }
        
		return null;
	}
	
	public static HashMap<Fluid, Integer> getElectrolyteList() {
		return electrolyteList;
	}
	
	@EventHandler
    @SideOnly(Side.CLIENT)
    public void textureHook(TextureStitchEvent.Post event)
    {
        if (event.map.getTextureType() == 0)
        {
            acid.setIcons(acidFluid.getBlockTextureFromSide(1), acidFluid.getBlockTextureFromSide(2));
        }
    }

	@Override
	public void clientSide() {
		FMLCommonHandler.instance().bus().register(new MultiblockClientTickHandler());
		FMLCommonHandler.instance().bus().register(new MultiblockServerTickHandler());
	}

	@Override
	public void serverSide() {
		FMLCommonHandler.instance().bus().register(new MultiblockServerTickHandler());
	}


}
