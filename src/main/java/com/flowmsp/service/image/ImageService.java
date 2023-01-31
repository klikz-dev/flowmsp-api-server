package com.flowmsp.service.image;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.flowmsp.db.LocationDao;
import com.flowmsp.domain.location.Image;
import com.flowmsp.domain.location.Location;
import com.flowmsp.util.ImageRotator;
import com.flowmsp.util.StreamConverter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Functions associated with the physical management and storage of images associated with locations. Images are
 * stored on AmazonS3.
 */
public class ImageService {
    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private final AmazonS3    s3;
    private final String      s3UrlRoot;
    private final String      imageBucket;
    private final LocationDao locationDao;

    private AWSLambda lambdaClient;

    private ImageRotator imageRotator = new ImageRotator();

    public ImageService(AmazonS3 s3, String s3UrlRoot, String imageBucket, LocationDao locationDao) {
        this.s3          = s3;
        this.s3UrlRoot   = s3UrlRoot;
        this.imageBucket = imageBucket;
        this.locationDao = locationDao;
        
        try
		{
    		lambdaClient = AWSLambdaClientBuilder.defaultClient();
		} catch (Exception ex){
			log.error("Error initializing lambda client ", ex);
		}
    }

    public void deleteImage(String href) {
        s3.deleteObject(imageBucket, stripRootAndBucket(href));
    }

    private String stripRootAndBucket(String href) {
        String prefix = String.format("%s/%s/", s3UrlRoot, imageBucket);
        return href.replace(prefix, "");
    }

    /**
     * Upload an image, copying it to S3 after sanitizing the filename and creating the associated entry in the
     * database.
     */
    public Image uploadImage(InputStream iStream, String customerSlug, String locationId, String filename, String userName) {
        String imageId = UUID.randomUUID().toString();
        Tuple2<String, String> splitFilename = splitFilename(filename);
        String namePart = splitFilename.v1;
        String extPart = splitFilename.v2;

        byte[] bytes = StreamConverter.isToBytes(iStream);

        if (validFileExtension(extPart)) {
            Optional<Location> l = locationDao.getById(locationId);
            if (l.isPresent()) {
                boolean isPDF = false;
                if (extPart.equalsIgnoreCase("pdf")) {
                    isPDF = true;
                } else if (extPart.equalsIgnoreCase("png")) {
                    InputStream is = StreamConverter.bytesToIs(bytes);
                    try {
                        BufferedImage bi = ImageIO.read(is);
                        BufferedImage newBufferedImage = new BufferedImage(bi.getWidth(),
                                bi.getHeight(), BufferedImage.TYPE_INT_RGB);
                        newBufferedImage.createGraphics().drawImage(bi, 0, 0, Color.WHITE, null);
                        try (ByteArrayOutputStream bo = new ByteArrayOutputStream()) {
                            ImageIO.write(newBufferedImage, "jpg", bo);
                            bytes = bo.toByteArray();
                        }
                        extPart = "jpg";
                    } catch (Exception e1) {
                        log.error("Error jpg conversion ", e1);
                    } finally {
                        org.apache.commons.io.IOUtils.closeQuietly(is);
                    }
                } else if (extPart.equalsIgnoreCase("jpeg")) {
                    extPart = "jpg";
                }else if (extPart.equalsIgnoreCase("bmp")) {
                    extPart = "jpg";
                }

                namePart = namePart.concat("_").concat(imageId);
                String sanitizedFilename = namePart.concat(".").concat(extPart);
                sanitizedFilename = sanitizeFilename(sanitizedFilename);

                Tuple2<String, String> splitSanitizedFilename = splitFilename(sanitizedFilename);
                String normalImageFilename = splitSanitizedFilename.v1.concat("-1024h").concat(".").concat(splitSanitizedFilename.v2);
                String thumbnailImageFilename = splitSanitizedFilename.v1.concat("-300h").concat(".").concat(splitSanitizedFilename.v2);

                if (!isPDF) {
                    bytes = imageRotator.rotate(bytes);
                }

                InputStream is = StreamConverter.bytesToIs(bytes);
                try {
                    ObjectMetadata cmd = new ObjectMetadata();
                    String key = String.format("originals/%s/%s/%s", customerSlug, locationId, sanitizedFilename);

                    if (isPDF) {
                        PutObjectRequest por = new PutObjectRequest(imageBucket, key, is, cmd);
                        por.setCannedAcl(CannedAccessControlList.PublicRead);
                        s3.putObject(por);
                    } else {
                        s3.putObject(imageBucket, key, is, cmd);
                    }
                } catch (Exception e) {
                    log.error("Error writing file to S3", e);
                    return null;
                } finally {
                    org.apache.commons.io.IOUtils.closeQuietly(is);
                }

                Image i = new Image();
                i.id = imageId;
                i.originalFileName = filename;
                i.sanitizedFileName = sanitizedFilename;
                i.hrefOriginal = String.format("%s/%s/originals/%s/%s/%s", s3UrlRoot, imageBucket, customerSlug, locationId, sanitizedFilename);
                i.href = String.format("%s/%s/processed/%s/%s/%s", s3UrlRoot, imageBucket, customerSlug, locationId, normalImageFilename);
                i.hrefThumbnail = String.format("%s/%s/processed/%s/%s/%s", s3UrlRoot, imageBucket, customerSlug, locationId, thumbnailImageFilename);
                i.title = filename;
                if (isPDF) {
                    i.href = i.hrefOriginal;
                    i.hrefThumbnail = "NoImage";
                }
                Bson newValue = new Document("images", i);
                Bson updateOperationDocument = new Document("$push", newValue);
                locationDao.updateById(locationId, updateOperationDocument);

                Optional<Location> l2 = locationDao.getById(locationId);

                Location loc = l2.get();

                Date now = new Date();

                String timeStamp = new SimpleDateFormat("MM-dd-yyyy HH.mm.ss").format(now);

                loc.building.lastReviewedOn = timeStamp;
                loc.building.lastReviewedBy = userName;

                locationDao.replaceById(locationId, loc);

                return i;
            } else {
                log.error("Unable to retrieve location with id {}", locationId);
                return null;
            }
        } else {
            log.error("Invalid image file format for file {}", filename);
            return null;
        }
    }


