package edu.weijunyong.satedgesim.TasksOrchestration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cloudbus.cloudsim.vms.Vm;

import edu.weijunyong.satedgesim.DataCentersManager.DataCenter;
import edu.weijunyong.satedgesim.ScenarioManager.simulationParameters;
import edu.weijunyong.satedgesim.SimulationManager.SimLog;
import edu.weijunyong.satedgesim.SimulationManager.SimulationManager;
import edu.weijunyong.satedgesim.TasksGenerator.Task;

public class DefaultEdgeOrchestrator extends Orchestrator {
	public DefaultEdgeOrchestrator(SimulationManager simulationManager) {
		super(simulationManager);
	}
	
	public static int FindVmId_TP =0;
	public static int Counttask = 0;

	protected int findVM(String[] architecture, Task task) {
		if ("ROUND_ROBIN".equals(algorithm)) {
			return roundRobin(architecture, task);
		} else if ("TRADE_OFF".equals(algorithm)) {
			return tradeOff(architecture, task);
		} else if ("TRADI_POLLING".equals(algorithm)) {
			return TradiPolling(architecture, task);
		} else if ("WEIGHT_GREEDY".equals(algorithm)) {
			return weightGreedy(architecture, task);
		} else if ("RANDOM_VM".equals(algorithm)) {
			return RandomVm(architecture, task);
		} else {
			SimLog.println("");
			SimLog.println("Default Orchestrator- Unknnown orchestration algorithm '" + algorithm
					+ "', please check the simulation parameters file...");
			// Cancel the simulation
			Runtime.getRuntime().exit(0);
		}
		return -1;
	}
	
	//orchestrationHistory.size() = vmList.size()
	
	
	//Comprehensive weighted greedy algorithm
	//评价指标类型一致化，无量纲化，动态加权，综合评价
	private int weightGreedy(String[] architecture, Task task) {
		//获取样本值
		List<Double> disdelay = new ArrayList<>();	//第一列
		List<Double> exedelay = new ArrayList<>();	//第二列
		List<Double> vmnum = new ArrayList<>();	//第三列
		List<Double> energylim = new ArrayList<>();	//第四列
		for (int i = 0; i < orchestrationHistory.size(); i++) {
			//传播延时
			double disdelay_tem = SimulationManager.getdistance(((DataCenter) vmList.get(i).getHost().getDatacenter())
					, task.getEdgeDevice())/simulationParameters.WAN_PROPAGATION_SPEED;
			disdelay.add(disdelay_tem);
			//处理延时
			double exedelay_tem = task.getLength()/vmList.get(i).getMips();
			exedelay.add(exedelay_tem);
			//VM运行的任务数
			vmnum.add((double)orchestrationHistory.get(i).size());
			//vm的能耗
			double energyuse =10*(Math.log10(((DataCenter) vmList.get(i).getHost().getDatacenter()).getEnergyModel().getTotalEnergyConsumption()));
			energylim.add(energyuse);	
		}
		//标准化（归一化）
		List<Double> disdelay_stand = new ArrayList<>();	//第一列
		List<Double> exedelay_stand = new ArrayList<>();	//第二列
		List<Double> vmnum_stand = new ArrayList<>();	//第三列
		List<Double> energylim_stand = new ArrayList<>();	//第四列
		disdelay_stand = standardization(disdelay);
		exedelay_stand = standardization(exedelay);
		vmnum_stand = standardization(vmnum);
		energylim_stand = standardization(energylim);
		
		//加权综合评定
		int vm = -1;
		double min = -1;
		double min_factor;// vm with minimum assigned tasks;
		double a=0.3, b=0.3, c=0.25, d=0.15;
		// get best vm for this task
		for (int i = 0; i < orchestrationHistory.size(); i++) {
			if (offloadingIsPossible(task, vmList.get(i), architecture)) {
				
				min_factor = a*disdelay_stand.get(i) + b*exedelay_stand.get(i) + c*vmnum_stand.get(i) + d*energylim_stand.get(i);
				if (min == -1) { // if it is the first iteration
					min = min_factor;
					// if this is the first time, set the first vm as the
					vm = i; // best one
				} else if (min > min_factor) { // if this vm has more cpu mips and less waiting tasks
					// idle vm, no tasks are waiting
					min = min_factor;
					vm = i;
				}
			}
		}
		// assign the tasks to the found vm
		return vm;
	}
	
	public List<Double> standardization (List<Double> Pre_standar){	//极值差法标准化
		List<Double> standard = new ArrayList<>();
		double premax = Collections.max(Pre_standar);
		double premin = Collections.min(Pre_standar);
		for(int k=0; k<Pre_standar.size(); k++) {
			double temp =(Pre_standar.get(k)-premin)/(premax-premin);
			standard.add(temp);
		}
		return standard;
	}

	
	
