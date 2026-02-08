# =============================================================================
# Hubz Frontend - Production Dockerfile
# Multi-stage build for optimized production image
# =============================================================================

# --- Stage 1: Build ---
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files first to leverage Docker cache
COPY package.json package-lock.json ./

# Install dependencies (clean install for reproducible builds)
RUN npm ci --silent

# Copy source code
COPY . .

# Build arguments for environment configuration
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL

# Build the application
RUN npm run build

# --- Stage 2: Runtime with Nginx ---
FROM nginx:alpine

# Add non-root user for security (nginx runs as nginx user by default)
# Remove default nginx config
RUN rm -rf /etc/nginx/conf.d/default.conf

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Create cache directory with proper permissions
RUN mkdir -p /var/cache/nginx && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/health || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
