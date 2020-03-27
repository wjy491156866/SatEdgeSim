package examples;

import edu.weijunyong.satedgesim.MainApplication;
import edu.weijunyong.satedgesim.LocationManager.DefaultMobilityModel; 

public class Example1 extends MainApplication {
	/**
	 * This is a simple example showing how to launch simulation using custom
	 * mobility model, energy model, custom edge orchestrator, custom tasks
	 * generator, and custom edge devices. By removing them, you will use the
	 * default models provided by SatEdgeSim. As you can see, this class extends
	 * the Main class provided by SatEdgeSim, which is required for this example to
	 * work.
	 */
	public Example1(int fromIteration, int step_) {
		super(fromIteration, step_);
	}

	public static void main(String[] args) {
		// To change the mobility model
		setCustomMobilityModel(DefaultMobilityModel.class);

		// To change the tasks orchestrator
		setCustomEdgeOrchestrator(CustomEdgeOrchestrator.class);

		// To change the tasks generator
		setCustomTasksGenerator(CustomTasksGenerator.class);

		// To use a custom edge device/datacenters class
		setCustomEdgeDataCenters(CustomDataCenter.class);

		// To use a custom energy model
		setCustomEnergyModel(CustomEnergyModel.class);

		/* to use the default one you can simply delete or comment those lines */

		// Finally,you can launch the simulation
		launchSimulation();
	}

}
