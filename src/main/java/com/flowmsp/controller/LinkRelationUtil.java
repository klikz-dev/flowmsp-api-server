package com.flowmsp.controller;

import java.util.List;
import java.util.Optional;

public class LinkRelationUtil {
    public static Optional<LinkRelation> getByRelation(String rel, List<LinkRelation> links) {
        return links.stream()
                    .filter(lr -> lr.rel.equals(rel))
                    .findFirst();
    }
}
