package edu.weijunyong.satedgesim.DataCentersManager;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmScheduler;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.weijunyong.satedgesim.LocationManager.Location;
import edu.weijunyong.satedgesim.LocationManager.Mobility;
import edu.weijunyong.satedgesim.ScenarioManager.simulationParameters;
import edu.weijunyong.satedgesim.ScenarioManager.simulationParameters.TYPES;
import edu.weijunyong.satedgesim.SimulationManager.SimLog;
import edu.weijunyong.satedgesim.SimulationManager.SimulationManager;

public class ServersManager {
	private List<DataCenter> datacentersList;
	private List<Vm> vmList;
	private List<DataCenter> orchestratorsList;
	private SimulationManager simulationManager;
	private Class<? extends Mobility> mobilityManager;
	private Class<? extends EnergyModel> energyModel;
	private Class<? extends DataCenter> edgeDataCenterType;

	public ServersManager(SimulationManager simulationManager, Class<? extends Mobility> mobilityManager,
			Class<? extends EnergyModel> energyModel, Class<? extends DataCenter> edgedatacenter) {
		datacentersList = new ArrayList<>();
		orchestratorsList = new ArrayList<>();
		vmList = new ArrayList<>();
		this.mobilityManager = mobilityManager;
		this.energyModel = energyModel;
		this.edgeDataCenterType = edgedatacenter;
		setSimulationManager(simulationManager);
	}

	public void generateDatacentersAndDevices() throws Exception {
		generateEdgeDevices();
		generateEdgeDataCenters();
		generateCloudDataCenters();
		
		// Select where the orchestrators are deployed
		if (simulationParameters.ENABLE_ORCHESTRATORS)
			selectOrchestrators();
		getSimulationManager().getSimulationLogger().print("ServersManager- Datacenters and devices were generated");

	}

	private void selectOrchestrators() {
		for (DataCenter edgeDataCenter : datacentersList) {
			if ("".equals(simulationParameters.DEPLOY_ORCHESTRATOR)
					|| ("CLOUD".equals(simulationParameters.DEPLOY_ORCHESTRATOR)
							&& edgeDataCenter.getType() == simulationParameters.TYPES.CLOUD)) {
				edgeDataCenter.setOrchestrator(true);
				orchestratorsList.add(edgeDataCenter);
			} else if ("EDGE".equals(simulationParameters.DEPLOY_ORCHESTRATOR)
					&& edgeDataCenter.getType() == simulationParameters.TYPES.EDGE_DATACENTER) {
				edgeDataCenter.setOrchestrator(true);
				orchestratorsList.add(edgeDataCenter);
			} else if ("MIST".equals(simulationParameters.DEPLOY_ORCHESTRATOR)
					&& edgeDataCenter.getType() == simulationParameters.TYPES.EDGE_DEVICE) {
				edgeDataCenter.setOrchestrator(true);
				orchestratorsList.add(edgeDataCenter);
			} else {
				SimLog.println("");
				SimLog.println("ServersManager- Unknnown orchestration deploy '" + simulationParameters.DEPLOY_ORCHESTRATOR
						+ "', please check the simulation parameters file...");
				// Cancel the simulation
				Runtime.getRuntime().exit(0);
			}
		}

	}

	public void generateEdgeDevices() throws Exception {
		// Generate edge devices instances from edge devices types in xml file
		File devicesFile = new File(simulationParameters.EDGE_DEVICES_FILE);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(devicesFile);
		NodeList edgeDevicesList = doc.getElementsByTagName("device");
		int instancesPercentage = 0;
		Element edgeElement = null;
		//从最大数目边缘设备中抽取当前场景边缘设备数量，号码随机乱序不重复
		int edgeDevicesNum = simulationParameters.MAX_NUM_OF_EDGE_DEVICES;
		int getDevicesCount = getSimulationManager().getScenario().getDevicesCount();
	    Set<Integer> set=new HashSet<Integer>();
		while(true){
			set.add((int)(Math.random()*edgeDevicesNum+1));
			if(set.size()== getDevicesCount)
				break;
		}
		//System.out.println(set);
		int[] EDGEID = new int[set.size()];
		Iterator<Integer> it = set.iterator();
		int count = 0;
        while(it.hasNext()){
            int s = it.next();
            EDGEID[count] = s;
			count++;
		}
        //EDGEID数组保存当前场景边缘设备的ID号码
        int edgeID = 0, index=0;

		// Load all devices types in edgedevices.xml file
		for (int i = 0; i < edgeDevicesList.getLength(); i++) {
			Node edgeNode = edgeDevicesList.item(i);
			edgeElement = (Element) edgeNode;
			instancesPercentage = Integer
					.parseInt(edgeElement.getElementsByTagName("percentage").item(0).getTextContent());

			// Find the number of instances of this type of devices
			float devicesInstances = getSimulationManager().getScenario().getDevicesCount() * instancesPercentage / 100;

			for (int j = 0; j < devicesInstances; j++) {
				if (datacentersList.size() > getSimulationManager().getScenario().getDevicesCount() || index >= getDevicesCount) {
					getSimulationManager().getSimulationLogger().print(
							"ServersManager- Wrong percentages values (the sum is superior than 100%), check edge_devices.xml file !");
					break;
				}
				edgeID = EDGEID[index];
				index++;
				datacentersList.add(createDatacenter(edgeElement, simulationParameters.TYPES.EDGE_DEVICE, edgeID));

			}
		}
		if (datacentersList.size() < getSimulationManager().getScenario().getDevicesCount()) // if percentage of
																								// generated devices is
																								// < 100%
			getSimulationManager().getSimulationLogger().print(
					"ServersManager- Wrong percentages values (the sum is inferior than 100%), check edge_devices.xml file !");
		// Add more devices
		int missingInstances = getSimulationManager().getScenario().getDevicesCount() - datacentersList.size();
		for (int k = 0; k < missingInstances; k++) {
			edgeID = EDGEID[index];
			index++;
			datacentersList.add(createDatacenter(edgeElement, simulationParameters.TYPES.EDGE_DEVICE, edgeID));
		}

	}

