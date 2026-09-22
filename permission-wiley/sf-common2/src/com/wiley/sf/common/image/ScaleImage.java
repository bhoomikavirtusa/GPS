package com.wiley.sf.common.image;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.wiley.sf.common.lang.ArgUtil;

/**
 * Provides methods for image scaling.
 *
 * @since  JDK 1.6
 * @version $Id: ScaleImage.java,v 1.5 2013-12-18 23:33:39 smarkoff Exp $
 * @author Steve Markoff
 */
public class ScaleImage {

    /**
     * If the given image has dimensions no larger than maxWidth and maxHeight,
     * returns the image unchanged. Otherwise returns an image scaled down to fit
     * inside a box of maxWidth and maxHeight, maintaining the original aspect ratio
     * (width proportional to height).
     *
     * @param image  Must be non-null
     * @param maxWidth  Must be at least 1
     * @param maxHeight  Must be at least 1
     * @param interpolationType  Must be one of the AffineTransformOp constants
     * @param outputFormatName  Generally "jpg", "png", or "gif"
     */
    public static byte [] scaleToFitBox(byte [] image, int maxWidth, int maxHeight, int interpolationType, String outputFormatName)
    throws IOException
    {
        ArgUtil.notNull(image, "image");
        ArgUtil.notLess1(maxWidth, "maxWidth");
        ArgUtil.notLess1(maxHeight, "maxHeight");

        ByteArrayInputStream baIn = new ByteArrayInputStream(image);
        BufferedImage bi = ImageIO.read(baIn);  // throws IOException
        BufferedImage bi2 = scaleToFitBox(bi, maxWidth, maxHeight, interpolationType);
        ByteArrayOutputStream baOut = new ByteArrayOutputStream();
        ImageIO.write(bi2, outputFormatName, baOut);  // throws IOException
        return baOut.toByteArray();
    }

    /**
     * If the given image has dimensions no larger than maxWidth and maxHeight,
     * returns the image unchanged. Otherwise returns an image scaled down to fit
     * inside a box of maxWidth and maxHeight, maintaining the original aspect ratio
     * (width proportional to height).
     *
     * @param bi  Must be non-null
     * @param maxWidth  Must be at least 1
     * @param maxHeight  Must be at least 1
     * @param interpolationType  Must be one of the AffineTransformOp constants
     */
    public static BufferedImage scaleToFitBox(BufferedImage bi, int maxWidth, int maxHeight, int interpolationType) {
        ArgUtil.notNull(bi, "bi");
        ArgUtil.notLess1(maxWidth, "maxWidth");
        ArgUtil.notLess1(maxHeight, "maxHeight");

        int width = bi.getWidth();
        int height = bi.getHeight();
        if (width <= maxWidth && height <= maxHeight) {
            return bi;
        }

        double widthScale = ((double) maxWidth) / width;
        double heightScale = ((double) maxHeight) / height;
        double minScale = widthScale < heightScale ? widthScale : heightScale;

        return scaleAffine(bi, minScale, interpolationType);
    }

    /**
     * Scale the input image by the specified amount in both dimensions. A scale
     * of 1.0 will have no effect.
     *
     * @param bi  Must be non-null
     * @param scale  Should be greater than 0
     * @param interpolationType  Must be one of the AffineTransformOp constants
     */
    public static BufferedImage scaleAffine(BufferedImage bi, double scale,
            int interpolationType) {
        ArgUtil.notNull(bi, "bi");

        AffineTransform af = AffineTransform.getScaleInstance(scale, scale);
        AffineTransformOp op = new AffineTransformOp(af, interpolationType);
        return op.filter(bi, null);
    }
}