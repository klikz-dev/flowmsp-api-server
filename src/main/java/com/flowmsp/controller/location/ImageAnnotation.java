package com.flowmsp.controller.location;

public class ImageAnnotation {
	public Object annotationJSON;
	public Object annotationSVG;
	
	public ImageAnnotation() {
		
	}
	
	public ImageAnnotation(Object annotationJSON, Object annotationSVG) {
		this.annotationJSON = annotationJSON;
		this.annotationSVG = annotationSVG;
	}
}
