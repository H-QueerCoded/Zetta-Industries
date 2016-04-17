package com.bymarcin.zettaindustries.mods.nfc.smartcard;

import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.EnumSet;

import javax.crypto.KeyAgreement;

import com.bymarcin.zettaindustries.mods.nfc.NFC;
import com.bymarcin.zettaindustries.mods.nfc.smartcard.SmartCardTerminal.KeyType;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.TileEntityEnvironment;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;


public class SmartCardTerminalTileEntity extends TileEntityEnvironment implements Analyzable {
	ItemStack card = null;
	String player;
	boolean needsUpdate = true;

	NBTTagCompound renderInfo = null;
	
	public SmartCardTerminalTileEntity() {
		node = Network.newNode(this, Visibility.Network).withConnector().withComponent("smartcard_terminal", Visibility.Network).create();
	}

	public boolean onBlockActivated(EntityPlayer player) {
		if (card == null && player.getHeldItem() != null && player.getHeldItem().getItem() instanceof SmartCardItem) {
			card = ItemStack.copyItemStack(player.getHeldItem());
			player.getHeldItem().stackSize = 0;
			this.player = player.getCommandSenderName();
			if(node!=null)
				node.sendToReachable("computer.signal","smartcard_in",player.getCommandSenderName());
			needsUpdate = true;
		}

		if (card != null && player.getHeldItem() == null) {
			player.inventory.setInventorySlotContents(player.inventory.currentItem, card);
			this.player = null;
			card = null;
			if(node!=null)
				node.sendToReachable("computer.signal","smartcard_out",player.getCommandSenderName());
			needsUpdate = true;
		}
		return true;
	}
	
	@Override
	public Packet getDescriptionPacket() {
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 666, getData());
	}
	
	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		renderInfo = pkt.func_148857_g();
	}
	
	@Override
	public void updateEntity() {
		super.updateEntity();
		if(needsUpdate){
			needsUpdate = false;
			markDirty();
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		if (player != null) {
			nbt.setString("PLAYER", player);
		}
		if (card != null) {
			NBTTagCompound tag = new NBTTagCompound();
			card.writeToNBT(tag);
			nbt.setTag("CARD", tag);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		if (nbt.hasKey("PLAYER")) {
			player = nbt.getString("PLAYER");
		}

		if (nbt.hasKey("CARD")) {
			card = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("CARD"));
		}
	}

	protected void checkCost(Double baseCost) throws Exception {
		if (!((Connector) node).tryChangeBuffer(-baseCost))
			throw new Exception("not enough energy");
	}
	
	protected byte[] checkCost(Context context, Arguments args, double baseCost, double byteCost) throws Exception {
		byte[] data = args.checkByteArray(0);
		if (data.length > NFC.dataCardHardLimit)
			throw new IllegalArgumentException("data size limit exceeded");
		double cost = baseCost + data.length * byteCost;
		if (!((Connector) node).tryChangeBuffer(-cost))
			throw new Exception("not enough energy");
		if (data.length > NFC.dataCardSoftLimit)
			context.pause(NFC.dataCardTimeout);
		return data;
	}

	protected byte[] asymmetricCost(Context context, Arguments args) throws Exception {
		return checkCost(context, args, NFC.dataCardAsymmetric, NFC.dataCardComplexByte);
	}

	@Callback(direct = true)
	public Object[] hasCard(Context ctx, Arguments args) {
		return card == null ? new Object[] { false } : new Object[] { true };
	}
	
	@Callback(direct = true, limit = 1, doc = "function(pub:userdata):string -- Generates a shared key. ecdh(a.priv, b.pub) == ecdh(b.priv, a.pub)")
	public Object[] ecdh(Context context, Arguments args) throws Exception {
		if (card == null)
			return new Object[] { null, "Card expected" };
		if (SmartCardItem.getNBT(card).hasKey(SmartCardItem.OWNER) && !SmartCardItem.getOwner(card).equals(player)) {
			return new Object[] { null, "You are not owner" };
		}
		
		checkCost(NFC.dataCardAsymmetric);
		Key pubKey = deserialize(KeyType.PUBLIC, args.checkByteArray(0));
		Key privKey = deserialize(KeyType.PRIVATE, SmartCardItem.getPrivateKey(card));
		KeyAgreement ka = KeyAgreement.getInstance("ECDH");
		ka.init(privKey);
		ka.doPhase(pubKey, true);
		return new Object[] { ka.generateSecret() };
	}

	@Callback(direct = true)
	public Object[] protect(Context ctx, Arguments args) {
		if (card != null && !SmartCardItem.getNBT(card).hasKey(SmartCardItem.OWNER)) {
			SmartCardItem.getNBT(card).setString(SmartCardItem.OWNER, player);
			needsUpdate = true;
			return new Object[] { true, player};
		}
		return new Object[] { false };
	}

	@Callback(direct = true, limit = 1, doc = "function(data:string [, sig:string]):string or bool -- Signs or verifies data.")
	public Object[] ecdsa(Context context, Arguments args) throws Exception {
		if (card == null)
			return new Object[] { null, "Card expected" };
		if (SmartCardItem.getNBT(card).hasKey(SmartCardItem.OWNER) && !SmartCardItem.getOwner(card).equals(player)) {
			return new Object[] { null, "You are not owner" };
		}

		byte[] data = asymmetricCost(context, args);
		byte[] sig = args.optByteArray(1, null);
		Signature sign = Signature.getInstance("SHA256withECDSA");
		if (sig != null) {
			// Verify mode
			byte[] key = SmartCardItem.getPublicKey(card);
			sign.initVerify((PublicKey) deserialize(KeyType.PUBLIC, key));
			sign.update(data);
			return new Object[] { sign.verify(sig) };
		} else {
			// Sign mode
			byte[] key = SmartCardItem.getPrivateKey(card);
			sign.initSign((PrivateKey) deserialize(KeyType.PRIVATE, key));
			sign.update(data);
			return new Object[] { sign.sign() };
		}
	}

	@Callback(direct = true, doc = "function():string")
	public Object[] getPublicKey(Context context, Arguments args) throws Exception {
		if (card == null)
			return new Object[] { null, "Card expected" };
		return new Object[] { SmartCardItem.getPublicKey(card) };
	}

	private Key deserialize(KeyType type, byte[] data) throws InvalidKeySpecException, NoSuchAlgorithmException {
		switch (type) {
		case PRIVATE:
			return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(data));
		case PUBLIC:
			return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(data));
		default:
			throw new IllegalArgumentException("invalid key type, must be public or private");
		}
	}

	enum KeyType {
		PRIVATE, PUBLIC;
	}

	public NBTTagCompound getData() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setBoolean("hasCard", card != null);
		if ((card != null && SmartCardItem.getOwner(card).isEmpty()) || (card != null && player != null && player.equals(SmartCardItem.getOwner(card)))) {
			nbt.setBoolean("validOwner", true);
			nbt.setBoolean("isProtected", !SmartCardItem.getOwner(card).isEmpty());
		} else {
			nbt.setBoolean("validOwner", false);
		}
		return nbt;
	}

	@Override
	public Node[] onAnalyze(EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
		return new Node[] { node };
	}

}