    public Image annotateImage(String customerSlug, String locationId, String imageId, String annotationJSON, String annotationSVG, String userName) {
        Optional<Location> l = locationDao.getById(locationId);
        if (l.isPresent()) {
            Location location = l.get();
            Image image = null;
            String origThumbNailImage = "";
            for (Image i : location.images) {
                if (i.id.equals(imageId)) {
                    image = i;
                    break;
                }
            }

            if (image != null) {
                if (image.annotationMetadata != null && annotationJSON != null) {
                    if (image.annotationMetadata.equalsIgnoreCase(annotationJSON)) {
                        //There is no change. Why should we do all these operations
                        return image;
                    }
                }
                origThumbNailImage = image.hrefThumbnail;
                image.annotationMetadata = annotationJSON;
                image.annotationSVG = annotationSVG;
                image.hrefAnnotated = String.format("%s/%s/annotated/%s/%s/%s.png", s3UrlRoot, imageBucket, customerSlug, locationId, imageId);
                image.hrefThumbnail = String.format("%s/%s/annotated/%s/%s/%s-300h.png", s3UrlRoot, imageBucket, customerSlug, locationId, imageId);

                try {
                    log.info("Calling the annotate image function with annotation data: {}", annotationJSON);

                    InvokeRequest invokeRequest = new InvokeRequest();
                    // invokeRequest.setFunctionName("createdAnnotatedImage");
                    //invokeRequest.setFunctionName("createAnnotatedImage_new");
                    invokeRequest.setFunctionName("createAnnotatedImageAndThumbnail");

                    String payloadString = String.format("{\"slug\": \"%s\",\n" +
                                    "    \"locationId\": \"%s\",\n" +
                                    "    \"imageId\": \"%s\",\n" +
                                    "    \"annotationMetadataJSON\": %s" +
                                    " }"
                            , customerSlug, locationId, imageId, annotationJSON);
                    invokeRequest.setPayload(payloadString);
                    if (lambdaClient != null) {
                        lambdaClient.invoke(invokeRequest);
                    }

                    //InvokeRequest invokeRequest1 = new InvokeRequest();
                    // invokeRequest1.setFunctionName("createAnnotatedThumbnail");
                    //invokeRequest1.setFunctionName("createAnnotatedThumbnail_new");
                    //String payloadString1 = String.format("{\"slug\": \"%s\",\n" +
                    //                "    \"locationId\": \"%s\",\n" +
                    //                "    \"imageId\": \"%s\"" +
                    //                " }"
                    //       , customerSlug, locationId, imageId);
                    //invokeRequest1.setPayload(payloadString1);
                    //if (lambdaClient != null) {
                    //    lambdaClient.invoke(invokeRequest1);
                    //}

                    Date now = new Date();

                    String timeStamp = new SimpleDateFormat("MM-dd-yyyy HH.mm.ss").format(now);

                    log.info("before update");
                    log.info("lastReviewedOn: ", location.building.lastReviewedOn);
                    log.info("lastReviewedBy: ", location.building.lastReviewedBy);

                    location.building.lastReviewedOn = timeStamp;
                    location.building.lastReviewedBy = userName;
                    locationDao.replaceById(locationId, location);

                    //Delete originil thumbnail
                    if (origThumbNailImage.length() > 0) {
                        if (!origThumbNailImage.equalsIgnoreCase(image.hrefThumbnail)) {
                            deleteImage(origThumbNailImage);
                        }
                    }
                    log.info("Completed the annotate image function");

                    return image;
                } catch (Exception e) {
                    log.error("Error rendering annotated image", e);
                    return null;
                }
            } else {
                log.error("Unable to find image with id {}", imageId);
                return null;
            }
        } else {
            log.error("Unable to find location with id {}", locationId);
            return null;
        }
    }

