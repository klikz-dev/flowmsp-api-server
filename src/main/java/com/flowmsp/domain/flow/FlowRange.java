package com.flowmsp.domain.flow;

public class FlowRange {
    public String   label;
    public PinColor pinColor;

    public FlowRange() {}

    public FlowRange(String label, PinColor pinColor) {
        this.label    = label;
        this.pinColor = pinColor;
    }
}