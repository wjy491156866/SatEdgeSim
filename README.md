# SatEdgeSim

SatEdgeSim: A Toolkit for Modeling and Simulation of Performance Evaluation in Satellite Edge Computing Environments, 
which uses [CloudSim Plus](http://cloudsimplus.org) and [PureEdgeSim](https://github.com/CharafeddineMechalikh/PureEdgeSim) as the underlying simulation framework.

# SATELLITE EDGE COMPUTING BACKGROUND
For edge computing hotspot issues, Table I shows the differences between satellite edge computing and ground edge computing scenarios. The existing simulators mainly simulate the ground edge computing environment, and the problems considered are also based on the ground network. In the configuration of the environment, the satellite edge computing simulator needs to provide a simulation environment that can solve the problems described in Table I.

**TABLE I. 	DIFFERENCES BETWEEN SATELLITE AND GROUND EDGE COMPUTING SCENARIOS**

| Related issues | Ground edge computing | Satellite edge computing|
|--|--|--|
|Network architecture	|Cloud、Edge、Mist node(edge device)| 	Cloud、Edge、Mist node(edge device)|
|Network topology	|cloud to edge, network topology changes from static to dynamic, from dynamic to random.	|Network topology changes dynamically, but the dynamic change is periodic. |
|Network resources|	cloud is unlimited. Edge and mist node (edge device) have limitations.	|All nodes have limitations.|
|Energy consumption|	Cloud and fixed node have no constraint. The mobile node of edge or mist have constraint. |	All network nodes have energy consumption constraint.|
|Node mobility|	Cloud are a fixed node. Edge and mist are fixed nodes or mobile nodes with slow moving speed.	|All network nodes are mobile nodes with fast moving speed.|
|Task deployment (offloading)|	It considers scenarios with short distance and low dynamic.	|It considers scenarios with long distance and high dynamic.|

**a) Network architecture:** Both scenarios include cloud computing centers, edge data centers, and mist nodes (edge devices), as shown in Figure 1. From mist nodes to edge data centers to cloud computing centers, the computing power of each layer is gradually increasing. 
Mist nodes (edge devices) can request assistance from the cloud, edge, or other mist nodes to process tasks. It can also assist in processing tasks from other nodes. Edge data centers can not only handle various applications offloading from mist nodes (edge devices), but also complete tasks such as task scheduling, task analysis, and data fusion for the entire network. Cloud computing centers have the function of edge data center or mist node (edge device).
 
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200508145910335.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxMjg1NzA5,size_5,color_FFFFFF,t_70#pic_center)
 
**Figure 1. 	Satellite edge computing network architecture**

The modeling of satellite edge computing simulator on the network architecture can refer to the three-tier architecture of cloud computing center, edge data center, and mist nodes (edge devices).

**b) Network topology:** In terms of network topology, the entire network topology in the satellite edge computing scene is changing in real time, but this dynamic change shows a certain rule. In the satellite edge computing simulator, the mobile characteristics of satellite should be taken into account.

**c) Network resource:** Generally speaking, the network resources of ground edge computing are more abundant, while the network resources of satellite edge computing are limited. In the satellite edge computing simulator, the network resources of satellite edge computing need to be characterized and quantified.

**d) Energy consumption:** all network nodes of satellite edge computing architecture have energy consumption constraints, because the power supply of satellite equipment is solar energy. The satellite edge computing simulator needs to consider the energy consumption model.

**e) Node mobility:** The important difference between the two scenarios is the mobility of the nodes. In the satellite edge computing simulator, the mobility of satellite is one of the problems that must be considered. Because network topology changes are closely related to satellite mobility, the simulator needs to add satellite position changes.

**f) Task deployment (offloading):** In the satellite edge computing scenario, the communication distance considered for task deployment is pretty long, and the dynamics is strong. In the satellite edge computing simulator, energy consumption, communication distance, mobility and other factors need to be added to the task deployment strategy.

There are seven architectures considered for task deployment: 1) Cloud computing only: tasks are deployed to the cloud computing center. 2) Mist computing and cloud computing: tasks are deployed to the mist node or cloud computing center. 3) Mist computing and edge computing: tasks are deployed into the mist node or edge data center. 4) Edge computing only: tasks are deployed to the edge data center. 5) Edge computing and cloud computing: tasks are deployed to the edge data center or cloud computing center. 6) Mist computing only: no edge or cloud computing at all. Tasks are deployed to other nearby mist nodes or execute locally. 7) All: tasks may be deployed on any satellite with computing capabilities,

# ARCHITECTURE AND APPLICATION SCENARIO OF SATEDGESIM
## SatEdgeSim Architecture
SatEdgeSim takes advantage of CloudSim Plus's support for discrete event simulation and cloud computing simulation, and uses PureEdgeSim's modular simulation framework and its expansion of the edge computing environment to build a satellite edge computing simulation environment. PureEdge- Sim provides high scalability, broad applicability and better code reusability. SatEdgeSim inherits the modular simulation framework. The main modules include: Simulation Manager, Data Centers Manager, Tasks Generator, Location Manager, Network Module, Tasks Orchestration and Scenario Manager. The block diagram of SatEdgeSim is shown in Figure 2. 
 
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/2020050815291941.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxMjg1NzA5,size_16,color_FFFFFF,t_70#pic_center)
 
