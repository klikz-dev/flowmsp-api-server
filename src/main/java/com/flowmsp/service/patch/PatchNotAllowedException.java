package com.flowmsp.service.patch;

public class PatchNotAllowedException extends Exception
{
    private Class<?> clazz;
    private PatchOp op;
    private String jsonPointer;

    PatchNotAllowedException(Class<?> clazz, PatchOp op, String jsonPointer)
    {
        this.clazz = clazz;
        this.op = op;
        this.jsonPointer = jsonPointer;
    }

    @Override
    public String getMessage()
    {
        return "Operation " + op + " not allowed on pointer " + jsonPointer + " for class " + clazz.getName();
    }
}
