package com.hubz.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for Hubz API documentation.
 * <p>
 * This configuration provides interactive API documentation accessible at /swagger-ui.html
 * and raw OpenAPI specification at /api-docs.
 * <p>
 * Security: All endpoints (except /api/auth/**) require JWT Bearer token authentication.
 * Include the token in the "Authorize" button in Swagger UI.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Hubz API",
                version = "1.0.0",
                description = """
                        Hubz is a productivity application for managing organizations, teams, tasks, goals, and personal habits.

                        ## Authentication
                        Most endpoints require JWT authentication. To authenticate:
                        1. Use POST /api/auth/login with your credentials
                        2. Copy the token from the response
                        3. Click the "Authorize" button and enter: Bearer {your-token}

                        ## Main Features
                        - **Organizations**: Create and manage organizations with teams and members
                        - **Tasks**: Task management with Kanban, priorities, and assignments
                        - **Goals**: Track short, medium, and long-term objectives
                        - **Events**: Calendar management with recurring events support
                        - **Notes**: Rich-text notes with versioning and collaboration
                        - **Habits**: Personal habit tracking with streaks and analytics

                        ## Error Responses
                        All errors follow a standard format with message and error code.
                        Common HTTP status codes: 400 (Bad Request), 401 (Unauthorized), 403 (Forbidden), 404 (Not Found)
                        """,
                contact = @Contact(
                        name = "Hubz Team",
                        email = "support@hubz.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8085", description = "Development Server"),
                @Server(url = "https://api.hubz.com", description = "Production Server")
        },
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {
                @Tag(name = "Authentication", description = "User authentication and account management"),
                @Tag(name = "Users", description = "User profile and settings management"),
                @Tag(name = "Organizations", description = "Organization CRUD and member management"),
                @Tag(name = "Tasks", description = "Task management with Kanban support"),
                @Tag(name = "Goals", description = "Goal tracking and progress management"),
                @Tag(name = "Events", description = "Calendar events and scheduling"),
                @Tag(name = "Notes", description = "Rich-text notes with versioning"),
                @Tag(name = "Habits", description = "Personal habit tracking"),
                @Tag(name = "Teams", description = "Team management within organizations"),
                @Tag(name = "Documents", description = "Document storage and versioning"),
                @Tag(name = "Analytics", description = "Productivity analytics and reporting"),
                @Tag(name = "Notifications", description = "In-app and email notifications")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT authentication token. Obtain it from POST /api/auth/login"
)
public class OpenApiConfig {
    // Configuration is handled through annotations
}
