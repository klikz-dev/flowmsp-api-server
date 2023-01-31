package com.flowmsp.controller.hydrant;

import com.flowmsp.controller.location.LocationResponse;
import com.flowmsp.domain.hydrant.Hydrant;
import com.flowmsp.domain.location.Location;
import com.flowmsp.service.HydrantDeleteResult;
import org.jooq.lambda.function.Function2;
import spark.Request;

import java.util.List;
import java.util.stream.Collectors;

public class HydrantDeleteResponse {
    public boolean                success;
    public List<LocationResponse> conflictingLocations = null;

    private static Function2<Request, Location, LocationResponse> locationResponseBuilder = LocationResponse::build;
    private static Function2<Request, Hydrant, HydrantResponse> hydrantResponseBuilder = HydrantResponse::build;

    public static HydrantDeleteResponse build(Request req, HydrantDeleteResult hdr) {
        HydrantDeleteResponse resp = new HydrantDeleteResponse();
        if(hdr != null) {
            resp.success = hdr.success;

            if(hdr.conflictingLocations != null && hdr.conflictingLocations.size() > 0) {
                resp.conflictingLocations = hdr.conflictingLocations                							
                                               .stream()
                                               .limit(2)
                                               .map(locationResponseBuilder.applyPartially(req))
                                               .collect(Collectors.toList());
            }
            resp.buildLinks(req);
        }
        return resp;
    }

    public void buildLinks(Request req) {

    }
}
