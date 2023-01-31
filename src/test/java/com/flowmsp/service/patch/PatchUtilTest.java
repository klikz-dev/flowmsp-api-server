package com.flowmsp.service.patch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

@RunWith(JUnit4.class)
public class PatchUtilTest
{
    @BeforeClass
    public static void before()
    {
        PatchUtil.intitialize(new ObjectMapper());
    }

    @Test
    public void testPatchAllowed() throws IOException, PatchNotAllowedException, NoSuchFieldException
    {
        GenericDomainObject gdo = new GenericDomainObject();
        gdo.val1 = "Test String";
        GenericDomainObject patchedGdo = PatchUtil.patch("[{\"op\":\"replace\", \"path\":\"/val1\", \"value\":\"Test String 2\"}]", gdo, GenericDomainObject.class);
        Assert.assertEquals("Test String 2", patchedGdo.val1);
    }

    @Test(expected = PatchNotAllowedException.class)
    public void testPatchNotAllowed() throws IOException, PatchNotAllowedException, NoSuchFieldException
    {
        GenericDomainObject gdo = new GenericDomainObject();
        gdo.val1 = "Test String";
        GenericDomainObject patchedGdo = PatchUtil.patch("[{\"op\":\"replace\", \"path\":\"/val2\", \"value\":\"Test String 2\"}]", gdo, GenericDomainObject.class);
        Assert.fail("Illegal patch failed to throw PatchNotAllowedException");
    }

    @Test
    public void testSubPatchAllowed() throws IOException, PatchNotAllowedException, NoSuchFieldException
    {
        GenericDomainObject gdo = new GenericDomainObject();
        gdo.val5 = new GenericDomainSubObject();
        GenericDomainObject patchedGdo = PatchUtil.patch("[{\"op\":\"add\", \"path\":\"/val5/subVal1\", \"value\":\"Test String\"}]", gdo, GenericDomainObject.class);
        Assert.assertEquals("Test String", patchedGdo.val5.subVal1);
    }

    @Test(expected = PatchNotAllowedException.class)
    public void testSubPatchNotAllowed() throws IOException, PatchNotAllowedException, NoSuchFieldException
    {
        GenericDomainObject gdo = new GenericDomainObject();
        gdo.val5 = new GenericDomainSubObject();
        GenericDomainObject patchedGdo = PatchUtil.patch("[{\"op\":\"add\", \"path\":\"/val5/subVal2\", \"value\":\"Test String\"}]", gdo, GenericDomainObject.class);
        Assert.fail("Illegal patch failed to throw PatchNotAllowedException");
    }

    @Test
    public void testMultiplePatchesAllowed() throws IOException, PatchNotAllowedException, NoSuchFieldException
    {
        GenericDomainObject gdo = new GenericDomainObject();
        GenericDomainObject patchedGdo = PatchUtil.patch(
                "[{\"op\":\"add\", \"path\":\"/val1\", \"value\":\"Test String\"}," +
                        "{\"op\":\"add\", \"path\":\"/val3\", \"value\":5}]", gdo, GenericDomainObject.class);
        Assert.assertEquals(5, patchedGdo.val3);
        Assert.assertEquals("Test String", patchedGdo.val1);
    }

    @Test(expected = PatchNotAllowedException.class)
    public void testMultipleMatchesNotAllowed() throws IOException, PatchNotAllowedException, NoSuchFieldException
    {
        GenericDomainObject gdo = new GenericDomainObject();
        GenericDomainObject patchedGdo = PatchUtil.patch(
                "[{\"op\":\"add\", \"path\":\"/val2\", \"value\":\"Test String\"}," +
                        "{\"op\":\"add\", \"path\":\"/val3\", \"value\":5}]", gdo, GenericDomainObject.class);
        Assert.fail("Illegal patch failed to throw PatchNotAllowedException");
    }

    public static class GenericDomainObject
    {
        @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
        public String val1;
        public String val2;
        @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
        public int val3;
        public int val4;
        @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
        public GenericDomainSubObject val5;
        public GenericDomainSubObject val6;
    }

    public static class GenericDomainSubObject
    {
        @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
        public String subVal1;
        public String subVal2;
    }
}
