package com.flowmsp.service.MessageParser;

import com.flowmsp.domain.psap.PSAP;
import com.flowmsp.service.Message.MessageService;
import com.flowmsp.service.psap.PSAPService;

public interface PSAPMsgParser extends MsgParser {
    public ParsedMessage Process(PSAP psap, String msg, MessageService msgService, PSAPService psapService);
}
