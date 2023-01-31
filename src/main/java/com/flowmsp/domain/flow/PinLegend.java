package com.flowmsp.domain.flow;

import java.util.List;

public class PinLegend {
    public static class FlowRange {
        public String  label;
        public Integer low;
        public Integer high;
        public PinColor pinColor;
        public FlowRange(){}
        public FlowRange(String label, Integer low, Integer high, PinColor pinColor) {
            this.label    = label;
            this.low      = low;
            this.high     = high;
            this.pinColor = pinColor;
        }
    }

    public FlowRange       unknownPinColor;
    public List<FlowRange> rangePinColors;
}
