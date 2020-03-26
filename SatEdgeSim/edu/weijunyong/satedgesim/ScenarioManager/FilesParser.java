package edu.weijunyong.satedgesim.ScenarioManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.weijunyong.satedgesim.ScenarioManager.simulationParameters.TYPES;
import edu.weijunyong.satedgesim.SimulationManager.SimLog;

public class FilesParser {

	// Scan files
	public boolean checkFiles(String simProp, String edgeDevicesFile, String edgeDataCentersFile, String appFile, String cloudFile
			, String cloudlocationFile,String edgeDataCenterslocationFile,String edgeDeviceslocationFile) {
		simulationParameters.EDGE_DEVICES_FILE = edgeDevicesFile;
		simulationParameters.EDGE_DATACENTERS_FILE = edgeDataCentersFile;
		simulationParameters.CLOUD_DATACENTERS_FILE = cloudFile;
		simulationParameters.CloudlocationFile = cloudlocationFile;
		simulationParameters.EdgeDataCenterslocationFile = edgeDataCenterslocationFile;
		simulationParameters.EdgeDeviceslocationFile = edgeDeviceslocationFile;
		
		
		return (checkcSVFiles(cloudlocationFile) && checkcSVFiles(edgeDataCenterslocationFile) && checkcSVFiles(edgeDeviceslocationFile) 
				&& checkSimulationProperties(simProp) && checkXmlFiles(edgeDevicesFile, TYPES.EDGE_DEVICE)
				&& checkXmlFiles(edgeDataCentersFile, TYPES.EDGE_DATACENTER) && checkXmlFiles(cloudFile, TYPES.CLOUD) && checkAppFile(appFile));
	}
	
	private boolean checkcSVFiles(String csvFile) {
		SimLog.println("FilesParser- Checking file: " + csvFile);
		if (csvFile == simulationParameters.CloudlocationFile) {
			simulationParameters.Cloudlocationinfo = Getlocationinfo(csvFile);
			return checklocationinfo(simulationParameters.Cloudlocationinfo,csvFile);
		}
		else if (csvFile == simulationParameters.EdgeDataCenterslocationFile) {
			simulationParameters.EdgeDataCenterslocationinfo = Getlocationinfo(csvFile);
			return checklocationinfo(simulationParameters.EdgeDataCenterslocationinfo,csvFile);
		} else if (csvFile == simulationParameters.EdgeDeviceslocationFile){
			simulationParameters.EdgeDeviceslocationinfo = Getlocationinfo(csvFile);
			return checklocationinfo(simulationParameters.EdgeDeviceslocationinfo,csvFile);
		}
		else {
			SimLog.println(
					"FilelParser, Error, check the 'csvFile' file name.");
			return false;
		}
	}
	
	private boolean checklocationinfo(List<Map<String,List<String>>> locationinfo, String csvFile) {
		if ((locationinfo.size() ==0) || (locationinfo.get(1).isEmpty()) || (locationinfo.get(1).get("1").size() == 0)) {
			SimLog.println("FilesParser- Failed to load LocationFolder file!"+csvFile);
			return false;
		}
		else {
			SimLog.println("FilesParser- Location file successfully Loaded!"+csvFile);
			return true;
		}
	}

