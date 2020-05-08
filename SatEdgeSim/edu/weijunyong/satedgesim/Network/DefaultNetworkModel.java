package edu.weijunyong.satedgesim.Network;

import java.util.List;
import org.cloudbus.cloudsim.core.events.SimEvent;

import edu.weijunyong.satedgesim.DataCentersManager.DataCenter;
import edu.weijunyong.satedgesim.DataCentersManager.DefaultEnergyModel;
import edu.weijunyong.satedgesim.ScenarioManager.simulationParameters;
import edu.weijunyong.satedgesim.SimulationManager.SimLog;
import edu.weijunyong.satedgesim.SimulationManager.SimulationManager;
import edu.weijunyong.satedgesim.TasksGenerator.Task;

public class DefaultNetworkModel extends NetworkModel {

	public DefaultNetworkModel(SimulationManager simulationManager) {
		super(simulationManager);
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch (ev.getTag()) {
		case SEND_REQUEST_FROM_DEVICE_TO_ORCH:
			// Send the offloading request to the orchestrator
			sendRequestFromDeviceToOrch((Task) ev.getData());
			break;
		case SEND_REQUEST_FROM_ORCH_TO_DESTINATION:
			// Forward the offloading request from orchestrator to offloading destination
			sendRequestFromOrchToDest((Task) ev.getData());
			break;
		case DOWNLOAD_CONTAINER:
			// Pull the container from the registry
			addContainer((Task) ev.getData());
			break;
		case SEND_RESULT_TO_ORCH:
			// Send the execution results to the orchestrator
			sendResultFromDevToOrch((Task) ev.getData());
			break;
		case SEND_RESULT_FROM_ORCH_TO_DEV:
			// Transfer the execution results from the orchestrators to the device
			sendResultFromOrchToDev((Task) ev.getData());
			break;
		case UPDATE_PROGRESS:
			// update the progress of the current transfers and their allocated bandwidth
			updateTasksProgress();
			schedule(this, simulationParameters.NETWORK_UPDATE_INTERVAL, UPDATE_PROGRESS);
			break;
		default:
			break;
		}
	}

	public List<FileTransferProgress> getTransferProgressList() {
		return transferProgressList;
	}

	public void sendRequestFromOrchToDest(Task task) {
		transferProgressList
				.add(new FileTransferProgress(task, task.getFileSize() * 8, FileTransferProgress.Type.TASK));
	}

	public void sendResultFromOrchToDev(Task task) {
		if (task.getOrchestrator() != task.getEdgeDevice())
			transferProgressList.add(
					new FileTransferProgress(task, task.getOutputSize() * 8, FileTransferProgress.Type.RESULTS_TO_DEV));
		else
			scheduleNow(simulationManager, SimulationManager.RESULT_RETURN_FINISHED, task);
	}

	public void sendResultFromDevToOrch(Task task) {
		//if (task.getOrchestrator() != task.getEdgeDevice())
		if (task.getOrchestrator() != (DataCenter)task.getVm().getHost().getDatacenter())
			transferProgressList.add(new FileTransferProgress(task, task.getOutputSize() * 8,
					FileTransferProgress.Type.RESULTS_TO_ORCH));
		else
			scheduleNow(this, DefaultNetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, task);
	}

	public void addContainer(Task task) {
		transferProgressList
				.add(new FileTransferProgress(task, task.getContainerSize() * 8, FileTransferProgress.Type.CONTAINER));
	}

	public void sendRequestFromDeviceToOrch(Task task) {
		if (task.getOrchestrator() != task.getEdgeDevice())  //协调器非本设备
			transferProgressList
					.add(new FileTransferProgress(task, task.getFileSize() * 8, FileTransferProgress.Type.REQUEST));
		else // The device orchestrate its tasks by itself, so, send the request directly to
				// destination
			scheduleNow(simulationManager, SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, task);
	}

