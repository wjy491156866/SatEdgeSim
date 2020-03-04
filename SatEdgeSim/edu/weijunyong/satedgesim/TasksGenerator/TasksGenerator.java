package edu.weijunyong.satedgesim.TasksGenerator;

import java.util.ArrayList;
import java.util.List;

import edu.weijunyong.satedgesim.DataCentersManager.DataCenter;
import edu.weijunyong.satedgesim.SimulationManager.SimulationManager;

public abstract class TasksGenerator {
	protected List<Task> taskList;
	protected List<? extends DataCenter> datacentersList;
	private SimulationManager simulationManager;

	public TasksGenerator(SimulationManager simulationManager) {
		taskList = new ArrayList<>();
		this.setSimulationManager(simulationManager);
		this.datacentersList = this.getSimulationManager().getServersManager().getDatacenterList();
	}

	public List<Task> getTaskList() {
		return taskList;
	}

	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}

	public abstract List<Task> generate() ;
}