    public Image tagImage(String customerSlug, String locationId, String imageId, ArrayList<String> tags, String userName) {
        Optional<Location> l = locationDao.getById(locationId);
        if (l.isPresent()) {
            Location location = l.get();
            Image image = null;
            for (Image i : location.images) {
                if (i.id.equals(imageId)) {
                    image = i;
                    break;
                }
            }

            if (image != null) {
                image.tags = tags;
                Date now = new Date();

                String timeStamp = new SimpleDateFormat("MM-dd-yyyy HH.mm.ss").format(now);

                location.building.lastReviewedBy = userName;
                location.building.lastReviewedOn = timeStamp;
                locationDao.replaceById(locationId, location);
                return image;
            } else {
                log.error("Unable to find image with id {}", imageId);
                return null;
            }
        } else {
            log.error("Unable to find location with id {}", locationId);
            return null;
        }
    }

    /**
     * String the given filename into its name and extension parts. For names that have more than one component,
     * separated by spaces, only the rightmost of the components is considered the extension.
     *
     * @param filename The filename to split
     * @return A Tuple with the base filename in the v1 element of the tuple and the extension, formatted
     * to lowercase, in the v2 element
     */
    public static Tuple2<String, String> splitFilename(String filename) {
        String namePart = null;
        String extPart = null;

        if (filename != null) {
            String[] splits = filename.split("\\.");
            if (splits.length > 1) {
                String[] nameParts = Arrays.copyOf(splits, splits.length - 1);
                namePart = Lists.newArrayList(nameParts).stream().collect(Collectors.joining("."));
                extPart = splits[splits.length - 1].toLowerCase();
            } else {
                namePart = filename;
            }
        }
        log.debug("Split filename {} into name part {} and extension {}", filename, namePart, extPart);
        return new Tuple2<>(namePart, extPart);
    }

    private static final Set<String> allowedExtensions = Sets.newHashSet("jpg", "jpeg", "bmp", "png", "pdf");

    public static boolean validFileExtension(String extension) {
        return allowedExtensions.contains(extension.toLowerCase());
    }

    /**
     * Gets rid of characters in the filename that aren't allowed on S3. These characters are replaced with the
     * underscore.
     *
     * @param filename  The filename to sanitize
     * @return          The sanitized version of the filename
     */
    private static String sanitizeFilename(String filename) {
        String sanitized = filename.replaceAll("[^a-zA-Z0-9_.]", "_");
        log.debug("Sanitized filename for upload from {} to {}", filename, sanitized);
        return sanitized;
    }

}
