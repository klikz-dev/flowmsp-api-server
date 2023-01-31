package com.flowmsp.service.preplan;

import com.mongodb.client.model.geojson.Polygon;

public class PreplanPolyStorey {
	public Polygon  locationCoords;
	public int		storey;
	public int storeyBelow;
	
	public PreplanPolyStorey() {
		this.locationCoords = null;
		this.storey = 0;
		this.storeyBelow = 0;		
	}
	
	public PreplanPolyStorey(Polygon locationCoords) {
		this.locationCoords = locationCoords;
		storey = 0;
		storeyBelow = 0;
	}
	
	public PreplanPolyStorey(Polygon locationCoords, int storey) {
		this.locationCoords = locationCoords;
		this.storey = storey;
		storeyBelow = 0;
	}
	
	public PreplanPolyStorey(Polygon locationCoords, int storey, int storeyBelow) {
		this.locationCoords = locationCoords;
		this.storey = storey;
		this.storeyBelow = storeyBelow;
	}	
}
