package com.hubz.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadPath;
    private static final String PROFILE_PHOTOS_DIR = "profile-photos";
    private static final String TASK_ATTACHMENTS_DIR = "task-attachments";
    private static final String ORGANIZATION_LOGOS_DIR = "organization-logos";
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final List<String> ALLOWED_DOCUMENT_EXTENSIONS = Arrays.asList(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".csv", ".zip", ".rar", ".7z",
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg",
            ".mp4", ".mp3", ".wav"
    );
    private static final long MAX_PROFILE_PHOTO_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_ORGANIZATION_LOGO_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_ATTACHMENT_SIZE = 25 * 1024 * 1024; // 25MB

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
            Files.createDirectories(this.uploadPath.resolve(PROFILE_PHOTOS_DIR));
            Files.createDirectories(this.uploadPath.resolve(TASK_ATTACHMENTS_DIR));
            Files.createDirectories(this.uploadPath.resolve(ORGANIZATION_LOGOS_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public String storeFile(MultipartFile file, UUID noteId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File must have a name");
        }

        // Generate unique filename
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        String fileName = UUID.randomUUID() + extension;

        // Create note-specific directory
        Path noteDir = this.uploadPath.resolve(noteId.toString());
        Files.createDirectories(noteDir);

        // Store file
        Path targetLocation = noteDir.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return noteId.toString() + "/" + fileName;
    }

    /**
     * Store a profile photo for a user.
     *
     * @param file the uploaded file
     * @param userId the user's ID
     * @return the relative path to the stored file
     * @throws IOException if file storage fails
     * @throws IllegalArgumentException if the file is invalid
     */
    public String storeProfilePhoto(MultipartFile file, UUID userId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File must have a name");
        }

        // Validate file size
        if (file.getSize() > MAX_PROFILE_PHOTO_SIZE) {
            throw new IllegalArgumentException("Profile photo must be less than 5MB");
        }

        // Validate file extension
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }

        // Generate unique filename using userId to make it easy to find/replace
        String fileName = userId.toString() + extension;

        // Store in profile-photos directory
        Path targetLocation = this.uploadPath.resolve(PROFILE_PHOTOS_DIR).resolve(fileName);

        // Delete any existing profile photo for this user (might have different extension)
        deleteExistingProfilePhotos(userId);

        // Store file
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return PROFILE_PHOTOS_DIR + "/" + fileName;
    }

    /**
     * Delete all existing profile photos for a user (handles different extensions).
     *
     * @param userId the user's ID
     */
    private void deleteExistingProfilePhotos(UUID userId) throws IOException {
        Path profilePhotosDir = this.uploadPath.resolve(PROFILE_PHOTOS_DIR);
        if (Files.exists(profilePhotosDir)) {
            for (String ext : ALLOWED_IMAGE_EXTENSIONS) {
                Path existingFile = profilePhotosDir.resolve(userId.toString() + ext);
                Files.deleteIfExists(existingFile);
            }
        }
    }

    /**
     * Delete a user's profile photo.
     *
     * @param userId the user's ID
     * @throws IOException if deletion fails
     */
    public void deleteProfilePhoto(UUID userId) throws IOException {
        deleteExistingProfilePhotos(userId);
    }

    /**
     * Store a logo for an organization.
     *
     * @param file the uploaded file
     * @param organizationId the organization's ID
     * @return the relative path to the stored file
     * @throws IOException if file storage fails
     * @throws IllegalArgumentException if the file is invalid
     */
    public String storeOrganizationLogo(MultipartFile file, UUID organizationId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File must have a name");
        }

        // Validate file size
        if (file.getSize() > MAX_ORGANIZATION_LOGO_SIZE) {
            throw new IllegalArgumentException("Organization logo must be less than 5MB");
        }

        // Validate file extension
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: " + String.join(", ", ALLOWED_IMAGE_EXTENSIONS));
        }

        // Generate unique filename using organizationId to make it easy to find/replace
        String fileName = organizationId.toString() + extension;

        // Store in organization-logos directory
        Path targetLocation = this.uploadPath.resolve(ORGANIZATION_LOGOS_DIR).resolve(fileName);

        // Delete any existing logo for this organization (might have different extension)
        deleteExistingOrganizationLogos(organizationId);

        // Store file
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return ORGANIZATION_LOGOS_DIR + "/" + fileName;
    }

    /**
     * Delete all existing logos for an organization (handles different extensions).
     *
     * @param organizationId the organization's ID
     */
    private void deleteExistingOrganizationLogos(UUID organizationId) throws IOException {
        Path logosDir = this.uploadPath.resolve(ORGANIZATION_LOGOS_DIR);
        if (Files.exists(logosDir)) {
            for (String ext : ALLOWED_IMAGE_EXTENSIONS) {
                Path existingFile = logosDir.resolve(organizationId.toString() + ext);
                Files.deleteIfExists(existingFile);
            }
        }
    }

    /**
     * Delete an organization's logo.
     *
     * @param organizationId the organization's ID
     * @throws IOException if deletion fails
     */
    public void deleteOrganizationLogo(UUID organizationId) throws IOException {
        deleteExistingOrganizationLogos(organizationId);
    }

    /**
     * Store an attachment for a task.
     *
     * @param file the uploaded file
     * @param taskId the task's ID
     * @return the relative path to the stored file
     * @throws IOException if file storage fails
     * @throws IllegalArgumentException if the file is invalid
     */
    public String storeTaskAttachment(MultipartFile file, UUID taskId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File must have a name");
        }

        // Validate file size
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new IllegalArgumentException("Attachment must be less than 25MB");
        }

        // Validate file extension
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        if (!extension.isEmpty() && !ALLOWED_DOCUMENT_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: " + String.join(", ", ALLOWED_DOCUMENT_EXTENSIONS));
        }

        // Generate unique filename
        String fileName = UUID.randomUUID() + extension;

        // Create task-specific directory
        Path taskDir = this.uploadPath.resolve(TASK_ATTACHMENTS_DIR).resolve(taskId.toString());
        Files.createDirectories(taskDir);

        // Store file
        Path targetLocation = taskDir.resolve(fileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return TASK_ATTACHMENTS_DIR + "/" + taskId.toString() + "/" + fileName;
    }

    public Path getFilePath(String filePath) {
        return this.uploadPath.resolve(filePath).normalize();
    }

    public void deleteFile(String filePath) throws IOException {
        Path file = getFilePath(filePath);
        Files.deleteIfExists(file);
    }
}