**Figure 2. 	 SatEdgeSim block diagram**

The next part is an introduction to the extended functions of each module of SatEdgeSim.

**a) SimulationManager:** It is responsible for management and driving the entire simulation events, as well as the generation and output of simulation results. SatEdgeSim adds the function of counting the results of task end-to-end delay and task success rate (or failure rate), and optimizes the output display of energy consumption, so as to better show the differences of different task deployment strategies.

**b) DataCentersManager:** This module is responsible for generating and managing the entire simulation node, and it also has an energy consumption model. In SatEdgeSim, a number is set for each type of satellite (cloud, edge data center, edge device) so that the corresponding position can be identified by the number. The positions of all satellites are initialized and updated in this module.
 
**c) TasksGenerator:** The role of the task generator is to assign applications to each edge device. Each application can be characterized by a custom application description file. SatEdgeSim expands the task generator to make the task arrival interval obey Poisson distribution. The task generator associates the three parameters of task arrival time, task type, and equipment, and adds them to the task list. By continuously inserting tasks, the simulator gets a list of all tasks during the entire simulation time.

**d) LocationManager:** It is used to manage the location of each satellite. The satellite's location update is triggered by the DataCentersManager module, and gets the location of the corresponding satellite from LocationManager. The position coordinate data of Satellite is generated by STK (Satellite Tool Kit), through which the three-dimensional coordinate position of each Satellite at a certain time can be obtained. Users can customize the satellite orbit to get different satellite orbit parameters.

**e) Network Module:** In this module, SatEdgeSim inherits the considerations of network load and bandwidth limitation of PureEdgeSim, and adds the propagation delay caused by satellite communication distance into the network module.

**f) TasksOrchestration:** In this module, users can customize different task deployment algorithms according to scenario settings. In SatEdgeSim, the relative position and visibility model of the satellite need to be considered before selecting the appropriate node.

**g) ScenarioManager:** This module mainly initializes the parameters of the simulation scenario. SatEdgeSim adds the earth radius and the 3d coordinate parameters.

## SatEdgeSim Application Scenario

In order to evaluate the performance of SatEdgeSim in the simulation and modeling of satellite edge computing scenarios, we established a satellite edge computing virtual environment composed of multiple satellite constellations. The satellite edge computing virtual environment is shown in Figure 3.

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200508153053750.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxMjg1NzA5,size_16,color_FFFFFF,t_70#pic_center)

**Figure 3. 	Satellite edge computing simulation scenario**

The satellite's orbit data and resource situation, the application's model can be characterized by file input. The satellite constellation parameters configured for different resource types are shown in Table II.

**TABLE II. 	SATELLITE PARAMETERS FOR DIFFERENT RESOURCE TYPES**

|Resource type|	Satellite height|	Orbit inclination|	Constellation parameter (Walker)|
|--|--|--|--|
|cloud	|3500km	|0deg|	6/ 1/ 0|
|	|3600km	|60deg	|12/ 4/ 2|
|edge	|2150km	|56deg	|24/ 6/ 2|
|mist	|1150km	|53deg	|160/ 32/ 2|
|	|1110km	|53.8deg	|160/ 32/ 2|
|	|1130km	|74deg	|40/ 8/ 2|
|	|1275km	|81deg	|40/ 5/ 2|
|	|1325km	|70deg	|48/ 6/ 2|
|	|550km|	45deg	|552/ 46/ 2|

The cloud computing center is composed of 18 satellites, with a high orbital altitude and the highest resources. The edge data center is composed of 24 satellites with a medium orbit altitude and a slightly lower amount of resources than the cloud computing center. The satellite mist node (edge device) is composed of 1000 satellites and is divided into six different low-orbit constellations with the lowest computing power. The mist node (edge device) will generate tasks and deploy the task according to the set deployment strategy. In order to better evaluate the effectiveness of the task deployment strategy, we configure the network architecture in all modes, and task deployment is not limited by resource types.

# EXPERIMENTAL PARAMETERS AND RESULTS

## Experimental parameters

The effectiveness of SatEdgeSim in satellite edge computing environment and the effectiveness of the proposed task deployment strategy can be verified by experimental results. The important simulation parameters are shown in Table III.

**TABLE III. 	SIMULATION PARAMETERS**

|Parameter|	Value|
|--|--|
|Simulation time| 	10 (minutes)|
|CPU utilization and energy consumption update interval|	1 (seconds)|
|Network update interval|	1 (seconds)|
|Earth radius	|6378137 (meters)|
|Satellite min height	|400000 (meters)|
|Network bandwidth	|1000 (Mbps)|
|Number of cloud|	18|
|Number of edge|	24|
Number of mist(edge devices)|	1000|
|edge devices count times|	10|
|Architectures|	ALL|
|Orchestration algorithms|	ROUND_ROBIN, TRADE_OFF, TRADI_POLLING, WEIGHT_GREEDY|

