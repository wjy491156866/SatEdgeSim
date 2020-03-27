package examples;

import edu.weijunyong.satedgesim.MainApplication;

public class Example2 extends MainApplication {
	/**
	 * This is a simple example showing how to launch simulation using a custom
	 * energy model. by removing it, SatEdgeSim will use the default model. As you
	 * can see, this class extends the Main class provided by SatEdgeSim, which is
	 * required for this example to work
	 */
	public Example2(int fromIteration, int step_) {
		super(fromIteration, step_);
	}

	public static void main(String[] args) {
		/*
		 * To use your custom Energy model, do this: The custom energy model class can
		 * be found in the examples folder as well. by removing this line, SatEdgeSim
		 * will use the default energy model. *
		 */
		setCustomEnergyModel(CustomEnergyModel.class);

		// To use the SatEdgeSim default Energy Model you can also uncomment this:
		// setCustomEnergyModel(DefaultEnergyModel.class);

		// Start the simulation
		launchSimulation();
	}

}
