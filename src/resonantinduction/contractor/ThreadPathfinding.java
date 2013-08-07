/**
 * 
 */
package resonantinduction.contractor;

import universalelectricity.core.vector.Vector3;

/**
 * @author Calclavia
 * 
 */
public class ThreadPathfinding extends Thread
{
	private boolean isCompleted = false;
	private PathfinderEMContractor pathfinder;
	private Vector3 start;

	public ThreadPathfinding(PathfinderEMContractor pathfinder, Vector3 start)
	{
		this.pathfinder = pathfinder;
		this.start = start;
		this.setPriority(Thread.MIN_PRIORITY);
	}

	@Override
	public void run()
	{
		this.pathfinder.find(this.start);
		this.isCompleted = true;
	}

	public PathfinderEMContractor getPath()
	{
		if (this.isCompleted)
		{
			return this.pathfinder;
		}

		return null;
	}
}
