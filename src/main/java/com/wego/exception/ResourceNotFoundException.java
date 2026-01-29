package com.wego.exception;

import java.util.Objects;

/**
 * Exception thrown when a requested resource is not found.
 *
 * @contract
 *   - pre: resourceType and resourceId are not null
 *   - post: errorCode is set to "{RESOURCE_TYPE}_NOT_FOUND"
 *   - calls: BusinessException constructor
 *   - calledBy: Service layer when entity lookup fails
 */
public class ResourceNotFoundException extends BusinessException {

    /**
     * Creates a new ResourceNotFoundException.
     *
     * @contract
     *   - pre: resourceType != null, resourceId != null
     *   - post: errorCode = "{RESOURCE_TYPE}_NOT_FOUND"
     *
     * @param resourceType The type of resource (e.g., "User", "Trip")
     * @param resourceId The ID of the resource that was not found
     * @throws NullPointerException if resourceType or resourceId is null
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(
            Objects.requireNonNull(resourceType, "resourceType must not be null").toUpperCase() + "_NOT_FOUND",
            String.format("%s not found with id: %s", resourceType,
                Objects.requireNonNull(resourceId, "resourceId must not be null"))
        );
    }

    /**
     * Creates a new ResourceNotFoundException with Long ID.
     *
     * @contract
     *   - pre: resourceType != null, resourceId != null
     *   - post: errorCode = "{RESOURCE_TYPE}_NOT_FOUND"
     *
     * @param resourceType The type of resource
     * @param resourceId The ID of the resource (will be converted to String)
     * @throws NullPointerException if resourceType or resourceId is null
     */
    public ResourceNotFoundException(String resourceType, Long resourceId) {
        this(resourceType, Objects.requireNonNull(resourceId, "resourceId must not be null").toString());
    }

    /**
     * Creates a new ResourceNotFoundException with custom error code and message.
     * Use this when you need to specify the exact error code without automatic suffix.
     *
     * @contract
     *   - pre: errorCode != null, message != null
     *   - post: exception is created with provided errorCode
     *
     * @param errorCode The error code to use directly
     * @param message The error message
     * @param directErrorCode marker parameter to differentiate from other constructors
     */
    private ResourceNotFoundException(String errorCode, String message, boolean directErrorCode) {
        super(errorCode, message);
    }

    /**
     * Factory method to create a ResourceNotFoundException with a direct error code.
     *
     * @param errorCode The exact error code to use
     * @param message The error message
     * @return A new ResourceNotFoundException
     */
    public static ResourceNotFoundException withCode(String errorCode, String message) {
        return new ResourceNotFoundException(errorCode, message, true);
    }
}
