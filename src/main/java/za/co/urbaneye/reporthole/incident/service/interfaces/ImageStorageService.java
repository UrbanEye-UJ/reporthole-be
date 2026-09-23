package za.co.urbaneye.reporthole.incident.service.interfaces;

public interface ImageStorageService {
    String saveBase64Image(String base64Image);

    /**
     * Loads the bytes of a previously stored image from the URL {@link #saveBase64Image} returned.
     *
     * @throws java.io.UncheckedIOException if the file is missing or unreadable
     */
    byte[] readImage(String imageUrl);
}