	private boolean checkSimulationProperties(String simProp) {
		SimLog.println("FilesParser- Checking simulation properties file");
		boolean result = false;
		InputStream input = null;
		try {
			input = new FileInputStream(simProp);

			// loading properties file
			Properties prop = new Properties();
			prop.load(input);
			simulationParameters.PARALLEL = Boolean.parseBoolean(prop.getProperty("parallel_simulation").trim());

			simulationParameters.INITIALIZATION_TIME = Double
					.parseDouble(prop.getProperty("initialization_time").trim()); // seconds
			simulationParameters.SIMULATION_TIME = simulationParameters.INITIALIZATION_TIME
					+ (double) 60 * Double.parseDouble(prop.getProperty("simulation_time").trim()); // seconds

			simulationParameters.DISPLAY_REAL_TIME_CHARTS = Boolean
					.parseBoolean(prop.getProperty("display_real_time_charts").trim());
			simulationParameters.AUTO_CLOSE_REAL_TIME_CHARTS = Boolean
					.parseBoolean(prop.getProperty("auto_close_real_time_charts").trim());
			simulationParameters.CHARTS_UPDATE_INTERVAL = Double
					.parseDouble(prop.getProperty("charts_update_interval").trim());
			simulationParameters.SAVE_CHARTS = Boolean.parseBoolean(prop.getProperty("save_charts").trim());

			//simulationParameters.AREA_LENGTH = Integer.parseInt(prop.getProperty("length").trim()); // seconds
			//simulationParameters.AREA_WIDTH = Integer.parseInt(prop.getProperty("width").trim()); // seconds
			simulationParameters.EARTH_RADIUS = Double
					.parseDouble(prop.getProperty("Earth_radius").trim());	// meters
			simulationParameters.MIN_HEIGHT= Double
					.parseDouble(prop.getProperty("min_height").trim());	// meters
			simulationParameters.UPDATE_INTERVAL = Double.parseDouble(prop.getProperty("update_interval").trim()); // seconds
			simulationParameters.DEEP_LOGGING = Boolean.parseBoolean(prop.getProperty("deep_log_enabled").trim());
			simulationParameters.SAVE_LOG = Boolean.parseBoolean(prop.getProperty("save_log_file").trim());
			simulationParameters.CLEAN_OUTPUT_FOLDER = Boolean
					.parseBoolean(prop.getProperty("clear_output_folder").trim());
			simulationParameters.WAIT_FOR_TASKS = Boolean.parseBoolean(prop.getProperty("wait_for_all_tasks").trim());
			simulationParameters.ENABLE_REGISTRY = Boolean.parseBoolean(prop.getProperty("enable_registry").trim());
			simulationParameters.registry_mode = prop.getProperty("registry_mode").trim();
			simulationParameters.ENABLE_ORCHESTRATORS = Boolean
					.parseBoolean(prop.getProperty("enable_orchestrators").trim());

			simulationParameters.EDGE_DEVICES_RANGE = Integer.parseInt(prop.getProperty("edge_devices_range").trim()); // meters
			simulationParameters.EDGE_DATACENTERS_RANGE = Integer.parseInt(prop.getProperty("edge_datacenters_coverage").trim()); // meters
			simulationParameters.CLOUD_RANGE = Integer.parseInt(prop.getProperty("cloud_coverage").trim()); // meters
			simulationParameters.PAUSE_LENGTH = Integer.parseInt(prop.getProperty("pause_length").trim());// seconds
			
			simulationParameters.EDGE_DEVICE_COUNTER_TIME = Integer
					.parseInt(prop.getProperty("edge_device_counter_time").trim());
			//simulationParameters.MIN_NUM_OF_EDGE_DEVICES = Integer
			//		.parseInt(prop.getProperty("min_number_of_edge_devices").trim());
			//cloudlocationFile,edgeDataCenterslocationFile,edgeDeviceslocationFile
			int cloudlocationtimenum = simulationParameters.Cloudlocationinfo.get(1).get("1").size()-2;
			int edgeDataCenterslocationtimenum = simulationParameters.EdgeDataCenterslocationinfo.get(1).get("1").size()-2;
			int edgeDeviceslocationtimenum = simulationParameters.EdgeDeviceslocationinfo.get(1).get("1").size()-2;
			if((cloudlocationtimenum == edgeDataCenterslocationtimenum) && (edgeDataCenterslocationtimenum == edgeDeviceslocationtimenum)) {
				simulationParameters.LOCATIONTIMENUM = edgeDeviceslocationtimenum;	//time_num
			} else {
				SimLog.println("FilelParser, Error,  the csv time num is not the same, check the '.csv' file.");
				System.exit(0);
			}
			if(simulationParameters.SIMULATION_TIME > simulationParameters.LOCATIONTIMENUM) {
				SimLog.println(
						"FilelParser, Error,  the location time is lower than the simulation time, check the 'simulation.properties' file."
						+ "The simulation time is bigger than the number(minutes): "+ simulationParameters.LOCATIONTIMENUM/60);
				System.exit(0);
			}
			simulationParameters.MAX_NUM_OF_EDGE_DEVICES = simulationParameters.EdgeDeviceslocationinfo.size(); //node_num
			
			//String fileNamemist = simulationParameters.LocationFolder + "edge_devices/mist Fixed Position.csv";
			//int[] locationinfo = Getlocationinfo(fileNamemist); //0为time_num，1为node_num
			
			//simulationParameters.LOCATIONTIMENUM = locationinfo[0]; //time_num
			//simulationParameters.MAX_NUM_OF_EDGE_DEVICES = locationinfo[1]; //node_num
			//simulationParameters.MAX_NUM_OF_EDGE_DEVICES = Integer
			//		.parseInt(prop.getProperty("max_number_of_edge_devices").trim());
			simulationParameters.MIN_NUM_OF_EDGE_DEVICES = simulationParameters.MAX_NUM_OF_EDGE_DEVICES
					/simulationParameters.EDGE_DEVICE_COUNTER_TIME;
			if (simulationParameters.MIN_NUM_OF_EDGE_DEVICES > simulationParameters.MAX_NUM_OF_EDGE_DEVICES) {
				SimLog.println(
						"FilelParser, Error,  the entered min number of edge devices is superior than the max number, check the 'simulation.properties' file.");
				System.exit(0);
			}
			//simulationParameters.EDGE_DEVICE_COUNTER_STEP = Integer
			//		.parseInt(prop.getProperty("edge_device_counter_size").trim());
			simulationParameters.EDGE_DEVICE_COUNTER_STEP = simulationParameters.MAX_NUM_OF_EDGE_DEVICES 
					/simulationParameters.EDGE_DEVICE_COUNTER_TIME;
			
			
			//simulationParameters.SPEED = Double.parseDouble(prop.getProperty("speed").trim()); // meters per second m/s
			
			simulationParameters.BANDWIDTH_WLAN = 1024 * Integer.parseInt(prop.getProperty("wlan_bandwidth").trim()); // Mbits/s
																														// to
																														// Kbits/s
			simulationParameters.WAN_BANDWIDTH = 1024 * Integer.parseInt(prop.getProperty("wan_bandwidth").trim());// Mbits/s
																													// to
																													// Kbits/s
			//simulationParameters.WAN_PROPAGATION_DELAY = Double
			//		.parseDouble(prop.getProperty("wan_propogation_delay").trim()); // seconds
			simulationParameters.WAN_PROPAGATION_SPEED = Double
							.parseDouble(prop.getProperty("wan_propogation_speed").trim()); // seconds
			
			simulationParameters.NETWORK_UPDATE_INTERVAL = Double
					.parseDouble(prop.getProperty("network_update_interval").trim()); // seconds
			simulationParameters.CPU_ALLOCATION_POLICY = prop.getProperty("Applications_CPU_allocation_policy").trim();
			simulationParameters.TASKS_PER_EDGE_DEVICE_PER_MINUTES = Integer
					.parseInt(prop.getProperty("tasks_generation_rate").trim());
			simulationParameters.ORCHESTRATION_ARCHITECTURES = prop.getProperty("orchestration_architectures")
					.split(",");
			simulationParameters.ORCHESTRATION_AlGORITHMS = prop.getProperty("orchestration_algorithms").split(",");
			simulationParameters.DEPLOY_ORCHESTRATOR = prop.getProperty("deploy_orchestrator").trim();

			simulationParameters.CONSUMED_ENERGY_PER_BIT = Double
					.parseDouble(prop.getProperty("consumed_energy_per_bit").trim()); // J/bit
			simulationParameters.AMPLIFIER_DISSIPATION_FREE_SPACE = Double
					.parseDouble(prop.getProperty("amplifier_dissipation_free_space").trim()); // J/bit/m^2
			simulationParameters.AMPLIFIER_DISSIPATION_MULTIPATH = Double
					.parseDouble(prop.getProperty("amplifier_dissipation_multipath").trim()); // J/bit/m^4

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
					result = true;
					SimLog.println("FilesParser- Properties file successfully Loaded propoerties file!");
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				SimLog.println("FilesParser- Failed to load properties file!");
				result = false;
			}
		}