	private void generateEdgeDataCenters() throws Exception {
		// Fill list with edge data centers
		File serversFile = new File(simulationParameters.EDGE_DATACENTERS_FILE);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(serversFile);
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
			int edcID = Integer.parseInt(location.getElementsByTagName("edcID").item(0).getTextContent());
			datacentersList.add(createDatacenter(datacenterElement, simulationParameters.TYPES.EDGE_DATACENTER, edcID));
		}
	}

	private void generateCloudDataCenters() throws Exception {
		// Fill the list with cloud datacenters
		File datacentersFile = new File(simulationParameters.CLOUD_DATACENTERS_FILE);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(datacentersFile);
		NodeList datacenterList = doc.getElementsByTagName("datacenter");
		for (int i = 0; i < datacenterList.getLength(); i++) {
			Node datacenterNode = datacenterList.item(i);
			Element datacenterElement = (Element) datacenterNode;
			Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
			int cloudID = Integer.parseInt(location.getElementsByTagName("cloudID").item(0).getTextContent());
			datacentersList.add(createDatacenter(datacenterElement, simulationParameters.TYPES.CLOUD, cloudID));
		}
	}

	private DataCenter createDatacenter(Element datacenterElement, simulationParameters.TYPES type, int ID)
			throws Exception {

		double x_position = -1;
		double y_position = -1;
		double z_position = -1;

		List<Host> hostList = createHosts(datacenterElement, type);

		Location datacenterLocation = null;
		Constructor<?> datacenterConstructor = edgeDataCenterType.getConstructor(SimulationManager.class, List.class);
		DataCenter datacenter = (DataCenter) datacenterConstructor.newInstance(getSimulationManager(),
				hostList);
		//初始化位置坐标
		datacenter.setDeviceID(ID);
		//String FID = Integer.toString(ID);
		if (type == simulationParameters.TYPES.EDGE_DATACENTER) {
			//String fileName = MainApplication.getLocationFolder() + "edge_datacenter/edge Fixed Position.csv";
	    	double[] locationPos= Setnodelocation(simulationParameters.EdgeDataCenterslocationinfo,ID,0);
	    	x_position = locationPos[0];
	    	y_position = locationPos[1];
	    	z_position = locationPos[2];		
			datacenterLocation = new Location(x_position, y_position, z_position);
		} else if (type == simulationParameters.TYPES.EDGE_DEVICE) {
			datacenter.setMobile(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("mobility").item(0).getTextContent()));
			datacenter.setBattery(
					Boolean.parseBoolean(datacenterElement.getElementsByTagName("battery").item(0).getTextContent()));
			datacenter.setBatteryCapacity(Double
					.parseDouble(datacenterElement.getElementsByTagName("batteryCapacity").item(0).getTextContent()));
			//String fileName = MainApplication.getLocationFolder() + "edge_devices/mist Fixed Position.csv";
	    	double[] locationPos= Setnodelocation(simulationParameters.EdgeDeviceslocationinfo,ID,0);
	    	x_position = locationPos[0];
	    	y_position = locationPos[1];
	    	z_position = locationPos[2];
			datacenterLocation = new Location(x_position, y_position, z_position);
			getSimulationManager().getSimulationLogger().deepLog("ServersManager- Edge device:" + datacentersList.size()
					+ "    location: ( " + datacenterLocation.getXPos() + "," + datacenterLocation.getYPos() + "," + datacenterLocation.getZPos()+ " )");
		}else if (type == simulationParameters.TYPES.CLOUD) {
			//String fileName = MainApplication.getLocationFolder() + "cloud/cloud Fixed Position.csv";
	    	double[] locationPos= Setnodelocation(simulationParameters.Cloudlocationinfo,ID,0);
	    	x_position = locationPos[0];
	    	y_position = locationPos[1];
	    	z_position = locationPos[2];
			datacenterLocation = new Location(x_position, y_position, z_position);
		}
		
		double idleConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("idleConsumption").item(0).getTextContent());
		double maxConsumption = Double
				.parseDouble(datacenterElement.getElementsByTagName("maxConsumption").item(0).getTextContent());
		datacenter.setOrchestrator(Boolean
				.parseBoolean(datacenterElement.getElementsByTagName("isOrchestrator").item(0).getTextContent()));
		datacenter.setType(type);
		if (type == TYPES.EDGE_DEVICE)
			datacenter.setTasksGeneration(Boolean
					.parseBoolean(datacenterElement.getElementsByTagName("generateTasks").item(0).getTextContent()));

		Constructor<?> mobilityConstructor = mobilityManager.getConstructor(Location.class);
		datacenter.setMobilityManager(mobilityConstructor.newInstance(datacenterLocation));

		Constructor<?> energyConstructor = energyModel.getConstructor(double.class, double.class);
		datacenter.setEnergyModel(energyConstructor.newInstance(maxConsumption, idleConsumption));
		return datacenter;
	}

	private List<Host> createHosts(Element datacenterElement, simulationParameters.TYPES type) {

		// Here are the steps needed to create a hosts and vms for that datacenter.
		List<Host> hostList = new ArrayList<>();

		NodeList hostNodeList = datacenterElement.getElementsByTagName("host");
		for (int j = 0; j < hostNodeList.getLength(); j++) {

			Node hostNode = hostNodeList.item(j);
			Element hostElement = (Element) hostNode;
			int numOfCores = Integer.parseInt(hostElement.getElementsByTagName("core").item(0).getTextContent());
			double mips = Double.parseDouble(hostElement.getElementsByTagName("mips").item(0).getTextContent());
			long storage = Long.parseLong(hostElement.getElementsByTagName("storage").item(0).getTextContent());
			long bandwidth;
			long ram;
			ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());

			if (type == simulationParameters.TYPES.CLOUD) {
				bandwidth = simulationParameters.WAN_BANDWIDTH / hostNodeList.getLength();
				//ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());
			} else {
				bandwidth = simulationParameters.BANDWIDTH_WLAN / hostNodeList.getLength();
				//ram = Integer.parseInt(hostElement.getElementsByTagName("ram").item(0).getTextContent());
			}

			// A Machine contains one or more PEs or CPUs/Cores. Therefore, should
			// create a list to store these PEs before creating
			// a Machine.
			List<Pe> peList = new ArrayList<>();

			// Create PEs and add these into the list.
			// for a quad-core machine, a list of 4 PEs is required:
			for (int i = 0; i < numOfCores; i++) {
				peList.add(new PeSimple(mips, new PeProvisionerSimple())); // need to store Pe id and MIPS Rating
			}

			ResourceProvisioner ramProvisioner = new ResourceProvisionerSimple();
			ResourceProvisioner bwProvisioner = new ResourceProvisionerSimple();
			VmScheduler vmScheduler = new VmSchedulerSpaceShared();

			// Create Hosts with its id and list of PEs and add them to the list of machines
			Host host = new HostSimple(ram, bandwidth, storage, peList);
			host.setRamProvisioner(ramProvisioner).setBwProvisioner(bwProvisioner).setVmScheduler(vmScheduler);

			NodeList vmNodeList = hostElement.getElementsByTagName("VM");
			for (int k = 0; k < vmNodeList.getLength(); k++) {
				Node vmNode = vmNodeList.item(k);
				Element vmElement = (Element) vmNode;
				// VM Parameters
				long vmNumOfCores = Long.parseLong(vmElement.getElementsByTagName("core").item(0).getTextContent());
				double vmMips = Double.parseDouble(vmElement.getElementsByTagName("mips").item(0).getTextContent());
				long vmStorage = Long.parseLong(vmElement.getElementsByTagName("storage").item(0).getTextContent());
				long vmBandwidth;
				int vmRam;

				vmBandwidth = bandwidth / vmNodeList.getLength();
				vmRam = Integer.parseInt(vmElement.getElementsByTagName("ram").item(0).getTextContent());

				CloudletScheduler tasksScheduler;

				if ("SPACE_SHARED".equals(simulationParameters.CPU_ALLOCATION_POLICY))
					tasksScheduler = new CloudletSchedulerSpaceShared();
				else
					tasksScheduler = new CloudletSchedulerTimeShared();

				Vm vm = new VmSimple(vmList.size(), vmMips, vmNumOfCores);
				vm.setRam(vmRam).setBw(vmBandwidth).setSize(vmStorage).setCloudletScheduler(tasksScheduler);
				vm.getUtilizationHistory().enable();
				vm.setHost(host);
				vmList.add(vm);
			}
			hostList.add(host);
		}

		return hostList;
	}

	public List<Vm> getVmList() {
		return vmList;

	}

	public List<DataCenter> getDatacenterList() {
		return datacentersList;
	}

	public List<DataCenter> getOrchestratorsList() {
		return orchestratorsList;
	}

	public SimulationManager getSimulationManager() {
		return simulationManager;
	}

	public void setSimulationManager(SimulationManager simulationManager) {
		this.simulationManager = simulationManager;
	}
	
	public static double[] Setnodelocation(List<Map<String,List<String>>> locationinfo, int id, int timeindex){
		if (simulationParameters.LOCATIONTIMENUM < timeindex) {
			SimLog.println("ServersManager- This time (" +timeindex +") is Overflow ");
			timeindex = timeindex % simulationParameters.LOCATIONTIMENUM;
		}
		String i1 = "",i2 = "",i3 = "";
        double xpos =0,ypos =0,zpos =simulationParameters.EARTH_RADIUS;
		if (locationinfo.size() !=0) {
			if(!(locationinfo.get(1).isEmpty())) {
				i1 = locationinfo.get(id-1).get("2").get(timeindex+1);
	     		i2 = locationinfo.get(id-1).get("3").get(timeindex+1);
	     		i3 = locationinfo.get(id-1).get("4").get(timeindex+1);
			}
	 		else {
	 			SimLog.println("ServersManager- locationinfo Map is null, check the '.csv' file.");
				System.exit(0);  
	 		}
	 		xpos = Double.parseDouble(i1)*1000;
	 		ypos = Double.parseDouble(i2)*1000;
	 		zpos = Double.parseDouble(i3)*1000;
	 		//System.out.println("The location is: "+xpos + "," + ypos + "," + zpos);
	 		double Geohigh = Math.abs(Math.sqrt(Math.pow(xpos, 2)+ Math.pow(ypos, 2)+ Math.pow(zpos, 2)));
	 	    if(simulationParameters.EARTH_RADIUS > Geohigh) {
	 	    	SimLog.println("ServersManager- locationinfo Id:"+ id + " Incorrect data. Time is: " +timeindex +", check the '.csv' file.");
				System.exit(0); 
	 	    }
		}
 	    else {
 	    	SimLog.println("ServersManager- locationinfo List is null, check the '.csv' file.");
			System.exit(0); 
 	    }
		double[] locationPos= {xpos,ypos,zpos};
		return locationPos;
	}
	
	/*
	public static double[] SetDefaultlocation(String fileName, int id, int time){
		if (simulationParameters.LOCATIONTIMENUM < time) {
			System.out.println("This time (" +time +") is Overflow ");
			time = simulationParameters.LOCATIONTIMENUM;
		}
		int count_id=0;
		String i1 = "";
        String i2 = "";
        String i3 = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line = null;
            while((line = reader.readLine()) != null){
                String item[] = line.split(",");//CSV格式文件为逗号分隔符文件，这里根据逗号切分
                if (item[0].equals("\"Time (EpSec)\"")){ // 
                	count_id++;
                }
                if (count_id == id) {
                	if(!(item[0].equals("\"Time (EpSec)\"")) && !(item[0].equals(""))) {
                		if((int)Double.parseDouble(item[0])== time) {
                			i1 = item[1];
                			i2 = item[2];
                			i3 = item[3];
                			break;
                		}
                	}
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        double xPos = Double.parseDouble(i1)*1000;
        double yPos = Double.parseDouble(i2)*1000;
        double zPos = Double.parseDouble(i3)*1000;
        double Geohigh = Math.abs(Math.sqrt(Math.pow(xPos, 2)+ Math.pow(yPos, 2)+ Math.pow(zPos, 2)));
        if(simulationParameters.EARTH_RADIUS > Geohigh) {
            System.out.println(fileName +"Id:"+id+ " Incorrect data. Time is: " +time);
            xPos = xPos*10;
            yPos = yPos*10;
            zPos = zPos*10;
        }
        double[] locationPos= {xPos,yPos,zPos};
        return locationPos;
    }
    */
}
