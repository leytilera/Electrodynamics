package dark.assembly.common.armbot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import universalelectricity.core.vector.Vector2;
import universalelectricity.core.vector.Vector3;
import universalelectricity.prefab.TranslationHelper;

import com.google.common.io.ByteArrayDataInput;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.relauncher.Side;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IPeripheral;
import dark.api.al.coding.IArmbot;
import dark.assembly.common.AssemblyLine;
import dark.assembly.common.armbot.command.CommandDrop;
import dark.assembly.common.armbot.command.CommandFire;
import dark.assembly.common.armbot.command.CommandGrab;
import dark.assembly.common.armbot.command.CommandReturn;
import dark.assembly.common.armbot.command.CommandRotateBy;
import dark.assembly.common.armbot.command.CommandRotateTo;
import dark.assembly.common.armbot.command.CommandUse;
import dark.assembly.common.machine.TileEntityAssembly;
import dark.assembly.common.machine.encoder.ItemDisk;
import dark.core.common.DarkMain;
import dark.core.network.PacketHandler;
import dark.core.prefab.IMultiBlock;
import dark.core.prefab.helpers.ItemWorldHelper;
import dark.core.prefab.helpers.MathHelper;
import dark.core.prefab.machine.BlockMulti;

public class TileEntityArmbot extends TileEntityAssembly implements IMultiBlock, IArmbot, IPeripheral
{
    protected List<IComputerAccess> connectedComputers = new ArrayList<IComputerAccess>();
    /** The rotation of the arms. In Degrees. */
    protected float rotationPitch = 0;
    protected float rotationYaw = 0;
    public float actualPitch = 0;
    public float actualYaw = 0;
    protected final float ROTATION_SPEED = 2.0f;

    protected boolean hasTask = false;

    protected String displayText = "";

    /** An entity that the Armbot is grabbed onto. Entity Items are held separately. */
    protected final List<Entity> grabbedEntities = new ArrayList<Entity>();
    protected final List<ItemStack> grabbedItems = new ArrayList<ItemStack>();

    /** Client Side Object Storage */
    public EntityItem renderEntityItem = null;

    public TileEntityArmbot()
    {
        super(.02f);
    }

    @Override
    public void initiate()
    {
        super.initiate();
    }

    @Override
    public void updateEntity()
    {
        super.updateEntity();
        Vector3 handPosition = this.getHandPos();

        for (Entity entity : this.grabbedEntities)
        {
            if (entity != null)
            {
                entity.setPosition(handPosition.x, handPosition.y, handPosition.z);
                entity.motionX = 0;
                entity.motionY = 0;
                entity.motionZ = 0;

                if (entity instanceof EntityItem)
                {
                    ((EntityItem) entity).delayBeforeCanPickup = 20;
                    ((EntityItem) entity).age = 0;
                }
            }
        }

        if (this.isFunctioning())
        {
            if (!this.worldObj.isRemote)
            {
                float preYaw = this.rotationYaw, prePitch = this.rotationPitch;
                this.updateLogic();
                if (this.rotationYaw != preYaw || this.rotationPitch != prePitch)
                {
                    this.sendRotationPacket();
                }
            }
            this.updateRotation();
        }
    }

    public void updateLogic()
    {

    }