	private int tradeOff(String[] architecture, Task task) {
		int vm = -1;
		double min = -1;
		double new_min;// vm with minimum assigned tasks;

		// get best vm for this task
		for (int i = 0; i < orchestrationHistory.size(); i++) {
			if (offloadingIsPossible(task, vmList.get(i), architecture)) {
				double latency = 1;
				double energy = 1;
				if (((DataCenter) vmList.get(i).getHost().getDatacenter())
						.getType() == simulationParameters.TYPES.CLOUD) {
					latency = 1.6;
					energy = 1.1;
				} else if (((DataCenter) vmList.get(i).getHost().getDatacenter())
						.getType() == simulationParameters.TYPES.EDGE_DEVICE) {
					energy = 1.4;
				}
				new_min = (orchestrationHistory.get(i).size() + 1) * latency * energy * task.getLength() / vmList.get(i).getMips();
				if (min == -1) { // if it is the first iteration
					min = new_min;
					// if this is the first time, set the first vm as the
					vm = i; // best one
				} else if (min > new_min) { // if this vm has more cpu mips and less waiting tasks
					// idle vm, no tasks are waiting
					min = new_min;
					vm = i;
				}
			}
		}
		// assign the tasks to the found vm
		return vm;
	}
	
	
	//执行任务最少的vm
	private int roundRobin(String[] architecture, Task task) {
		List<Vm> vmList = simulationManager.getServersManager().getVmList();
		int vm = -1;
		int minTasksCount = -1; // vm with minimum assigned tasks;
		// get best vm for this task
		for (int i = 0; i < orchestrationHistory.size(); i++) {
			if (offloadingIsPossible(task, vmList.get(i), architecture)) {
				if (minTasksCount == -1) {
					minTasksCount = orchestrationHistory.get(i).size();
					// if this is the first time, set the first vm as the best one
					vm = i;
				} else if (minTasksCount > orchestrationHistory.get(i).size()) {
					minTasksCount = orchestrationHistory.get(i).size();
					// new min found, so we choose it as the best VM
					vm = i;
					break;
				}
			}
		}
		// assign the tasks to the found vm
		return vm;
	}
	
	//轮询算法虚拟机编号轮询只考虑能否建链不考虑资源情况
	private int TradiPolling(String[] architecture, Task task) {
		List<Vm> vmList = simulationManager.getServersManager().getVmList();
		List<Task> tasksList = simulationManager.getTasksList();
		List<Integer>  minfindvmid = new ArrayList<>();		//记录可以调度的虚拟机
		boolean flag = false;
		//当轮询变量比虚拟机编号大的时候，取余
		if(FindVmId_TP> vmList.size()-1) {
			FindVmId_TP = (FindVmId_TP+1) % vmList.size();
		}
		// get best vm for this task
		for (int i = 0; i < orchestrationHistory.size(); i++) {
			if (offloadingIsPossible(task, vmList.get(i), architecture)) {
				flag = true;
				minfindvmid.add(i); //把可以进行调度的虚拟机先记下来
				if(FindVmId_TP <=i) {	//遇到与FindVmId_TP相等或者大的Id结束循环
					FindVmId_TP = i;
					break;
				}
			} else{ 
				if((i == vmList.size()-1) && flag) {	//当FindVmId_TP过大且比它大的vm没有符合的
					FindVmId_TP = minfindvmid.get(0);	//FindVmId_TP取第一个满足的
					flag = false;
					
				}
			}
		}
		// assign the tasks to the found vm
		int vm = FindVmId_TP;
		FindVmId_TP++;
		Counttask++;
		//System.out.println("task "+task.getId() + "vm id is: " + vm+". taskcount: "+Counttask);
		if(Counttask == tasksList.size()) {
			FindVmId_TP=0;
			Counttask=0;
		}
		return vm;
	}
	
	
	//随机一个vm
	private int RandomVm(String[] architecture, Task task) {
		List<Vm> vmList = simulationManager.getServersManager().getVmList();
		int vm = -1;
		int RandomCount = 0; // random time;
		// get random vm for this task
		while(RandomCount<orchestrationHistory.size()) {
			double d = Math.random();
			int index = (int)(d*(orchestrationHistory.size()-1));
			if (offloadingIsPossible(task, vmList.get(index), architecture)) {
				vm = index;
				break;
			}
			RandomCount++;
		}
		
		//万一没有随机出来
		if(RandomCount>=orchestrationHistory.size()) {
			for (int i = 0; i < orchestrationHistory.size(); i++) {
				if (offloadingIsPossible(task, vmList.get(i), architecture)) {
					vm =i;
					break;
				}
			}
			RandomCount = 0;
		}
		// assign the tasks to the found vm
		return vm;
	}
	

	@Override
	public void resultsReturned(Task task) { 
		
	}

}
