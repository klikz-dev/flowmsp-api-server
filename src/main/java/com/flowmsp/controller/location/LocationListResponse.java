package com.flowmsp.controller.location;

import com.flowmsp.controller.BaseListResponse;
import com.flowmsp.controller.hydrant.HydrantResponse;
import spark.Request;

public class LocationListResponse extends BaseListResponse<LocationResponse> {


    @Override
    public void buildLinks(Request req) {

    }
}
