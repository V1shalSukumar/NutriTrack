package com.nutritrack.util;

import java.io.*;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

public final class FoodPhotos {
  public static final int MAX_BYTES = 5 * 1024 * 1024;

  /** Decode actual image content and re-encode PNG, discarding metadata and original filename. */
  public static byte[] validate(byte[] input) throws IOException {
    if (input == null || input.length == 0) return null;
    if (input.length > MAX_BYTES)
      throw new IllegalArgumentException("Choose a JPG or PNG image smaller than 5 MB.");
    try (ImageInputStream stream =
        ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext())
        throw new IllegalArgumentException("The attachment is not a valid JPG or PNG image.");
      var reader = readers.next();
      try {
        String format = reader.getFormatName();
        if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg"))
          throw new IllegalArgumentException("Please attach a JPG or PNG image.");
        reader.setInput(stream);
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width < 1
            || height < 1
            || width > 6000
            || height > 6000
            || (long) width * height > 12000000)
          throw new IllegalArgumentException(
              "Choose an image up to 12 megapixels and 6,000 pixels per side.");
        var image = reader.read(0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        if (out.size() > MAX_BYTES)
          throw new IllegalArgumentException(
              "This image is too large after processing. Choose a smaller image.");
        return out.toByteArray();
      } finally {
        reader.dispose();
      }
    } catch (javax.imageio.IIOException e) {
      throw new IllegalArgumentException("The image could not be read. Choose another JPG or PNG.");
    }
  }
}