	protected void updateTasksProgress() {
		// Ignore finished transfers, so we will start looping from the first index of
		// the remaining transfers
		for (int i = 0; i < transferProgressList.size(); i++) {
			int remainingTransfersCount_Lan = 0;
			int remainingTransfersCount_Wan = 0;
			if (transferProgressList.get(i).getRemainingFileSize() > 0) {
				for (int j = i; j < transferProgressList.size(); j++) {
					if (transferProgressList.get(j).getRemainingFileSize() > 0 && j != i) {
						if (wanIsUsed(transferProgressList.get(j))) {
							remainingTransfersCount_Wan++;
							bwUsage += transferProgressList.get(j).getRemainingFileSize();
						}
						if (sameLanIsUsed(transferProgressList.get(i).getTask(), transferProgressList.get(j).getTask())) {
							// Both transfers use same Lan
							remainingTransfersCount_Lan++;
						}
					}
				}
				// allocate bandwidths
				transferProgressList.get(i).setLanBandwidth(getLanBandwidth(remainingTransfersCount_Lan));
				transferProgressList.get(i).setWanBandwidth(getWanBandwidth(remainingTransfersCount_Wan));
				updateBandwidth(transferProgressList.get(i));
				updateTransfer(transferProgressList.get(i));
			}

		}
	}

	protected void updateTransfer(FileTransferProgress transfer) {

		double oldRemainingSize = transfer.getRemainingFileSize();

		// Update progress (remaining file size)
		transfer.setRemainingFileSize(transfer.getRemainingFileSize()
				- (simulationParameters.NETWORK_UPDATE_INTERVAL * transfer.getCurrentBandwidth()));

		// Update LAN network usage delay
		transfer.setLanNetworkUsage(transfer.getLanNetworkUsage()
				+ (oldRemainingSize - transfer.getRemainingFileSize()) / transfer.getCurrentBandwidth());

		// Update WAN network usage delay
		if (wanIsUsed(transfer))
			transfer.setWanNetworkUsage(transfer.getWanNetworkUsage()
					+ (oldRemainingSize - transfer.getRemainingFileSize()) / transfer.getCurrentBandwidth());
		if (transfer.getRemainingFileSize() <= 0) {// Transfer finished
			transfer.setRemainingFileSize(0);
			transferFinished(transfer);
		}
	}

