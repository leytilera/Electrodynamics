package edx.electrical.circuit.transformer

import net.minecraftforge.common.util.ForgeDirection
import resonantengine.api.graph.INodeProvider
import resonantengine.lib.grid.energy.electric.NodeElectricComponent

/**
 * Created by robert on 8/11/2014.
 */
class ElectricTransformerNode(parent: INodeProvider) extends NodeElectricComponent(parent: INodeProvider)
{
  var connectionDirection: ForgeDirection = ForgeDirection.NORTH
  var input = true
  var otherNode: ElectricTransformerNode = null
  var step: Int = 2

  def this(parent: INodeProvider, side: ForgeDirection, in: Boolean) =
  {
    this(parent)
    connectionDirection = side
    input = in
  }

  def getVoltage: Double =
  {
    if (!input)
    {
      return otherNode.getVoltage * step
    }
    return 120
  }

  override def canConnect[B <: NodeElectricComponent](obj: B, from: ForgeDirection): Boolean =
  {
    return obj.isInstanceOf[INodeProvider] && from == connectionDirection
  }

  /*
  override def addEnergy(dir: ForgeDirection, wattage: Double, doAdd: Boolean): Double =
  {
    if (input)
    {
      return otherNode.sendEnergy(wattage, doAdd)
    }
    return 0
  }

  def sendEnergy(wattage: Double, doAdd: Boolean): Double =
  {
    val tile: TileEntity = new VectorWorld(parent.asInstanceOf[TileEntity]).add(connectionDirection).getTileEntity
    if (Compatibility.isHandler(tile, connectionDirection.getOpposite))
    {
      return Compatibility.fill(tile, connectionDirection.getOpposite, wattage, doAdd)
    }
    return 0
  }

  override def removeEnergy(dir: ForgeDirection, wattage: Double, doRemove: Boolean): Double =
  {
    return 0
  }*/
}
