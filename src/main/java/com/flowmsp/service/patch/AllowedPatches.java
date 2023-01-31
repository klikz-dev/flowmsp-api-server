package com.flowmsp.service.patch;

import java.lang.annotation.*;

/**
 * Allows the given patch operations on the annotated fields.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface AllowedPatches
{
    PatchOp[] value();
}
