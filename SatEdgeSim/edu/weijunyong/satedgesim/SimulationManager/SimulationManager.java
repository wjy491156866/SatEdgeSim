package edu.weijunyong.satedgesim.SimulationManager;

import java.io.IOException;
import java.util.List;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.vms.Vm;

import edu.weijunyong.satedgesim.DataCentersManager.DataCenter;
import edu.weijunyong.satedgesim.DataCentersManager.ServersManager;
import edu.weijunyong.satedgesim.Network.NetworkModel;
import edu.weijunyong.satedgesim.ScenarioManager.Scenario;
import edu.weijunyong.satedgesim.ScenarioManager.simulationParameters;
import edu.weijunyong.satedgesim.ScenarioManager.simulationParameters.TYPES;
import edu.weijunyong.satedgesim.TasksGenerator.Task;
import edu.weijunyong.satedgesim.TasksOrchestration.CustomBroker;
import edu.weijunyong.satedgesim.TasksOrchestration.Orchestrator;

public class SimulationManager extends CloudSimEntity {
	public static final int Base = 1000; // avoid conflict with CloudSim Plus tags
	public static final int SEND_TASK_FROM_ORCH_TO_DESTINATION = Base + 8;
	private static final int PRINT_LOG = Base + 1;
	private static final int SHOW_PROGRESS = Base + 2;
	public static final int EXECUTE_TASK = Base + 3;
	public static final int TRANSFER_RESULTS_TO_ORCH = Base + 4;
	public static final int RESULT_RETURN_FINISHED = Base + 5;
	public static final int SEND_TO_ORCH = Base + 6;
	public static final int UPDATE_REAL_TIME_CHARTS = Base + 7;
	public static int overflowtime=0;
	private CustomBroker broker;
	private List<Task> tasksList;
	private Orchestrator edgeOrchestrator;
	private ServersManager serversManager;
	private SimulationVisualizer simulationVisualizer;
	private CloudSim simulation;
	private int simulationId;
	private int iteration;
	private SimLog simLog;
	private int lastWrittenNumber = 0;
	private int oldProgress = -1;
	private Scenario scenario;
	private NetworkModel networkModel;
	private List<? extends DataCenter> orchestratorsList;
	private double failedTasksCount = 0;
	private int tasksCount = 0;
	private int waittoendCount = 0;

	public SimulationManager(SimLog simLog, CloudSim simulation, int simulationId, int iteration, Scenario scenario) {
		super(simulation);
		this.simulation = simulation;
		this.simLog = simLog;
		this.scenario = scenario;
		this.simulationId = simulationId;
		this.iteration = iteration;

		// Create Broker
		broker = createBroker();

		// Show real time results during the simulation
		if (simulationParameters.DISPLAY_REAL_TIME_CHARTS && !simulationParameters.PARALLEL)
			simulationVisualizer = new SimulationVisualizer(this);
	}

	// Start simulation
	public void startSimulation() {
		simLog.print("SimulationManager-  Orchestration algorithm= " + scenario.getStringOrchAlgorithm()
				+ "-  Architechitecture= " + scenario.getStringOrchArchitecture() + " -  number of edge devices= "
				+ scenario.getDevicesCount());
		simulation.start();
	}

	@Override
	public void startEntity() {

		// Initialize logger variables
		simLog.setGeneratedTasks(tasksList.size());
		simLog.setCurrentOrchPolicy(scenario.getStringOrchArchitecture());

		simLog.print("SimulationManager- Simulation: " + this.simulationId + "  , iteration: " + this.iteration);

		// Tasks scheduling
		for (Task task : tasksList) {
			if (!simulationParameters.ENABLE_ORCHESTRATORS)
				task.setOrchestrator(task.getEdgeDevice());

			// Schedule the tasks offloading
			schedule(this, task.getTime(), SEND_TO_ORCH, task);
		}

		// Scheduling the end of the simulation
		schedule(this, simulationParameters.SIMULATION_TIME+simulationParameters.INITIALIZATION_TIME, PRINT_LOG);

		// Updating real time charts
		if (simulationParameters.DISPLAY_REAL_TIME_CHARTS && !simulationParameters.PARALLEL)
			schedule(this, simulationParameters.INITIALIZATION_TIME, UPDATE_REAL_TIME_CHARTS);

		// Show simulation progress
		schedule(this, simulationParameters.INITIALIZATION_TIME, SHOW_PROGRESS);

		simLog.printSameLine("Simulation progress : [", "red");
	}

