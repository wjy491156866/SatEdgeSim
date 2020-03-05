package edu.weijunyong.satedgesim.LocationManager;

public abstract class Mobility {

	protected Location currentLocation;

	public Mobility(Location location) {
		this.currentLocation = location;
	}

	public Mobility() { 
	}

	public abstract Location getNextLocation(int ID, double Simulationtime, String type);

	public Location getCurrentLocation() {
		return currentLocation;
	}
}
