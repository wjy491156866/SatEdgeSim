package examples;

import java.util.List;
import java.util.Random;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel; 
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;

import edu.weijunyong.satedgesim.ScenarioManager.simulationParameters;
import edu.weijunyong.satedgesim.ScenarioManager.simulationParameters.TYPES;
import edu.weijunyong.satedgesim.SimulationManager.SimulationManager;
import edu.weijunyong.satedgesim.TasksGenerator.Task;
import edu.weijunyong.satedgesim.TasksGenerator.TasksGenerator;
import edu.weijunyong.satedgesim.TasksOrchestration.Orchestrator;

public class CustomTasksGenerator extends TasksGenerator {
	public CustomTasksGenerator(SimulationManager simulationManager) {
		super(simulationManager);
	}

	public List<Task> generate() {
		// get simulation time in minutes (excluding the initialization time)
		double simulationTime = (simulationParameters.SIMULATION_TIME - simulationParameters.INITIALIZATION_TIME) / 60;
		for (int dev = 0; dev < datacentersList.size(); dev++) { // for each device
			if (datacentersList.get(dev).getType() == TYPES.EDGE_DEVICE && datacentersList.get(dev).isGeneratingTasks()) {

				int app = new Random().nextInt(simulationParameters.APPS_COUNT); // pickup a random application type for
																					// every device
				datacentersList.get(dev).setApplication(app); // assign this application to that device
				
				for (int st = 0; st < simulationTime; st++) { // for each minute
					// generating tasks
					int time = st * 60;
					time += new Random().nextInt(59);// pickup random second in this minute "st";

					// Shift the time by the defined value "INITIALIZATION_TIME"
					// in order to start after generating all the resources
					time += simulationParameters.INITIALIZATION_TIME;
					insert(time, app, dev);
				}
			}
		}
		return this.getTaskList();
	}

	private void insert(int time, int app, int dev) {
		double maxLatency = (long) simulationParameters.APPLICATIONS_TABLE[app][0]; // Load length from application file
		long length = (long) simulationParameters.APPLICATIONS_TABLE[app][3]; // Load length from application file
		long requestSize = (long) simulationParameters.APPLICATIONS_TABLE[app][1];
		long outputSize = (long) simulationParameters.APPLICATIONS_TABLE[app][2];
		int pesNumber = (int) simulationParameters.APPLICATIONS_TABLE[app][4];
		long containerSize = (int) simulationParameters.APPLICATIONS_TABLE[app][5]; // the size of the container 
		Task[] task = new Task[simulationParameters.TASKS_PER_EDGE_DEVICE_PER_MINUTES];
		int id;
		double min = -1;
		int selected = datacentersList.size()-1;
		
		for (int Registrydev = 0; Registrydev < datacentersList.size(); Registrydev++) {
			if (datacentersList.get(Registrydev).getType() == TYPES.CLOUD  
					&&  Orchestrator.issetlink(datacentersList.get(Registrydev),datacentersList.get(dev))) {
				double dis = Orchestrator.getdistance(datacentersList.get(Registrydev),datacentersList.get(dev));
				if (min == -1 || min > dis) {
					min = dis;
					selected = Registrydev;
				}
			}
		}

		// generate tasks for every edge device
		for (int i = 0; i < simulationParameters.TASKS_PER_EDGE_DEVICE_PER_MINUTES; i++) {
			id = taskList.size();
			UtilizationModel utilizationModel = new UtilizationModelFull();
			task[i] = new Task(id, length, pesNumber);
			task[i].setFileSize(requestSize).setOutputSize(outputSize).setUtilizationModel(utilizationModel);
			task[i].setTime(time);
			task[i].setContainerSize(containerSize);
			task[i].setMaxLatency(maxLatency);
			task[i].setEdgeDevice(datacentersList.get(dev)); // the device that generate this task (the origin)
			task[i].setRegistry(datacentersList.get(selected)); //set the cloud as registry
			taskList.add(task[i]);
			getSimulationManager().getSimulationLogger()
					.deepLog("BasicTasksGenerator, Task " + id + " with execution time " + time + " (s) generated.");
		}
	}

}
