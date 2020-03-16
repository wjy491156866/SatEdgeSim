package edu.weijunyong.satedgesim.LocationManager;

import edu.weijunyong.satedgesim.MainApplication;
import edu.weijunyong.satedgesim.DataCentersManager.ServersManager;

public class DefaultMobilityModel extends Mobility {


	public DefaultMobilityModel(Location currentLocation) {
		super(currentLocation);
	}

	public DefaultMobilityModel() { 
		super();
	}

	public Location getNextLocation(int ID, double Simulationtime, String type) {
		int time = (int)Simulationtime; //double
		String FID = Integer.toString(ID);
		String fileName = null;
	
		if (type == "cloud") {
			fileName = MainApplication.getLocationFolder() + "cloud/cloud" + FID + ".csv";
		} else if (type == "edge") {
			fileName = MainApplication.getLocationFolder() + "edge_datacenter/edge" + FID + ".csv";
		} else {
			fileName = MainApplication.getLocationFolder() + "edge_devices/mist" + FID + ".csv";
		}
		
    	double[] locationPos= ServersManager.SetDefaultlocation(fileName,time);
    	Double x_position = locationPos[0];
    	Double y_position = locationPos[1];
    	Double z_position = locationPos[2];
    	currentLocation = new Location(x_position, y_position, z_position);
    	//System.out.println("DefaultMobilityModel: "+type + FID+ " Location is: "+ x_position+","+y_position+","+z_position);
		return new Location(x_position, y_position, z_position);
	}

	public Location getCurrentLocation() {
		return this.currentLocation;
	}
}