    public void updateRotation()
    {
        if (Math.abs(this.actualYaw - this.rotationYaw) > 0.001f)
        {
            float speedYaw;
            if (this.actualYaw > this.rotationYaw)
            {
                if (Math.abs(this.actualYaw - this.rotationYaw) >= 180)
                {
                    speedYaw = this.ROTATION_SPEED;
                }
                else
                {
                    speedYaw = -this.ROTATION_SPEED;
                }
            }
            else
            {
                if (Math.abs(this.actualYaw - this.rotationYaw) >= 180)
                {
                    speedYaw = -this.ROTATION_SPEED;
                }
                else
                {
                    speedYaw = this.ROTATION_SPEED;
                }
            }

            this.actualYaw += speedYaw;

            this.rotationYaw = MathHelper.clampAngleTo360(this.rotationYaw);

            if (this.ticks % 5 == 0 && FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
            {
                // sound is 0.25 seconds long (20 ticks/second)
                this.worldObj.playSound(this.xCoord, this.yCoord, this.zCoord, "mods.assemblyline.conveyor", 0.4f, 1.7f, true);
            }

            if (Math.abs(this.actualYaw - this.rotationYaw) < this.ROTATION_SPEED + 0.1f)
            {
                this.actualYaw = this.rotationYaw;
            }

            for (Entity e : (ArrayList<Entity>) this.worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(this.xCoord, this.yCoord + 2, this.zCoord, this.xCoord + 1, this.yCoord + 3, this.zCoord + 1)))
            {
                e.rotationYaw = this.actualYaw;
            }
        }

        if (Math.abs(this.actualPitch - this.rotationPitch) > 0.001f)
        {
            float speedPitch;
            if (this.actualPitch > this.rotationPitch)
            {
                speedPitch = -this.ROTATION_SPEED;
            }
            else
            {
                speedPitch = this.ROTATION_SPEED;
            }

            this.actualPitch += speedPitch;

            this.rotationPitch = MathHelper.clampAngle(this.rotationPitch, 0, 60);

            if (this.ticks % 4 == 0 && this.worldObj.isRemote)
            {
                this.worldObj.playSound(this.xCoord, this.yCoord, this.zCoord, "mods.assemblyline.conveyor", 2f, 2.5f, true);
            }

            if (Math.abs(this.actualPitch - this.rotationPitch) < this.ROTATION_SPEED + 0.1f)
            {
                this.actualPitch = this.rotationPitch;
            }

            for (Entity e : (ArrayList<Entity>) this.worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(this.xCoord, this.yCoord + 2, this.zCoord, this.xCoord + 1, this.yCoord + 3, this.zCoord + 1)))
            {
                e.rotationPitch = this.actualPitch;
            }
        }

