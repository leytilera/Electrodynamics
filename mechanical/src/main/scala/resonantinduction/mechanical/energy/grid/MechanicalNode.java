package resonantinduction.mechanical.energy.grid;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import resonant.api.grid.INodeProvider;
import resonant.lib.utility.nbt.ISaveObj;
import resonantinduction.core.interfaces.IMechanicalNode;
import universalelectricity.api.vector.Vector3;
import codechicken.multipart.TMultiPart;

/** A mechanical node for mechanical energy.
 * 
 * @author Calclavia */
public class MechanicalNode implements IMechanicalNode, ISaveObj
{
    public boolean doDebug = false;
    public double torque = 0;
    public double prevAngularVelocity, angularVelocity = 0;
    public float acceleration = 2f;

    /** The current rotation of the mechanical node. */
    public double renderAngle = 0;
    public double prev_angle = 0;
    /** Limits the max distance an object can rotate in a single update */
    protected double maxDeltaAngle = Math.toRadians(180);

    protected double load = 2;
    protected byte connectionMap = Byte.parseByte("111111", 2);

    private double power = 0;
    private INodeProvider parent;

    protected final AbstractMap<MechanicalNode, ForgeDirection> connections = new WeakHashMap<MechanicalNode, ForgeDirection>();

    public MechanicalNode(INodeProvider parent)
    {
        this.setParent(parent);
    }

    @Override
    public MechanicalNode setLoad(double load)
    {
        this.load = load;
        return this;
    }

    public MechanicalNode setConnection(byte connectionMap)
    {
        this.connectionMap = connectionMap;
        return this;
    }

    @Override
    public double getRadius()
    {
        return 0.5;
    }

    public void debug(String txt)
    {
        if (doDebug)
            System.out.println("[MechMode]" + txt);
    }

    @Override
    public void update(float deltaTime)
    {
        prevAngularVelocity = angularVelocity;
        if (world() != null && !world().isRemote)
            debug("Node :" + toString());
        //Update 
        if (world() != null && !world().isRemote)
            debug("AngleBefore: " + renderAngle + "  Vel: " + angularVelocity);
        if (angularVelocity >= 0)
        {
            renderAngle += Math.min(angularVelocity, this.maxDeltaAngle) * deltaTime;
        }
        else
        {
            renderAngle += Math.max(angularVelocity, -this.maxDeltaAngle) * deltaTime;
        }
        if (world() != null && !world().isRemote)
            debug("AngleAfter: " + renderAngle + "  Vel: " + angularVelocity);

        if (renderAngle % (Math.PI * 2) != renderAngle)
        {
            revolve();
            renderAngle = renderAngle % (Math.PI * 2);
        }

        if (world() != null && !world().isRemote)
        {
            final double acceleration = this.acceleration * deltaTime;

            /** Energy loss */
            double torqueLoss = Math.min(Math.abs(getTorque()), (Math.abs(getTorque() * getTorqueLoad()) + getTorqueLoad() / 10) * deltaTime);

            if (torque > 0)
            {
                torque -= torqueLoss;
            }
            else
            {
                torque += torqueLoss;
            }

            double velocityLoss = Math.min(Math.abs(getAngularSpeed()), (Math.abs(getAngularSpeed() * getAngularVelocityLoad()) + getAngularVelocityLoad() / 10) * deltaTime);

            if (angularVelocity > 0)
            {
                angularVelocity -= velocityLoss;
            }
            else
            {
                angularVelocity += velocityLoss;
            }

            if (getEnergy() <= 0)
            {
                angularVelocity = torque = 0;
            }

            power = getEnergy() / deltaTime;

            synchronized (connections)
            {
                Iterator<Entry<MechanicalNode, ForgeDirection>> it = connections.entrySet().iterator();

                while (it.hasNext())
                {
                    Entry<MechanicalNode, ForgeDirection> entry = it.next();

                    ForgeDirection dir = entry.getValue();
                    MechanicalNode adjacentMech = entry.getKey();
                    debug("Connection: " + adjacentMech + "  Side: " + dir);
                    /** Calculate angular velocity and torque. */
                    float ratio = adjacentMech.getRatio(dir.getOpposite(), this) / getRatio(dir, adjacentMech);
                    boolean inverseRotation = inverseRotation(dir, adjacentMech) && adjacentMech.inverseRotation(dir.getOpposite(), this);

                    int inversion = inverseRotation ? -1 : 1;

                    double targetTorque = inversion * adjacentMech.getTorque() / ratio;
                    double applyTorque = targetTorque * acceleration;

                    if (Math.abs(torque + applyTorque) < Math.abs(targetTorque))
                    {
                        torque += applyTorque;
                    }
                    else if (Math.abs(torque - applyTorque) > Math.abs(targetTorque))
                    {
                        torque -= applyTorque;
                    }

                    double targetVelocity = inversion * adjacentMech.getAngularSpeed() * ratio;
                    double applyVelocity = targetVelocity * acceleration;

                    if (Math.abs(angularVelocity + applyVelocity) < Math.abs(targetVelocity))
                    {
                        angularVelocity += applyVelocity;
                    }
                    else if (Math.abs(angularVelocity - applyVelocity) > Math.abs(targetVelocity))
                    {
                        angularVelocity -= applyVelocity;
                    }

                    /** Set all current rotations */
                    // adjacentMech.angle = Math.abs(angle) * (adjacentMech.angle >= 0 ? 1 : -1);
                }
            }
        }

        onUpdate();
        prev_angle = renderAngle;
    }