	@Override
	public void processEvent(SimEvent ev) {
		Task task = (Task) ev.getData();
		switch (ev.getTag()) {
		case SEND_TO_ORCH:
			// Send the offloading request to the closest orchestrator
			sendTaskToOrchestrator(task);
			break;

		case SEND_TASK_FROM_ORCH_TO_DESTINATION:
			// Send the request from the orchestrator to the offloading destination
			sendFromOrchToDestination(task);
			break;

		case EXECUTE_TASK:
			// Execute the task
			if (taskFailed(task, 2))
				return;
			broker.submitCloudlet(task);
			break;

		case TRANSFER_RESULTS_TO_ORCH:
			// Transfer the results to the orchestrator
			sendResultsToOchestrator(task);
			break;

		case RESULT_RETURN_FINISHED:
			// Result returned to edge device
			if (taskFailed(task, 0))
				return;
			this.edgeOrchestrator.resultsReturned(task);
			task.setTaskFinishTime(task.getSimulation().clock()); //设置完成时间
			simLog.getTasksdelayInfos(task);
			tasksCount++;
			break;

		case SHOW_PROGRESS:
			// Calculate the simulation progress
			int progress = 100 * broker.getCloudletFinishedList().size() / simLog.getGeneratedTasks();
			if (oldProgress != progress) {
				oldProgress = progress;
				if (progress % 10 == 0 || (progress % 10 < 5) && lastWrittenNumber + 10 < progress) {
					lastWrittenNumber = progress - progress % 10;
					if (lastWrittenNumber != 100)
						simLog.printSameLine(" " + lastWrittenNumber + " ", "red");
				} else
					simLog.printSameLine("#", "red");
			}
			schedule(this, simulationParameters.SIMULATION_TIME / 100, SHOW_PROGRESS);
			break;

		case UPDATE_REAL_TIME_CHARTS:
			// Update simulation Map
			simulationVisualizer.updateCharts();

			// Schedule the next update
			schedule(this, simulationParameters.CHARTS_UPDATE_INTERVAL, UPDATE_REAL_TIME_CHARTS);
			break;

		case PRINT_LOG:
			// Print results when simulation is over
			List<Task> finishedTasks = broker.getCloudletFinishedList();
			// If some tasks have not been executed
			if (simulationParameters.WAIT_FOR_TASKS && (tasksCount / simLog.getGeneratedTasks()) < 0.99) {
				// 1 = 100% , 0,9= 90%
				// Some tasks may take hours to be executed that's why we don't wait until
				// all of them get executed, but we only wait for 99% of tasks to be executed at
				// least, to end the simulation. that's why we set it to " < 0.99"
				// especially when 1% doesn't affect the simulation results that much, change
				// this value to lower ( 95% or 90%) in order to make simulation faster. however
				// this may affect the results
				waittoendCount++;
				if(waittoendCount <= 3) {  //30s wait
					schedule(this, 10, PRINT_LOG);
					break;
				}
				overflowtime++;
			}
			waittoendCount =0;

			simLog.printSameLine(" 100% ]", "red");

			if (simulationParameters.DISPLAY_REAL_TIME_CHARTS && !simulationParameters.PARALLEL) {
				// Close real time charts after the end of the simulation
				if (simulationParameters.AUTO_CLOSE_REAL_TIME_CHARTS)
					simulationVisualizer.close();
				try {
					// Save those charts in bitmap and vector formats
					if (simulationParameters.SAVE_CHARTS)
						simulationVisualizer.saveCharts();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// Show results and stop the simulation
			simLog.showIterationResults(finishedTasks);

			// Terminate the simulation
			simulation.terminate();
			break;
		default:
			simLog.print("Unknown event type");
			break;
		}

	}

	private void sendResultsToOchestrator(Task task) {
		if (taskFailed(task, 2))
			return;
		// If the task was offloaded
		if (task.getEdgeDevice().getId() != task.getVm().getHost().getDatacenter().getId()) {
			scheduleNow(networkModel, NetworkModel.SEND_RESULT_TO_ORCH, task);

		} else { // The task has been executed locally / no offloading
			scheduleNow(this, RESULT_RETURN_FINISHED, task);
		}
		// update tasks execution and waiting delays
		simLog.getTasksExecutionInfos(task);
	}

	private void sendFromOrchToDestination(Task task) {
		if (taskFailed(task, 1))
			return;

		// Find the best VM for executing the task
		edgeOrchestrator.initialize(task);

		// Stop in case no resource was available for this task, the offloading is
		// failed
		if (task.getVm() == Vm.NULL) {
			simLog.incrementTasksFailedLackOfRessources(task);
			tasksCount++;
			return;
		} else {
			simLog.taskSentFromOrchToDest(task);
		}

		// If the task is offloaded
		// and the orchestrator is not the offloading destination
		if (task.getEdgeDevice().getId() != task.getVm().getHost().getDatacenter().getId()
				&& task.getOrchestrator() != ((DataCenter) task.getVm().getHost().getDatacenter())) {
			scheduleNow(networkModel, NetworkModel.SEND_REQUEST_FROM_ORCH_TO_DESTINATION, task);

		} else { // The task will be executed locally / no offloading or will be executed where
					// the orchestrator is deployed (no network usage)
			scheduleNow(this, EXECUTE_TASK, task);
		}
	}

	private void sendTaskToOrchestrator(Task task) {
		if (taskFailed(task, 0))
			return;

		simLog.incrementTasksSent();

		if (simulationParameters.ENABLE_ORCHESTRATORS) {
			//get the orchestration deploy
			simulationParameters.TYPES type = null;
			if ("".equals(simulationParameters.DEPLOY_ORCHESTRATOR)
					|| ("CLOUD".equals(simulationParameters.DEPLOY_ORCHESTRATOR))) {
				type = simulationParameters.TYPES.CLOUD;
			} else if ("EDGE".equals(simulationParameters.DEPLOY_ORCHESTRATOR)) {
				type = simulationParameters.TYPES.EDGE_DATACENTER;
			} else if ("MIST".equals(simulationParameters.DEPLOY_ORCHESTRATOR)) {
				type = simulationParameters.TYPES.EDGE_DEVICE;
			} else {	//simulationParameters.DEPLOY_ORCHESTRATOR 可以继续添加自定义类型
				SimLog.println("");
				SimLog.println("SimulationManager- Unknnown orchestration deploy '" + simulationParameters.DEPLOY_ORCHESTRATOR
						+ "', please check the simulation parameters file...");
				// Cancel the simulation
				Runtime.getRuntime().exit(0);
			}
			
			if (task.getEdgeDevice().getType() == type) //if the orchestration deploy is the same with task davice, set itself  Orchestrator
			{
				task.setOrchestrator(task.getEdgeDevice());
			}
			else {
				// Send the offloading request to the closest orchestrator
				double min = -1;
				int selected = 0;
				double distance;
				for (int i = 0; i < orchestratorsList.size(); i++) {
					if (orchestratorsList.get(i).getType() == type 
							&& issetlink(task.getEdgeDevice(),orchestratorsList.get(i))) {
						distance = getdistance(task.getEdgeDevice(),orchestratorsList.get(i));
						if (min == -1 || min > distance) {
							min = distance;
							selected = i;
						}
					}
				}
				if (orchestratorsList.size() == 0) {
					simLog.printSameLine("SimulationManager- Error no orchestrator found", "red");
					tasksCount++;
					return;
				}
				task.setOrchestrator(orchestratorsList.get(selected));
			}
		}
		scheduleNow(networkModel, NetworkModel.SEND_REQUEST_FROM_DEVICE_TO_ORCH, task);
	}

	private CustomBroker createBroker() {
		CustomBroker broker;
		try {
			broker = new CustomBroker(simulation);
			broker.setSimulationManager(this);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	public NetworkModel getNetworkModel() {
		return this.networkModel;
	}

	public int getSimulationId() {
		return this.simulationId;
	}

	public SimLog getSimulationLogger() {
		return simLog;
	}

	public ServersManager getServersManager() {
		return serversManager;
	}

	public Scenario getScenario() {
		return this.scenario;
	}

	public CustomBroker getBroker() {
		return this.broker;
	}

	public double getFailureRate() {
		double result = (failedTasksCount * 100) / tasksList.size();
		failedTasksCount = 0;
		return result;
	}

	public int getIterationId() {
		return this.iteration;
	}

	public boolean taskFailed(Task task, int phase) {
		// task not generated because device died
		if (phase == 0 && task.getEdgeDevice().isDead()) {
			simLog.incrementNotGeneratedBeacuseDeviceDead();
			task.setFailureReason(Task.Status.NOT_GENERATED_BECAUSE_DEVICE_DEAD);
			setFailed(task);
			return true;
		} // Set the task as failed if the device is dead
		if (phase != 0 && task.getEdgeDevice().isDead()) {
			simLog.incrementFailedBeacauseDeviceDead(task); 
			task.setFailureReason(Task.Status.FAILED_BECAUSE_DEVICE_DEAD);
			setFailed(task);
			return true;
		}
		// or if the orchestrator died
		if (phase == 1 && task.getOrchestrator() != null && task.getOrchestrator().isDead()) { 
			task.setFailureReason(Task.Status.FAILED_BECAUSE_DEVICE_DEAD);
			simLog.incrementFailedBeacauseDeviceDead(task);
			setFailed(task);
			return true;
		}
		// or the destination device is dead
		if (phase == 2 && ((DataCenter) task.getVm().getHost().getDatacenter()).isDead()) { 
			task.setFailureReason(Task.Status.FAILED_BECAUSE_DEVICE_DEAD);
			simLog.incrementFailedBeacauseDeviceDead(task);
			setFailed(task);
			return true;
		}
		// The task is failed due to long delay
		if ((task.getSimulation().clock() - task.getTime()) > task.getMaxLatency()) {
			//task.setTaskFinishTime(task.getSimulation().clock() - task.getTime());
			task.setFailureReason(Task.Status.FAILED_DUE_TO_LATENCY);
			simLog.incrementTasksFailedLatency(task);
			setFailed(task);
			return true;
		}
		// A simple representation of task failure due to
		// device mobility, if the vm location doesn't match
		// the edge device location (that generated this task)
		if (phase == 1 && task.getOrchestrator() != null
				//&& task.getOrchestrator().getType() != simulationParameters.TYPES.CLOUD
				&& !sameLocation(task.getEdgeDevice(), task.getOrchestrator())) {
			task.setFailureReason(Task.Status.FAILED_DUE_TO_DEVICE_MOBILITY);
			simLog.incrementTasksFailedMobility(task);
			setFailed(task);
			return true;
		}
		if (phase == 2 && (task.getVm().getHost().getDatacenter()) != null
				//&& ((DataCenter) task.getVm().getHost().getDatacenter())
				//		.getType() != simulationParameters.TYPES.CLOUD
				&& !sameLocation(task.getEdgeDevice(), ((DataCenter) task.getVm().getHost().getDatacenter()))) {
			task.setFailureReason(Task.Status.FAILED_DUE_TO_DEVICE_MOBILITY);
			simLog.incrementTasksFailedMobility(task);
			setFailed(task);
			return true;
		}
		return false;
	}

	private void setFailed(Task task) {
		failedTasksCount++;
		tasksCount++;
		this.edgeOrchestrator.resultsReturned(task);
		task.setTaskFinishTime(task.getSimulation().clock()); //设置完成时间
	}

	private boolean sameLocation(DataCenter Dev1, DataCenter Dev2) {
		int RANGE = simulationParameters.EDGE_DEVICES_RANGE;
		if ((Dev1.getType() == TYPES.CLOUD && Dev2.getType() != TYPES.EDGE_DATACENTER) 
				|| (Dev1.getType() != TYPES.EDGE_DATACENTER && Dev2.getType() == TYPES.CLOUD)) {
			RANGE = simulationParameters.CLOUD_RANGE;
		}
		else if((Dev1.getType() == TYPES.EDGE_DATACENTER && Dev2.getType() != TYPES.CLOUD) 
				|| (Dev1.getType() != TYPES.CLOUD && Dev2.getType() == TYPES.EDGE_DATACENTER)){
			RANGE = simulationParameters.EDGE_DATACENTERS_RANGE;
		}
		double distance = getdistance(Dev1, Dev2);
		if (distance < RANGE && issetlink(Dev1, Dev2)) {
			//System.out.println("Simulation"+ Dev1.getId()+" the same location with"+ Dev2.getId());
			return true;
		}
		else {
			//System.out.println("Simulation"+ Dev1.getId()+" not the same location with"+ Dev2.getId());
			return false;
		}
	}
	
	
	public static boolean issetlink(DataCenter device1, DataCenter device2) {	//几何可见建立链路
		double h1 = getHight(device1), h2 = getHight(device2), d = getdistance(device1, device2);
		if(d == 0) {
			return true;
		}else
		{
			double p = (h1 + h2 + d)/2;
			double L = 2*(Math.sqrt(p*(p-h1)*(p-h1)*(p-h2)))/d;
			if(L > simulationParameters.MIN_HEIGHT + simulationParameters.EARTH_RADIUS) {
				return true;
			}
			else
				return false;
		}
	}
	
	public static double getdistance(DataCenter device1, DataCenter device2) { //distance
		return Math.abs(Math.sqrt(Math.pow((device1.getLocation().getXPos() - device2.getLocation().getXPos()), 2)
						+ Math.pow((device1.getLocation().getYPos() - device2.getLocation().getYPos()), 2)
						+ Math.pow((device1.getLocation().getZPos() - device2.getLocation().getZPos()), 2)));
	}
	
	public static double getHight(DataCenter device) { //Geocentric height
		return Math.abs(Math.sqrt(Math.pow(device.getLocation().getXPos(), 2)
				+ Math.pow(device.getLocation().getYPos(), 2)+ Math.pow(device.getLocation().getZPos(), 2)));
	}

	public void setServersManager(ServersManager serversManager) {
		// Get orchestrators list from the server manager
		orchestratorsList = serversManager.getOrchestratorsList();
		this.serversManager = serversManager;

		// Submit vm list to the broker
		simLog.deepLog("SimulationManager- Submitting VM list to the broker");
		broker.submitVmList(serversManager.getVmList());
	}

	public void setTasksList(List<Task> tasksList) {
		this.tasksList = tasksList;
	}

	public void setOrchestrator(Orchestrator edgeOrchestrator) {
		this.edgeOrchestrator = edgeOrchestrator;

	}

	public void setNetworkModel(NetworkModel networkModel) {
		this.networkModel = networkModel;
	}

	public List<Task> getTasksList() {
		return tasksList;
	}
}