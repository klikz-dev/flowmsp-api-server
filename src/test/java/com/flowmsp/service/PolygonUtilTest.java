package com.flowmsp.service;

import com.google.common.collect.Lists;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PolygonUtilTest {
    @Test
    public void testCartesianArea() {
        // Unit square
        Polygon p1 = new Polygon(Lists.newArrayList(new Position(0,0),
                new Position(1,0),
                new Position(1,1),
                new Position(0,1),
                new Position(0,0)));
        Assert.assertEquals(1.0, PolygonUtil.areaCartesianPolygon(p1), 0.00001);

        // L-Shape
        Polygon p2 = new Polygon(Lists.newArrayList(new Position(0,0),
                                                    new Position(2,0),
                                                    new Position(2,1),
                                                    new Position(1,1),
                                                    new Position(1,2),
                                                    new Position(0,2),
                                                    new Position(0,0)));
        Assert.assertEquals(3.0, PolygonUtil.areaCartesianPolygon(p2), 0.00001);
    }


    @Test
    public void testLatLonArea() {
        // 100ft per side square
        Polygon p1 = new Polygon(Lists.newArrayList(
                new Position(88.0,44.0),
                new Position(88.00038002,44.0),
                new Position(88.00038002,44.00027432),
                new Position(88.0,44.00027432),
                new Position(88.0,44.0)));
        Assert.assertEquals(10000.0, PolygonUtil.areaLatLonPolygon(p1), 10.0);
    }

    @Test
    public void testCartesianConversion() {
        Polygon p1 = new Polygon(Lists.newArrayList(
                new Position(88.0,44.0),
                new Position(88.00038002,44.0),
                new Position(88.00038002,44.00027432),
                new Position(88.0,44.00027432),
                new Position(88.0,44.0)));

        Polygon p2 = PolygonUtil.cartesianConversion(p1);
        Assert.assertEquals(0.0, p2.getExterior().get(0).getValues().get(0), .1);
        Assert.assertEquals(0.0, p2.getExterior().get(0).getValues().get(1), .1);

        Assert.assertEquals(30.48, p2.getExterior().get(1).getValues().get(0), .1);
        Assert.assertEquals(0.0, p2.getExterior().get(1).getValues().get(1), .1);

        Assert.assertEquals(30.48, p2.getExterior().get(2).getValues().get(0), .1);
        Assert.assertEquals(30.48, p2.getExterior().get(2).getValues().get(1), .1);

        Assert.assertEquals(0.0, p2.getExterior().get(3).getValues().get(0), .1);
        Assert.assertEquals(30.48, p2.getExterior().get(3).getValues().get(1), .1);

        Assert.assertEquals(0.0, p2.getExterior().get(4).getValues().get(0), .1);
        Assert.assertEquals(0.0, p2.getExterior().get(4).getValues().get(1), .1);

    }
}
