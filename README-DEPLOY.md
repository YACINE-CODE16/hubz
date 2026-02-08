# Hubz - Production Deployment Guide

This guide explains how to deploy Hubz in a production environment using Docker.

## Prerequisites

- Docker 20.10+ with Docker Compose V2
- At least 4GB RAM available for containers
- Ports 80 (HTTP) and 443 (HTTPS) available
- A domain name configured (optional but recommended)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/hubz.git
cd hubz
```

### 2. Create Environment File

Create a `.env` file in the project root with your production settings:

```bash
cp .env.example .env
```

Edit the `.env` file with your values:

```env
# =============================================================================
# Database Configuration
# =============================================================================
DB_USERNAME=hubz
DB_PASSWORD=your-secure-database-password-here

# =============================================================================
# JWT Configuration
# =============================================================================
# Generate a secure secret: openssl rand -base64 64
JWT_SECRET=your-jwt-secret-at-least-256-bits-long-for-security
JWT_EXPIRATION=86400000

# =============================================================================
# CORS Configuration
# =============================================================================
# Comma-separated list of allowed origins
CORS_ALLOWED_ORIGINS=https://hubz.yourdomain.com

# =============================================================================
# Application URL
# =============================================================================
APP_BASE_URL=https://hubz.yourdomain.com

# =============================================================================
# Email Configuration (SMTP)
# =============================================================================
SMTP_HOST=smtp.yourmailprovider.com
SMTP_PORT=587
SMTP_USERNAME=your-email@domain.com
SMTP_PASSWORD=your-smtp-password

# =============================================================================
# Redis Configuration (Optional)
# =============================================================================
REDIS_PASSWORD=your-redis-password

# =============================================================================
# Google OAuth Configuration (Optional)
# =============================================================================
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# =============================================================================
# Frontend Port
# =============================================================================
FRONTEND_PORT=80
```

### 3. Build the Images

```bash
docker-compose -f docker-compose.prod.yml build
```

This will build optimized production images for both frontend and backend.

### 4. Start the Application

```bash
docker-compose -f docker-compose.prod.yml up -d
```

### 5. Verify the Deployment

Check that all containers are running:

```bash
docker-compose -f docker-compose.prod.yml ps
```

View logs:

```bash
# All services
docker-compose -f docker-compose.prod.yml logs -f

# Specific service
docker-compose -f docker-compose.prod.yml logs -f backend
```

### 6. Access the Application

Open your browser and navigate to:
- `http://localhost` (or your configured domain)

## Environment Variables Reference

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_PASSWORD` | PostgreSQL password | `securePassword123!` |
| `JWT_SECRET` | JWT signing secret (min 32 chars) | `openssl rand -base64 64` |

### Optional Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | `hubz` | PostgreSQL username |
| `JWT_EXPIRATION` | `86400000` | JWT expiration in ms (24h) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost` | Allowed CORS origins |
| `APP_BASE_URL` | `http://localhost` | Application base URL |
| `SMTP_HOST` | - | SMTP server host |
| `SMTP_PORT` | `587` | SMTP server port |
| `SMTP_USERNAME` | - | SMTP username |
| `SMTP_PASSWORD` | - | SMTP password |
| `REDIS_PASSWORD` | - | Redis password |
| `GOOGLE_CLIENT_ID` | - | Google OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | - | Google OAuth client secret |
| `FRONTEND_PORT` | `80` | Frontend exposed port |

## Common Operations

### Stop the Application

```bash
docker-compose -f docker-compose.prod.yml down
```

### Stop and Remove Volumes (WARNING: Data Loss)

```bash
docker-compose -f docker-compose.prod.yml down -v
```

### Update the Application

```bash
# Pull latest changes
git pull origin main

# Rebuild images
docker-compose -f docker-compose.prod.yml build

# Restart with zero downtime
docker-compose -f docker-compose.prod.yml up -d
```

### View Container Logs

```bash
# All services
docker-compose -f docker-compose.prod.yml logs -f

# Backend only
docker-compose -f docker-compose.prod.yml logs -f backend

# Last 100 lines
docker-compose -f docker-compose.prod.yml logs --tail=100 backend
```

### Access Container Shell

```bash
# Backend
docker-compose -f docker-compose.prod.yml exec backend sh

# PostgreSQL
docker-compose -f docker-compose.prod.yml exec postgres psql -U hubz -d hubzdb
```

### Database Backup

```bash
# Create backup
docker-compose -f docker-compose.prod.yml exec postgres pg_dump -U hubz hubzdb > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore backup
cat backup.sql | docker-compose -f docker-compose.prod.yml exec -T postgres psql -U hubz -d hubzdb
```

## Production Checklist

Before going live, ensure you have:

- [ ] Set strong, unique passwords for database and Redis
- [ ] Generated a secure JWT secret (at least 256 bits)
- [ ] Configured CORS with your actual domain(s)
- [ ] Set up SMTP for email functionality
- [ ] Configured SSL/TLS (see HTTPS section below)
- [ ] Set up regular database backups
- [ ] Configured monitoring and alerting
- [ ] Tested the deployment thoroughly

## HTTPS Configuration

For production, you should use HTTPS. Here are two options:

### Option 1: Reverse Proxy (Recommended)

Use a reverse proxy like Traefik, Caddy, or Nginx in front of the application to handle SSL termination.

Example with Traefik (add to docker-compose.prod.yml):

```yaml
services:
  traefik:
    image: traefik:v2.10
    command:
      - "--api.dashboard=true"
      - "--providers.docker=true"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.websecure.address=:443"
      - "--certificatesresolvers.letsencrypt.acme.httpchallenge=true"
      - "--certificatesresolvers.letsencrypt.acme.email=your-email@domain.com"
      - "--certificatesresolvers.letsencrypt.acme.storage=/letsencrypt/acme.json"
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - letsencrypt:/letsencrypt
    networks:
      - hubz-network

  frontend:
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.hubz.rule=Host(`hubz.yourdomain.com`)"
      - "traefik.http.routers.hubz.entrypoints=websecure"
      - "traefik.http.routers.hubz.tls.certresolver=letsencrypt"
```

### Option 2: Cloud Load Balancer

If deploying to cloud providers (AWS, GCP, Azure), use their load balancer services with managed SSL certificates.

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker-compose -f docker-compose.prod.yml logs backend

# Check container status
docker-compose -f docker-compose.prod.yml ps
```

### Database Connection Issues

```bash
# Test database connectivity
docker-compose -f docker-compose.prod.yml exec backend sh -c "nc -zv postgres 5432"

# Check PostgreSQL logs
docker-compose -f docker-compose.prod.yml logs postgres
```

### Out of Memory

Increase Docker memory limits or adjust container resource limits in `docker-compose.prod.yml`.

### Slow Performance

1. Check container resource usage: `docker stats`
2. Review database indexes
3. Enable Redis caching
4. Consider horizontal scaling

## Security Recommendations

1. **Change default passwords** - Never use default or weak passwords
2. **Use secrets management** - Consider Docker secrets or a vault service
3. **Limit network exposure** - Only expose necessary ports
4. **Regular updates** - Keep base images and dependencies updated
5. **Enable logging** - Configure centralized logging for auditing
6. **Backup regularly** - Automate database backups
7. **Monitor health** - Set up health checks and alerting

## Support

For issues and questions:
- GitHub Issues: [Create an issue](https://github.com/your-org/hubz/issues)
- Documentation: [CLAUDE.md](./CLAUDE.md)
- Features: [FEATURES.md](./FEATURES.md)
