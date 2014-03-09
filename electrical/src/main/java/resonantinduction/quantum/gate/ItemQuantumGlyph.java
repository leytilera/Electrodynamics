package resonantinduction.quantum.gate;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import resonantinduction.core.prefab.part.IHighlight;
import resonantinduction.mechanical.energy.gear.PartGearShaft;
import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.CornerPlacementGrid$;
import codechicken.multipart.JItemMultiPart;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.PartMap;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

public class ItemQuantumGlyph extends JItemMultiPart implements IHighlight
{
	public ItemQuantumGlyph(int id)
	{
		super(id);
		setHasSubtypes(true);
	}

	@Override
	public String getUnlocalizedName(ItemStack itemStack)
	{
		return super.getUnlocalizedName(itemStack) + "." + itemStack.getItemDamage();
	}

	@Override
	public TMultiPart newPart(ItemStack itemStack, EntityPlayer player, World world, BlockCoord pos, int slot, Vector3 hit)
	{
		PartQuantumGlyph part = (PartQuantumGlyph) MultiPartRegistry.createPart("resonant_induction_quantum_glyph", false);
		slot = CornerPlacementGrid$.MODULE$.getHitSlot(hit, slot);
		System.out.println(slot);
		switch (slot)
		{
			case 7:
				slot = 0;
				break;
			case 9:
				slot = 1;
				break;
			case 11:
				slot = 2;
				break;
			case 13:
				slot = 3;
				break;

			case 8:
				slot = 4;
				break;
			case 10:
				slot = 5;
				break;
			case 12:
				slot = 6;
				break;
			case 14:
				slot = 7;
				break;
		}

		TileEntity tile = world.getBlockTileEntity(pos.x, pos.y, pos.z);

		if (tile instanceof TileMultipart)
		{

		}

		part.preparePlacement(slot, itemStack.getItemDamage());
		return part;
	}

	@Override
	public void getSubItems(int itemID, CreativeTabs tab, List listToAddTo)
	{
		for (int i = 0; i < 4; i++)
		{
			listToAddTo.add(new ItemStack(itemID, 1, i));
		}
	}

	@Override
	public int getHighlightType()
	{
		return 1;
	}
}
