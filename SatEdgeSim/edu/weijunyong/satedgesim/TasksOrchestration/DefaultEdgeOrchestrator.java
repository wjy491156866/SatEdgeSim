package edu.weijunyong.satedgesim.TasksOrchestration;

import java.util.ArrayList;
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
		}else {
			SimLog.println("");
			SimLog.println("Default Orchestrator- Unknnown orchestration algorithm '" + algorithm
					+ "', please check the simulation parameters file...");
			// Cancel the simulation
			Runtime.getRuntime().exit(0);
		}
		return -1;
	}
	
	//orchestrationHistory.size() = vmList.size()
	//

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
	
	
	
	//***************************************************************************************************************
	/*
	
	//根据贪心算法将一组任务分配给一组虚拟机，使总执行时间接近最短
	public void bindCloudletsToVmsTimeAwared(){
		int cloudletNum=cloudletList.size();
		int vmNum=vmList.size();
		//time[i][j] 表示任务i在虚拟机j上的执行时间
		double[][] time=new double[cloudletNum][vmNum];
		//cloudletList按MI降序排列, vm按MIPS升序排列
		Collections.sort(cloudletList,new CloudletComparator());
		Collections.sort(vmList,new VmComparator());

		for(int i=0;i<cloudletNum;i++){
			for(int j=0;j<vmNum;j++){
				time[i][j]=
					(double)cloudletList.get(i).getCloudletLength()/vmList.get(j).getMips();
				//System.out.print("time["+i+"]["+j+"]="+time[i][j]+" ");
				//For test
			}
			//System.out.println(); //For test
		}
		double[] vmLoad=new double[vmNum];//在某个虚拟机上任务的总执行时间
		int[] vmTasks=new int[vmNum]; //在某个Vm上运行的任务数量
		double minLoad=0;//记录当前任务分配方式的最优值
		int idx=0;//记录当前任务最优分配方式对应的虚拟机列号
		//第一个cloudlet分配给最快的vm
		vmLoad[vmNum-1]=time[0][vmNum-1];
		vmTasks[vmNum-1]=1;
		cloudletList.get(0).setVmId(vmList.get(vmNum-1).getId());
		for(int i=1;i<cloudletNum;i++){
			minLoad=vmLoad[vmNum-1]+time[i][vmNum-1];
			idx=vmNum-1;
			for(int j=vmNum-2;j>=0;j--){
				//如果当前虚拟机未分配任务，则比较完当前任务
				//分配给该虚拟机是否最优，即可以退出循环
				if(vmLoad[j]==0){
					if(minLoad>=time[i][j])
						idx=j;
					break;
				}
				if(minLoad>vmLoad[j]+time[i][j]){
					minLoad=vmLoad[j]+time[i][j];
					idx=j;
				}
				//简单的负载均衡
				else if(minLoad==vmLoad[j]+time[i][j]&&vmTasks[j]<vmTasks[idx])
					idx=j;
			}
			vmLoad[idx]+=time[i][idx];
			vmTasks[idx]++;
			cloudletList.get(i).setVmId(vmList.get(idx).getId());
			//System.out.print(i+"th "+"vmLoad["+idx+"]="+vmLoad[idx]+"minLoad="+minLoad);
			//System.out.println();
		}
	}
	//根据指令长度降序排列任务，需要导入包Java.util.Comparator
	private class CloudletComparator implements Comparator<Cloudlet>{
		public int compare(Cloudlet cl1,Cloudlet cl2) {
			return (int)(cl2.getCloudletLength()-cl1.getCloudletLength());
		}
	}
	
	//根据执行速度升序排列虚拟机
		private class VmComparator implements Comparator<Vm>{
			public int compare(Vm vm1,Vm vm2) {
				return (int)(vm1.getMips()-vm2.getMips());
			}
		}
	
	*/
	//***************************************************************************************************************

	
	
	
	
	
	

	@Override
	public void resultsReturned(Task task) { 
		
	}

}
