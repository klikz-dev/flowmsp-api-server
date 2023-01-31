package com.flowmsp.domain.dispatch;

import com.flowmsp.service.patch.AllowedPatches;
import com.flowmsp.service.patch.PatchOp;
import org.bson.codecs.pojo.annotations.BsonId;

public class DispatchReadStatus {
    @BsonId
    public String id;
    public String userId;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public String lastReadDispatchId;
    @AllowedPatches({PatchOp.ADD, PatchOp.REMOVE, PatchOp.REPLACE})
    public Long lastReadDispatchSequence;

    public DispatchReadStatus() {
    }

    public DispatchReadStatus(String id, String userId) {
        this.id = id;
        this.userId = userId;
    }

    public DispatchReadStatus(String id, String userId, String lastReadDispatchId, Long lastReadDispatchSequence) {
        this.id = id;
        this.userId = userId;
        this.lastReadDispatchId = lastReadDispatchId;
        this.lastReadDispatchSequence = lastReadDispatchSequence;
    }
}
