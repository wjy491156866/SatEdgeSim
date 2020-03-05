package edu.weijunyong.satedgesim.LocationManager;

public class Location {
	private double xPos;
	private double yPos;
	private double zPos;

	public Location(double _xPos, double _yPos, double _zPos) {
		xPos = _xPos;
		yPos = _yPos;
		zPos = _zPos;
	}

	public boolean equals(Location otherLocation) {
		if (otherLocation == this)
			return true;

		return (this.xPos == otherLocation.xPos && this.yPos == otherLocation.yPos && this.zPos == otherLocation.zPos);

	}

	public double getXPos() {
		return xPos;
	}

	public double getYPos() {
		return yPos;
	}
	
	public double getZPos() {
		return zPos;
	}
}
