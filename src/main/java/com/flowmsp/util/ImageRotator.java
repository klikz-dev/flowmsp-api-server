package com.flowmsp.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageRotator {

    public byte[] rotate(byte[] imageBytes) {
        Metadata metadata = null;
        BufferedImage bi = null;

        InputStream isMetadata = StreamConverter.bytesToIs(imageBytes);
        InputStream isBufferedImage = StreamConverter.bytesToIs(imageBytes);
        try {
            metadata = ImageMetadataReader.readMetadata(isMetadata);
            bi = ImageIO.read(isBufferedImage);
        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(isMetadata);
            org.apache.commons.io.IOUtils.closeQuietly(isBufferedImage);
        }

        return rotate(metadata, bi);
    }

    private byte[] rotate(Metadata metadata, BufferedImage bufferedImage) {
        try {

            ImageInformation imageInformation = readImageInformation(metadata, bufferedImage.getWidth(), bufferedImage.getHeight());

//            BufferedImage bi;
//            if (imageInformation != null) {
//                bi = transformImage(bufferedImage, getExifTransformation(imageInformation));
//            } else {
//                bi = bufferedImage;
//            }

            BufferedImage bi = transformImage(bufferedImage, getExifTransformation(imageInformation));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "jpg", baos);
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();

            return imageInByte;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static ImageInformation readImageInformation(Metadata metadata, int imageWidth, int imageHeight) throws IOException, MetadataException, ImageProcessingException {
        Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
        PngDirectory pngDirectory = metadata.getFirstDirectoryOfType(PngDirectory.class);

        int orientation = 1;
        int width = 0;
        int height = 0;

        if (directory != null)
            try {
                orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            } catch (MetadataException me) {
                // logger.warn("Could not get orientation");
            }

        try {
            if (jpegDirectory != null) {
                width = jpegDirectory.getImageWidth();
                height = jpegDirectory.getImageHeight();
            } else if (pngDirectory != null) {
                for (Tag tag : pngDirectory.getTags()) {
                    if (tag.getTagName().equals("Image Width")) {
                        width = Integer.parseInt(tag.getDescription());
                    } else if (tag.getTagName().equals("Image Height")) {
                        height = Integer.parseInt(tag.getDescription());
                    }
                }
            }
        } catch (Exception ignored) {

        }

        if (width == 0 || height == 0) {
            width = imageWidth;
            height = imageHeight;
        }
        return new ImageInformation(orientation, width, height);
    }

    public static AffineTransform getExifTransformation(ImageInformation info) {

        AffineTransform t = new AffineTransform();

        switch (info.orientation) {
            case 1:
                break;
            case 2: // Flip X
                t.scale(-1.0, 1.0);
                t.translate(-info.width, 0);
                break;
            case 3: // PI rotation
                t.translate(info.width, info.height);
                t.rotate(Math.PI);
                break;
            case 4: // Flip Y
                t.scale(1.0, -1.0);
                t.translate(0, -info.height);
                break;
            case 5: // - PI/2 and Flip X
                t.rotate(-Math.PI / 2);
                t.scale(-1.0, 1.0);
                break;
            case 6: // -PI/2 and -width
                t.translate(info.height, 0);
                t.rotate(Math.PI / 2);
                break;
            case 7: // PI/2 and Flip
                t.scale(-1.0, 1.0);
                t.translate(-info.height, 0);
                t.translate(0, info.width);
                t.rotate(3 * Math.PI / 2);
                break;
            case 8: // PI / 2
                t.translate(0, info.width);
                t.rotate(3 * Math.PI / 2);
                break;
        }

        return t;
    }

    public static BufferedImage transformImage(BufferedImage image, AffineTransform transform) throws Exception {
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);

        BufferedImage rotatedSizes = op.createCompatibleDestImage(image, image.getColorModel());
        BufferedImage destinationImage = new BufferedImage(rotatedSizes.getWidth(), rotatedSizes.getHeight(), image.getType());
        op.filter(image, destinationImage);
        return destinationImage;
    }

    // Inner class containing image information
    public static class ImageInformation {
        public final int orientation;
        public final int width;
        public final int height;

        public ImageInformation(int orientation, int width, int height) {
            this.orientation = orientation;
            this.width = width;
            this.height = height;
        }

        public String toString() {
            return String.format("%dx%d,%d", this.width, this.height, this.orientation);
        }
    }

}
