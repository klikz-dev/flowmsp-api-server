package com.flowmsp.controller.location;

import com.flowmsp.domain.location.Image;
import org.jooq.lambda.function.Function2;
import spark.Request;

public interface ImageResponseBuilder {

    static ImageResponse build(Request request, Image image) {
        ImageResponse ir = new ImageResponse();
        if(image != null) {
            ir.id = image.id;
            ir.title = image.title;
            ir.description = image.description;
            ir.originalFileName = image.originalFileName;
            ir.sanitizedFileName = image.sanitizedFileName;
            ir.href = image.href;
            ir.hrefThumbnail = image.hrefThumbnail;
            ir.hrefOriginal = image.hrefOriginal;
            ir.hrefAnnotated = image.hrefAnnotated;
            ir.annotationMetadata = image.annotationMetadata;
            ir.annotationSVG = image.annotationSVG;
            ir.tags = image.tags;
        }
        return ir;
    }

    Function2<Request, Image, ImageResponse> responseBuider = ImageResponseBuilder::build;
}