        this.rotationYaw = MathHelper.clampAngleTo360(this.rotationYaw);
        this.rotationPitch = MathHelper.clampAngle(this.rotationPitch, 0, 60);
    }



    @Override
    public String getInvName()
    {
        return TranslationHelper.getLocal("tile.armbot.name");
    }

    public String getCommandDisplayText()
    {
        return this.displayText;
    }

    /** NBT Data */
    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);

        NBTTagCompound diskNBT = nbt.getCompoundTag("disk");
        ItemStack disk = null;
        if (diskNBT != null)
        {
            disk = ItemStack.loadItemStackFromNBT(diskNBT);
        }

        this.rotationYaw = nbt.getFloat("yaw");
        this.rotationPitch = nbt.getFloat("pitch");

        NBTTagList entities = nbt.getTagList("entities");
        this.grabbedEntities.clear();
        for (int i = 0; i < entities.tagCount(); i++)
        {
            NBTTagCompound entityTag = (NBTTagCompound) entities.tagAt(i);
            if (entityTag != null)
            {
                Entity entity = EntityList.createEntityFromNBT(entityTag, worldObj);
                this.grabbedEntities.add(entity);
            }
        }

        NBTTagList items = nbt.getTagList("items");
        this.grabbedItems.clear();
        for (int i = 0; i < items.tagCount(); i++)
        {
            NBTTagCompound itemTag = (NBTTagCompound) items.tagAt(i);
            if (itemTag != null)
            {
                ItemStack item = ItemStack.loadItemStackFromNBT(itemTag);
                this.grabbedItems.add(item);
            }
        }
    }

    /** Writes a tile entity to NBT. */
    @Override
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);

        nbt.setFloat("yaw", this.rotationYaw);
        nbt.setFloat("pitch", this.rotationPitch);

        NBTTagList entities = new NBTTagList();

        for (Entity entity : grabbedEntities)
        {
            if (entity != null)
            {
                NBTTagCompound entityNBT = new NBTTagCompound();
                entity.writeToNBT(entityNBT);
                entity.writeToNBTOptional(entityNBT);
                entities.appendTag(entityNBT);
            }
        }

        nbt.setTag("entities", entities);

        NBTTagList items = new NBTTagList();

        for (ItemStack itemStack : grabbedItems)
        {
            if (itemStack != null)
            {
                NBTTagCompound entityNBT = new NBTTagCompound();
                itemStack.writeToNBT(entityNBT);
                items.appendTag(entityNBT);
            }
        }

        nbt.setTag("items", items);
    }

    @Override
    public Packet getDescriptionPacket()
    {
        return PacketHandler.instance().getPacket(this.getChannel(), this, "armbot", this.functioning, this.rotationYaw, this.rotationPitch);
    }

    public void sendRotationPacket()
    {
        PacketHandler.instance().sendPacketToClients(PacketHandler.instance().getPacket(this.getChannel(), "arbotRotation", this.rotationYaw, this.rotationPitch), worldObj, new Vector3(this).translate(new Vector3(.5f, 1f, .5f)), 40);
    }

    @Override
    public boolean simplePacket(String id, ByteArrayDataInput dis, Player player)
    {
        try
        {
            if (this.worldObj.isRemote && !super.simplePacket(id, dis, player))
            {
                if (id.equalsIgnoreCase("armbot"))
                {
                    this.functioning = dis.readBoolean();
                    this.rotationYaw = dis.readFloat();
                    this.rotationPitch = dis.readFloat();
                    return true;
                }
                else if (id.equalsIgnoreCase("arbotRotation"))
                {
                    this.rotationYaw = dis.readFloat();
                    this.rotationPitch = dis.readFloat();
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onActivated(EntityPlayer player)
    {
        ItemStack containingStack = this.getStackInSlot(0);

        if (containingStack != null)
        {
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER)
            {
                EntityItem dropStack = new EntityItem(this.worldObj, player.posX, player.posY, player.posZ, containingStack);
                dropStack.delayBeforeCanPickup = 0;
                this.worldObj.spawnEntityInWorld(dropStack);
            }

            this.setInventorySlotContents(0, null);
            return true;
        }
        else
        {
            if (player.getCurrentEquippedItem() != null)
            {
                if (player.getCurrentEquippedItem().getItem() instanceof ItemDisk)
                {
                    this.setInventorySlotContents(0, player.getCurrentEquippedItem());
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onCreate(Vector3 placedPosition)
    {
        if (DarkMain.blockMulti instanceof BlockMulti)
        {
            DarkMain.blockMulti.makeFakeBlock(this.worldObj, Vector3.translate(placedPosition, new Vector3(0, 1, 0)), placedPosition);
        }
    }

    @Override
    public void onDestroy(TileEntity callingBlock)
    {
        this.worldObj.setBlock(this.xCoord, this.yCoord, this.zCoord, 0, 0, 3);
        this.worldObj.setBlock(this.xCoord, this.yCoord + 1, this.zCoord, 0, 0, 3);
    }

    @Override
    public String getType()
    {
        return "ArmBot";
    }

    @Override
    public String[] getMethodNames()
    {
        return new String[] { "rotateBy", "rotateTo", "grab", "drop", "reset", "isWorking", "touchingEntity", "use", "fire", "return", "clear", "isHolding" };
    }

    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) throws Exception
    {
        return null;
    }

    @Override
    public boolean canAttachToSide(int side)
    {
        return side != ForgeDirection.UP.ordinal();
    }

    @Override
    public void attach(IComputerAccess computer)
    {
        synchronized (connectedComputers)
        {
            connectedComputers.add(computer);
        }
    }

    @Override
    public void detach(IComputerAccess computer)
    {
        synchronized (connectedComputers)
        {
            connectedComputers.remove(computer);
        }
    }

    @Override
    public List<Object> getGrabbedObjects()
    {
        List<Object> list = new ArrayList<Object>();
        list.addAll(this.grabbedEntities);
        list.addAll(this.grabbedItems);
        return list;
    }

    @Override
    public void grab(Object entity)
    {
        if (entity instanceof EntityItem)
        {
            this.grabbedItems.add(((EntityItem) entity).getEntityItem());
            ((EntityItem) entity).setDead();
        }
        else if (entity instanceof Entity)
        {
            this.grabbedEntities.add((Entity) entity);
        }
    }

    @Override
    public void drop(Object object)
    {
        if (object instanceof Entity)
        {
            this.grabbedEntities.remove(object);
        }
        if (object instanceof ItemStack)
        {
            Vector3 handPosition = this.getHandPos();
            ItemWorldHelper.dropItemStack(worldObj, handPosition, (ItemStack) object, false);
            this.grabbedItems.remove(object);
        }
        if (object instanceof String)
        {
            String string = ((String) object).toLowerCase();
            if (string.equalsIgnoreCase("all"))
            {
                Vector3 handPosition = this.getHandPos();
                Iterator<ItemStack> it = this.grabbedItems.iterator();

                while (it.hasNext())
                {
                    ItemWorldHelper.dropItemStack(worldObj, handPosition, it.next(), false);
                }

                this.grabbedEntities.clear();
                this.grabbedItems.clear();
            }
        }
    }

    @Override
    public boolean canConnect(ForgeDirection direction)
    {
        return direction == ForgeDirection.DOWN;
    }

    @Override
    public boolean isInvNameLocalized()
    {
        return false;
    }

    @Override
    public double getWattLoad()
    {
        if (this.hasTask)
        {
            return .4;//400w
        }
        return .03;//30w
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack)
    {
        return itemstack != null && itemstack.itemID == AssemblyLine.recipeLoader.itemDisk.itemID;
    }

    @Override
    public Vector3 getHandPos()
    {
        Vector3 position = new Vector3(this);
        position.translate(0.5);
        position.translate(this.getDeltaHandPosition());
        return position;
    }

    public Vector3 getDeltaHandPosition()
    {
        // The distance of the position relative to the main position.
        double distance = 1f;
        Vector3 delta = new Vector3();
        // The delta Y of the hand.
        delta.y = Math.sin(Math.toRadians(this.actualPitch)) * distance * 2;
        // The horizontal delta of the hand.
        double dH = Math.cos(Math.toRadians(this.actualPitch)) * distance;
        // The delta X and Z.
        delta.x = Math.sin(Math.toRadians(-this.actualYaw)) * dH;
        delta.z = Math.cos(Math.toRadians(-this.actualYaw)) * dH;
        return delta;
    }

    @Override
    public Vector2 getRotation()
    {
        return new Vector2(this.actualYaw, this.actualPitch);
    }

    @Override
    public void setRotation(float yaw, float pitch)
    {
        if (!this.worldObj.isRemote)
        {
            this.actualYaw = yaw;
            this.actualPitch = pitch;
        }

    }

    @Override
    public void moveArmTo(float yaw, float pitch)
    {
        if (!this.worldObj.isRemote)
        {
            this.rotationYaw = yaw;
            this.rotationPitch = pitch;
        }
    }

    @Override
    public void moveTo(ForgeDirection direction)
    {
        if (direction == ForgeDirection.SOUTH)
        {
            this.rotationYaw = 0;
        }
        else if (direction == ForgeDirection.EAST)
        {
            this.rotationYaw = 90;
        }
        else if (direction == ForgeDirection.NORTH)
        {

            this.rotationYaw = 180;
        }
        else if (direction == ForgeDirection.WEST)
        {
            this.rotationYaw = 270;
        }
    }
}