In the WEIGHT_GREEDY task deployment strategy, the weighting ratio of the four evaluation indicators of transmission distance, CPU processing time, number of tasks in parallel, and equipment energy consumption is 6: 6: 5: 3. In the satellite edge computing scenario, the timeliness of the task is important with the minimum energy consumption. Therefore, the weight of the evaluation indicators related to timeliness is relatively large. Finally, we use SatEdgeSim for simulation according to the above scenario description and parameter configuration.
## Simulation Results

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200508153314430.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxMjg1NzA5,size_16,color_FFFFFF,t_70#pic_center)

**Figure 4. 	Task's average end-to-end delay**

Figure 4 shows the average end-to-end latency of tasks under different deployment strategies. It can be seen from the figure that the average end-to-end delay of all deployment strategies shows an upward trend with the increase in the number of satellites. Compared with other task deployment strategies, the proposed task deployment strategy WEIGHT_GREEDY has a lower average end-to-end delay. This strategy focuses on the impact of transmission distance, and chooses closer satellites for deployment, which greatly reduces the propagation delay.


![在这里插入图片描述](https://img-blog.csdnimg.cn/20200508153425419.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxMjg1NzA5,size_16,color_FFFFFF,t_70#pic_center)

**Figure 5. 	Average energy consumption of each satellite

The average energy consumption of each satellite node is shown in Figure 5. As the number of satellites increases, the average energy consumption of each satellite node remains basically unchanged. The proposed task deployment strategy, takes into account the energy consumption of each device in a balanced manner, so that the energy consumption of each satellite is as small as possible, which is about 3% -5% less than other deployment strategies.
 
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200508153510615.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxMjg1NzA5,size_16,color_FFFFFF,t_70#pic_center)
 
**Figure 6. 	Task success rate**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200508153619276.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxMjg1NzA5,size_16,color_FFFFFF,t_70#pic_center)

**(a) Task failure due to delay**     

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200508153709173.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxMjg1NzA5,size_16,color_FFFFFF,t_70#pic_center)

**(b) Task failure due to mobility**

**Figure 7. 	Task failure rate**

In terms of task success rate, the task success rate of the proposed task deployment strategy is higher than other strategies, maintaining above 80%, as shown in Figure 6. There are two main reasons for the failure of the task. First, the task is not completed within the maximum tolerance delay. Second, the movement of the satellite position exceeds the communication range.
Figure 7 (a) (b) shows the situation of task failure rate caused by delay and mobility, respectively. The proposed strategy reduces task failures due to delays, but the failure rate due to mobility is slightly higher. 

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200508153811151.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxMjg1NzA5,size_16,color_FFFFFF,t_70#pic_center)

**(a) Cloud**                          

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200508153833128.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxMjg1NzA5,size_16,color_FFFFFF,t_70#pic_center)

**(b) Edge**
 
 ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200508153901379.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzMxMjg1NzA5,size_16,color_FFFFFF,t_70#pic_center)
 
**(c) Mist**

**Figure 8. 	The number of processing tasks for each resource type**

Figure 8 (a) (b) (c) shows the number of processing tasks for each resource type. By comparison, we can find that WEIGHT_GREEDY makes more tasks processed on the edge side, which will have lower end-to-end delay. Because low-orbit satellites move fast, it is easy to cause mission failures due to mobility. For tasks with low timeliness requirements, we can choose cloud or edge data centers to deploy to reduce task failures caused by mobility.



# Authors : Junyong Wei, Suzhi Cao, Siyan Pan, Jiarong Han, Lei Yan, Lei Zhang

## Please cite it as

J. Wei, S. Cao, S. Pan, J. Han, L. Yan, and L. Zhang, "SatEdgeSim: A Toolkit for Modeling and Simulation of Performance Evaluation in Satellite Edge Computing Environments," in 2020 12th International Conference on Communication Software and Networks, ICCSN 2020, June 12, 2020 - June 15, 2020, Chongqing, China, 2020: Institute of Electrical and Electronics Engineers Inc.

Other publication using SatEdgeSim can be found [here](https://orcid.org/0000-0001-7649-3380)

Files in locationflie (mistfixed Position.csv ,edge Fixed Position.csv ,cloud Fixed Position.csv) can be downloaded from the following link.
link：https://pan.baidu.com/s/1lI-7TOJSVJsIj6C7IJV6TA  Extraction code：4zww

There are still some parts that can be improved in the project. I hope everyone can try to modify and submit your ideas to make Satedgesim more realistic.

For any questions, contact me at weijunyong17@mails.ucas.ac.cn or read [here](https://github.com/wjy491156866/SatEdgeSim/edit/master/README.md).



