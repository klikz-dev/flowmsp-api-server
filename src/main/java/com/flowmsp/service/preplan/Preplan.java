package com.flowmsp.service.preplan;

import com.flowmsp.domain.hydrant.HydrantRef;
import com.flowmsp.domain.location.LocationRef;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;

import java.util.ArrayList;
import java.util.List;

public class Preplan {
    public LocationRef      location;
    public Polygon          geoOutline;
    public int				storey;
    public int				storeyBelow;
    public Long             roofArea;
    public Long             totalArea;
    public Long             requiredFlow;
    public Point            planningCenter;
    public List<HydrantRef> hydrants = new ArrayList<>();
}