    protected void onUpdate()
    {

    }

    /** Called when one revolution is made. */
    protected void revolve()
    {

    }

    @Override
    public void apply(Object source, double torque, double angularVelocity)
    {
        this.torque += torque;
        this.angularVelocity += angularVelocity;
    }

    @Override
    public double getTorque()
    {
        return angularVelocity != 0 ? torque : 0;
    }

    @Override
    public double getAngularSpeed()
    {
        return torque != 0 ? angularVelocity : 0;
    }

    @Override
    public float getRatio(ForgeDirection dir, IMechanicalNode with)
    {
        return 0.5f;
    }

    @Override
    public boolean inverseRotation(ForgeDirection dir, IMechanicalNode with)
    {
        return true;
    }

    /** The energy percentage loss due to resistance in seconds. */
    public double getTorqueLoad()
    {
        return load;
    }

    public double getAngularVelocityLoad()
    {
        return load;
    }

    public World world()
    {
        return getParent() instanceof TMultiPart ? ((TMultiPart) getParent()).world() : getParent() instanceof TileEntity ? ((TileEntity) getParent()).getWorldObj() : null;
    }

    public Vector3 position()
    {
        return getParent() instanceof TMultiPart ? new Vector3(((TMultiPart) getParent()).x(), ((TMultiPart) getParent()).y(), ((TMultiPart) getParent()).z()) : getParent() instanceof TileEntity ? new Vector3((TileEntity) getParent()) : null;
    }

    public boolean canConnect(ForgeDirection from, Object source)
    {
        return (source instanceof MechanicalNode) && (connectionMap & (1 << from.ordinal())) != 0;
    }

    @Override
    public double getEnergy()
    {
        return getTorque() * getAngularSpeed();
    }

    @Override
    public double getPower()
    {
        return power;
    }

    @Override
    public void load(NBTTagCompound nbt)
    {
        torque = nbt.getDouble("torque");
        angularVelocity = nbt.getDouble("angularVelocity");
    }

    @Override
    public void save(NBTTagCompound nbt)
    {
        nbt.setDouble("torque", torque);
        nbt.setDouble("angularVelocity", angularVelocity);
    }

    @Override
    public void reconstruct()
    {
        recache();
    }

    @Override
    public void deconstruct()
    {
        for (Entry<MechanicalNode, ForgeDirection> entry : connections.entrySet())
        {
            entry.getKey().recache();
        }
        connections.clear();
    }

    @Override
    public void recache()
    {
        connections.clear();

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
        {
            TileEntity tile = position().translate(dir).getTileEntity(world());

            if (tile instanceof INodeProvider)
            {
                MechanicalNode check = (MechanicalNode) ((INodeProvider) tile).getNode(MechanicalNode.class, dir.getOpposite());

                if (check != null && canConnect(dir, check) && check.canConnect(dir.getOpposite(), this))
                {
                    connections.put(check, dir);
                }
            }
        }
    }

    public INodeProvider getParent()
    {
        return parent;
    }

    public void setParent(INodeProvider parent)
    {
        this.parent = parent;
    }

}
