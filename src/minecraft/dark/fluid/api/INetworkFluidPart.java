package dark.fluid.api;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.IFluidTank;
import dark.core.api.IColorCoded;
import dark.core.api.INetworkPart;

public interface INetworkFluidPart extends IColorCoded, IFluidHandler, INetworkPart
{
	/** Gets the part's main tank for shared storage */
	public IFluidTank getTank();

	/** Sets the content of the part's main tank */
	public void setTankContent(FluidStack stack);
}