		return result;

	}

	private boolean checkXmlFiles(String xmlFile, TYPES type) {
		SimLog.println("FilesParser- Checking file: " + xmlFile);

		try {
			File devicesFile = new File(xmlFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document xmlDoc = dBuilder.parse(devicesFile);
			xmlDoc.getDocumentElement().normalize();
			NodeList datacenterList;
			if (type == TYPES.EDGE_DEVICE)
				datacenterList = xmlDoc.getElementsByTagName("device");
			else
				datacenterList = xmlDoc.getElementsByTagName("datacenter");
			double percentage = 0;
			for (int i = 0; i < datacenterList.getLength(); i++) {

				Node datacenterNode = datacenterList.item(i);

				Element datacenterElement = (Element) datacenterNode;
				isAttribtuePresent(datacenterElement, "arch");
				isAttribtuePresent(datacenterElement, "os");
				isAttribtuePresent(datacenterElement, "vmm");
				isElementPresent(datacenterElement, "idleConsumption");
				isElementPresent(datacenterElement, "maxConsumption");
				if (type == TYPES.EDGE_DEVICE) {
					isElementPresent(datacenterElement, "mobility");
					isElementPresent(datacenterElement, "battery");
					isElementPresent(datacenterElement, "percentage");
					percentage += Double
							.parseDouble(datacenterElement.getElementsByTagName("percentage").item(0).getTextContent());
					isElementPresent(datacenterElement, "batteryCapacity");
					isElementPresent(datacenterElement, "generateTasks");
				} else if (type == TYPES.CLOUD) {
					simulationParameters.NUM_OF_CLOUD_DATACENTERS++;
					Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);///
					isElementPresent(location, "cloudID");///
				} else {
					simulationParameters.NUM_OF_EDGE_DATACENTERS++;
					Element location = (Element) datacenterElement.getElementsByTagName("location").item(0);
					isElementPresent(location, "edcID");
				}

				NodeList hostList = datacenterElement.getElementsByTagName("host");
				for (int j = 0; j < hostList.getLength(); j++) {
					Node hostNode = hostList.item(j);

					Element hostElement = (Element) hostNode;
					isElementPresent(hostElement, "core");
					isElementPresent(hostElement, "mips");
					isElementPresent(hostElement, "ram");
					isElementPresent(hostElement, "storage");

					NodeList vmList = hostElement.getElementsByTagName("VM");
					for (int k = 0; k < vmList.getLength(); k++) {
						Node vmNode = vmList.item(k);

						Element vmElement = (Element) vmNode; 
						isElementPresent(vmElement, "core");
						isElementPresent(vmElement, "mips");
						isElementPresent(vmElement, "ram");
						isElementPresent(vmElement, "storage");
					}
				}
			} 
			if (percentage != 100 && type == TYPES.EDGE_DEVICE) {
				SimLog.println(
						"FilesParser- check the edge_devices.xml file!, the sum of percentages must be equal to 100%");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			SimLog.println("FilesParser- Failed to load edge devices file!");
			return false;
		}
		
		if (type == TYPES.EDGE_DATACENTER) {
			//String edclocationname = simulationParameters.LocationFolder + "edge_datacenter/edge Fixed Position.csv";
			//int[] edclocationinfo = Getlocationinfo(edclocationname); //0为time_num，1为node_num
			//System.out.println("The time: "+ edclocationinfo[0]+". The node num: "+edclocationinfo[1]);
			if(simulationParameters.NUM_OF_EDGE_DATACENTERS == simulationParameters.EdgeDataCenterslocationinfo.size()) {
				SimLog.println("FilesParser- EdgeDataCenters XML file successfully Loaded!");
				return true;
			}
			else {
				SimLog.println("FilesParser- Failed to load edge_datacenter XML file! check the edge_datacenter.xml file!");
				return false;
			}
				
		}
		else if (type == TYPES.CLOUD) {
			//String cloudlocationname = simulationParameters.LocationFolder + "cloud/cloud Fixed Position.csv";
			//int[] cloudlocationinfo = Getlocationinfo(cloudlocationname); //0为time_num，1为node_num
			//System.out.println("The time: "+ cloudlocationinfo[0]+". The node num: "+cloudlocationinfo[1]);
			if(simulationParameters.NUM_OF_CLOUD_DATACENTERS == simulationParameters.Cloudlocationinfo.size()) {
				SimLog.println("FilesParser- cloud  XML file successfully Loaded!");
				return true;
			}
			else {
				SimLog.println("FilesParser- Failed to load cloud XML file! check the cloud.xml file!");
				return false;
			}
		}
		SimLog.println("FilesParser- Edge devices XML file successfully Loaded!");
		return true;
	}

	private boolean checkAppFile(String appFile) {
		SimLog.println("FilesParser- Checking applications file");
		Document doc;
		try {
			File devicesFile = new File(appFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(devicesFile);
			doc.getDocumentElement().normalize();

			NodeList appList = doc.getElementsByTagName("application");
			simulationParameters.APPS_COUNT = appList.getLength();// save the number of apps, this will be used later by
																	// the tasks generator
			simulationParameters.APPLICATIONS_TABLE = new double[appList.getLength()][7];
			for (int i = 0; i < appList.getLength(); i++) {
				Node appNode = appList.item(i);

				Element appElement = (Element) appNode;
				isAttribtuePresent(appElement, "name");
				isElementPresent(appElement, "max_delay");
				isElementPresent(appElement, "container_size");
				isElementPresent(appElement, "request_size");
				isElementPresent(appElement, "results_size");
				isElementPresent(appElement, "task_length");
				isElementPresent(appElement, "required_core"); 
				isElementPresent(appElement, "poisson_interarrival"); 

				double max_delay = Double
						.parseDouble(appElement.getElementsByTagName("max_delay").item(0).getTextContent());
				double container_size = Double
						.parseDouble(appElement.getElementsByTagName("container_size").item(0).getTextContent());
				double request_size = Double
						.parseDouble(appElement.getElementsByTagName("request_size").item(0).getTextContent());
				double results_size = Double
						.parseDouble(appElement.getElementsByTagName("results_size").item(0).getTextContent());
				double task_length = Double
						.parseDouble(appElement.getElementsByTagName("task_length").item(0).getTextContent());
				double required_core = Double
						.parseDouble(appElement.getElementsByTagName("required_core").item(0).getTextContent()); 
				double poisson_interarrival = Double
						.parseDouble(appElement.getElementsByTagName("poisson_interarrival").item(0).getTextContent()); 

				// save apps parameters
				simulationParameters.APPLICATIONS_TABLE[i][0] = max_delay; // max delay in seconds
				simulationParameters.APPLICATIONS_TABLE[i][1] = request_size; // avg request size (KB)
				simulationParameters.APPLICATIONS_TABLE[i][2] = results_size; // avg downloaded results size (KB)
				simulationParameters.APPLICATIONS_TABLE[i][3] = task_length; // avg task length (MI)
				simulationParameters.APPLICATIONS_TABLE[i][4] = required_core; // required # of core
				simulationParameters.APPLICATIONS_TABLE[i][5] = container_size; // the size of the container (KB) 
				simulationParameters.APPLICATIONS_TABLE[i][6] = poisson_interarrival; // poisson_interarrival in seconds 
			}

		} catch (Exception e) {
			SimLog.println("FilesParser- Applications XML file cannot be parsed!");
			e.printStackTrace();
			return false;
		}
		SimLog.println("FilesParser- Applications XML file successfully loaded!");
		return true;
	}

	private void isElementPresent(Element element, String key) {
		try {
			String value = element.getElementsByTagName(key).item(0).getTextContent();
			if (value == null || value.isEmpty()) {
				throw new IllegalArgumentException(
						"Element '" + key + "' is not found in '" + element.getNodeName() + "'");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Element '" + key + "' is not found in '" + element.getNodeName() + "'");
		}
	}

	private void isAttribtuePresent(Element element, String key) {
		String value = element.getAttribute(key);
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException(
					"Attribure '" + key + "' is not found in '" + element.getNodeName() + "'");
		}
	}
	
	//这种方法减少了CSV文件的数量，将csv数据读进内存，减少IO读取时间
	public static List<Map<String,List<String>>> Getlocationinfo(String fileName){	  
		  List<Map<String,List<String>>> data = new ArrayList<>();	//全部数据
		  Map<String,List<String>> map = new HashMap<>();	//记录每一块数据
		  List<String> list1 = new ArrayList<>();	//第一列
		  List<String> list2 = new ArrayList<>();	//第二列
		  List<String> list3 = new ArrayList<>();	//第三列
		  List<String> list4 = new ArrayList<>();	//第四列
		  try {
			  BufferedReader reader = new BufferedReader(new FileReader(fileName));//换成你的文件名
		      String line = null;
		      while((line = reader.readLine()) != null){
		    	  String item[] = line.split(",");//CSV格式文件为逗号分隔符文件，这里根据逗号切分
		    	  if (!(item[0].equals(""))){
		    		  list1.add(item[0]);
		    		  list2.add(item[1]);
		    		  list3.add(item[2]);
		    		  list4.add(item[3]);
		    	  }
		    	  else {
		        	  map.put("1",list1);
		        	  map.put("2",list2);
		        	  map.put("3",list3);
		        	  map.put("4",list4);
		        	  data.add(map);
		        	  list1 = new ArrayList<>();
		        	  list2 = new ArrayList<>();
		        	  list3 = new ArrayList<>();
		        	  list4 = new ArrayList<>();
		        	  map = new HashMap<>();
		          }
		      }
		      reader.close();
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
		  return data;
	}
	
	//这种方法减少了CSV文件的数量，但是单个CSV文件的大小太大，IO读取速度很慢。
	/*
	public static int[] Getlocationinfo(String fileName){
		int count_time=0,count_node=0; 
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));//换成你的文件名
            String line = null;
            while((line = reader.readLine()) != null){
                String item[] = line.split(",");//CSV格式文件为逗号分隔符文件，这里根据逗号切分
                if (item[0].equals("\"Time (EpSec)\"")){ //统计节点数量
                	count_node++;
                }
                if (count_node == 1) {
                	if(!(item[0].equals("\"Time (EpSec)\"")) && !(item[0].equals(""))) { //统计时间情况
                		count_time++;
                	}
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        int[] locationinfo= {count_time-1,count_node};
        return locationinfo;
	}
	*/
	
	
	//这种方法会导致CSV文件数量过多，生成数据工作量太大
	/*
	 * 方法名：getCsvFilesCount
	 * 作用：统计.csv文件个数
	 */
	/*
	public static int getCsvFilesCount(File srcFile){
		int count=0;
	    // 判断传入的文件是不是为空
	    if (srcFile == null) {
	        throw new NullPointerException();
	    }
	    // 把所有目录、文件放入数组
	    File[] files = srcFile.listFiles();
	    // 遍历数组每一个元素
	    for (File f : files) {
	        // 判断元素是不是文件夹，是文件夹就重复调用此方法（递归）
	        if (f.isDirectory()) {
	        	getCsvFilesCount(f);
	        }else {
	            // 判断文件是不是以.csv结尾的文件，并且count++（注意：文件要显示扩展名）
	            if (f.getName().endsWith(".csv")) {
	                count++;
	            }
	        }
	    }
	    // 返回.csv文件个数
	    return count;
	}
	    
	public static int Getlocationtimenum(String fileName){
		int timenum=-2;  //出去标题和第0秒
	    try {
	        BufferedReader reader = new BufferedReader(new FileReader(fileName));//换成你的文件名
	        reader.readLine();//第一行信息，为标题信息，不用,如果需要，注释掉
	        while((reader.readLine()) != null){
	        	timenum++;
	        }
	        reader.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return timenum;
	}
	*/
	
}