	protected void updateEnergyConsumption(FileTransferProgress transfer, String type) {
		// update energy consumption
		if ("Orchestrator".equals(type)) {
			calculateEnergyConsumption(transfer.getTask().getEdgeDevice(), transfer.getTask().getOrchestrator(),
					transfer);
		} else if ("Destination".equals(type)) {
			calculateEnergyConsumption(transfer.getTask().getOrchestrator(),
					((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()), transfer);
		} else if ("Container".equals(type)) {
			// update the energy consumption of the registry and the device
			calculateEnergyConsumption(transfer.getTask().getRegistry(),
					transfer.getTask().getEdgeDevice(), transfer);
		} else if ("Result_Orchestrator".equals(type)) {
			calculateEnergyConsumption(((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()),
					transfer.getTask().getOrchestrator(), transfer);
		} else if ("Result_Origin".equals(type)) {
			calculateEnergyConsumption(transfer.getTask().getOrchestrator(), transfer.getTask().getEdgeDevice(),
					transfer);
		}

	}

	private void calculateEnergyConsumption(DataCenter origin, DataCenter destination,
			FileTransferProgress transfer) {
		if (origin != null) {
			origin.getEnergyModel().updatewirelessEnergyConsumption(transfer, origin, destination,
					DefaultEnergyModel.TRANSMISSION);
		}
		destination.getEnergyModel().updatewirelessEnergyConsumption(transfer, origin, destination,
				DefaultEnergyModel.RECEPTION);
	}

	protected void transferFinished(FileTransferProgress transfer) {
		// Update logger parameters
		simulationManager.getSimulationLogger().updateNetworkUsage(transfer);

		// Delete the transfer from the queue
		transferProgressList.remove(transfer);

		// If it is an offlaoding request that is sent to the orchestrator
		if (transfer.getTransferType() == FileTransferProgress.Type.REQUEST) {
			offloadingRequestRecievedByOrchestrator(transfer);
			//transfer.getTask().getEdgeDevice(), transfer.getTask().getOrchestrator()
			updateEnergyConsumption(transfer, "Orchestrator");
		}
		// If it is an task (or offloading request) that is sent to the destination
		else if (transfer.getTransferType() == FileTransferProgress.Type.TASK) {
			transfer.getTask().setReceptionTime(simulationManager.getSimulation().clock());
			executeTaskOrDownloadContainer(transfer);
			//transfer.getTask().getOrchestrator(),((DataCenter) transfer.getTask().getVm().getHost().getDatacenter())
			updateEnergyConsumption(transfer, "Destination");
		}
		// If the container has been downloaded, then execute the task now
		else if (transfer.getTransferType() == FileTransferProgress.Type.CONTAINER) { 
			transfer.getTask().setReceptionTime(simulationManager.getSimulation().clock());
			containerDownloadFinished(transfer);
			//transfer.getTask().getRegistry(),transfer.getTask().getEdgeDevice()
			updateEnergyConsumption(transfer, "Container");
		}
		// If the transfer of execution results to the orchestrator has finished
		else if (transfer.getTransferType() == FileTransferProgress.Type.RESULTS_TO_ORCH) {
			returnResultToDevice(transfer);
			//(DataCenter) transfer.getTask().getVm().getHost().getDatacenter()),transfer.getTask().getOrchestrator()
			updateEnergyConsumption(transfer, "Result_Orchestrator");
		}
		// Results transferred to the device
		else {		//transfer.getTransferType() == FileTransferProgress.Type.RESULTS_TO_DEV
			resultsReturnedToDevice(transfer);
			//transfer.getTask().getOrchestrator(), transfer.getTask().getEdgeDevice()
			updateEnergyConsumption(transfer, "Result_Origin");
		}

	}

	protected void containerDownloadFinished(FileTransferProgress transfer) {
		scheduleNow(simulationManager, SimulationManager.EXECUTE_TASK, transfer.getTask());
	}

	protected void resultsReturnedToDevice(FileTransferProgress transfer) {
		// if the results are returned from different location, consider the wan propagation delay
		if (transfer.getTask().getOrchestrator() != transfer.getTask().getEdgeDevice()) {
			double WAN_PROPAGATION_DELAY = Getpropagationdelay(transfer.getTask().getOrchestrator()
					,transfer.getTask().getEdgeDevice());
			schedule(simulationManager, WAN_PROPAGATION_DELAY, SimulationManager.RESULT_RETURN_FINISHED,
					transfer.getTask());
		}
		else
			scheduleNow(simulationManager, SimulationManager.RESULT_RETURN_FINISHED, transfer.getTask());
	}

	protected void returnResultToDevice(FileTransferProgress transfer) {
		// if the results are returned from different location, consider the wan propagation delay
		if (transfer.getTask().getOrchestrator() != ((DataCenter) transfer.getTask().getVm().getHost().getDatacenter())) {
			double WAN_PROPAGATION_DELAY = Getpropagationdelay((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()
					, transfer.getTask().getOrchestrator());
			schedule(this, WAN_PROPAGATION_DELAY, DefaultNetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV,
					transfer.getTask());
		}
		else
			scheduleNow(this, DefaultNetworkModel.SEND_RESULT_FROM_ORCH_TO_DEV, transfer.getTask());
	}

	protected void executeTaskOrDownloadContainer(FileTransferProgress transfer) {
		//get the orchestration deploy
		simulationParameters.TYPES type = null;
		if ("".equals(simulationParameters.registry_mode)
				|| ("CLOUD".equals(simulationParameters.registry_mode))) {
			type = simulationParameters.TYPES.CLOUD;
		} else if ("EDGE".equals(simulationParameters.registry_mode)) {
			type = simulationParameters.TYPES.EDGE_DATACENTER;
		} else if ("MIST".equals(simulationParameters.registry_mode)) {
			type = simulationParameters.TYPES.EDGE_DEVICE;
		} else {	//simulationParameters.registry_mode 可以继续添加自定义类型
			SimLog.println("");
			SimLog.println("SimulationManager- Unknnown orchestration deploy '" + simulationParameters.DEPLOY_ORCHESTRATOR
					+ "', please check the simulation parameters file...");
			// Cancel the simulation
			Runtime.getRuntime().exit(0);
		}
		double WAN_PROPAGATION_DELAY_TASK = Getpropagationdelay(transfer.getTask().getOrchestrator()
				,((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()));
		if (simulationParameters.ENABLE_REGISTRY 
				&& !((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()).getType().equals(type)){
			// if the registry is enabled and the node where task offloaded(Type) is different with the registry_mode(Type), 
			//then download the container
			if (((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()) != transfer.getTask().getOrchestrator()
					&& ((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()) != transfer.getTask().getEdgeDevice()) {
				//find the closest registry_mode
				double min = -1;
				int selected = 0;
				double distance;
				for (int i = 0; i < datacentersList.size(); i++) {
					if (datacentersList.get(i).getType() == type 
							&& SimulationManager.issetlink((DataCenter) transfer.getTask().getVm().getHost().getDatacenter(),datacentersList.get(i))) {
						distance = SimulationManager.getdistance((DataCenter) transfer.getTask().getVm().getHost().getDatacenter(),datacentersList.get(i));
						if (min == -1 || min > distance) {
							min = distance;
							selected = i;
						}
					}
				}
				transfer.getTask().setRegistry(datacentersList.get(selected));
				double WAN_PROPAGATION_DELAY_DOWNLOAD_CONTAINER = Getpropagationdelay((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()
						,transfer.getTask().getRegistry());
				double WAN_PROPAGATION_DELAY = WAN_PROPAGATION_DELAY_DOWNLOAD_CONTAINER + WAN_PROPAGATION_DELAY_TASK;
				schedule(this, WAN_PROPAGATION_DELAY, DefaultNetworkModel.DOWNLOAD_CONTAINER, transfer.getTask());
			}
			else {
				//scheduleNow(this, DefaultNetworkModel.DOWNLOAD_CONTAINER, transfer.getTask());
				//schedule(this, WAN_PROPAGATION_DELAY_TASK, DefaultNetworkModel.DOWNLOAD_CONTAINER, transfer.getTask());
				scheduleNow(simulationManager, SimulationManager.EXECUTE_TASK, transfer.getTask());
			}
		} 
		else {// if the registry is disabled, execute directly the request, as it represents
				// the offloaded task in this case
			//task.getEdgeDevice().getId() != task.getVm().getHost().getDatacenter().getId()
			if (((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()) != transfer.getTask().getOrchestrator()
					&& ((DataCenter) transfer.getTask().getVm().getHost().getDatacenter()) != transfer.getTask().getEdgeDevice()) {
				schedule(simulationManager, WAN_PROPAGATION_DELAY_TASK, SimulationManager.EXECUTE_TASK,
						transfer.getTask());
			}
			else
				scheduleNow(simulationManager, SimulationManager.EXECUTE_TASK, transfer.getTask());
		}
	}

	protected void offloadingRequestRecievedByOrchestrator(FileTransferProgress transfer) {
		// Find the offloading destination and execute the task
		if (transfer.getTask().getOrchestrator() != transfer.getTask().getEdgeDevice()) {
			double WAN_PROPAGATION_DELAY = Getpropagationdelay(transfer.getTask().getEdgeDevice()
					, transfer.getTask().getOrchestrator());
			schedule(simulationManager, WAN_PROPAGATION_DELAY,
					SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, transfer.getTask());
		}
		else
			scheduleNow(simulationManager, SimulationManager.SEND_TASK_FROM_ORCH_TO_DESTINATION, transfer.getTask());
	}

	@Override
	protected void startEntity() {
		schedule(this, 1, UPDATE_PROGRESS);
	}
	
	public double Getpropagationdelay(DataCenter origin, DataCenter destination) { //计算传播时间
		double distance = SimulationManager.getdistance(origin,destination);
		double propagationdelay = distance / simulationParameters.WAN_PROPAGATION_SPEED;
		return propagationdelay;
	}

}
