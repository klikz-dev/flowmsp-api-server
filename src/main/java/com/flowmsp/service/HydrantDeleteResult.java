package com.flowmsp.service;

import com.flowmsp.domain.location.Location;

import java.util.List;

public class HydrantDeleteResult {
    public boolean        success;
    public List<Location> conflictingLocations;

    public HydrantDeleteResult() {}

    public HydrantDeleteResult(boolean success, List<Location> conflictingLocations) {
        this.success              = success;
        this.conflictingLocations = conflictingLocations;
    }
}
