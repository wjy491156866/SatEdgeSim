package examples;

import edu.weijunyong.satedgesim.MainApplication;

public class Example3 extends MainApplication {
	/**
	 * This is a simple example showing how to launch simulation using a custom Edge
	 * device/ datacenter class . by removing it, SatEdgeSim will use the default
	 * one. As you can see, this class extends the Main class provided by
	 * SatEdgeSim, which is required for this example to work
	 */
	public Example3(int fromIteration, int step_) {
		super(fromIteration, step_);
	}

	public static void main(String[] args) {
		/*
		 * To use your custom Edge datacenters/ devices class, do this: The custom edge
		 * data center class can be found in the examples folder as well. by removing
		 * this line, SatEdgeSim will use the default datacenters/devices class.
		 */
		setCustomEdgeDataCenters(CustomDataCenter.class);

		// To use the SatEdgeSim default edge data centers class you can also uncomment
		// this:
		// setCustomEdgeDataCenters(DefaultEdgeDataCenter.class);

		// Start the simulation
		launchSimulation();
	}

}
