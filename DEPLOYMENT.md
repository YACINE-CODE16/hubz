# Hubz - Deployment Guide

This document provides instructions for deploying the Hubz application to production using **Vercel** (frontend) and **Render** (backend).

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Backend Deployment (Render)](#backend-deployment-render)
4. [Frontend Deployment (Vercel)](#frontend-deployment-vercel)
5. [Environment Variables](#environment-variables)
6. [Database Migration](#database-migration)
7. [Post-Deployment Checks](#post-deployment-checks)
8. [Troubleshooting](#troubleshooting)
9. [CI/CD Pipeline](#cicd-pipeline)

---

## Architecture Overview

```
                    ┌─────────────────────┐
                    │      Users          │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   Vercel (CDN)      │
                    │   hubz.vercel.app   │
                    │   React Frontend    │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   Render.com        │
                    │   hubz-backend      │
                    │   Spring Boot API   │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │   Render PostgreSQL │
                    │   hubz-db           │
                    └─────────────────────┘
```

---

## Prerequisites

Before deploying, ensure you have:

1. **GitHub Account** - Repository should be hosted on GitHub
2. **Vercel Account** - Sign up at [vercel.com](https://vercel.com)
3. **Render Account** - Sign up at [render.com](https://render.com)
4. **Domain (Optional)** - Custom domain for production

### Required Tools (Local Development)

```bash
# Vercel CLI
npm install -g vercel

# Render CLI (optional)
# Render uses Git-based deployments, CLI is optional
```

---

## Backend Deployment (Render)

### Option 1: Blueprint Deployment (Recommended)

The `render.yaml` file defines the complete infrastructure. Use this for the initial setup.

1. **Connect Repository**
   - Go to [Render Dashboard](https://dashboard.render.com)
   - Click "New" > "Blueprint"
   - Connect your GitHub repository
   - Select the branch to deploy (usually `main`)

2. **Review Blueprint**
   - Render will detect `render.yaml` in the root
   - Review the services and database configuration
   - Click "Apply" to create the infrastructure

3. **Configure Environment Variables**
   After deployment, go to the service settings and configure:

   ```
   # Required
   SPRING_PROFILES_ACTIVE=prod
   DATABASE_URL=(auto-configured from database)
   JWT_SECRET=(auto-generated)
   CORS_ALLOWED_ORIGINS=https://your-frontend-domain.vercel.app
   FRONTEND_URL=https://your-frontend-domain.vercel.app

   # Mail Configuration (Required for email features)
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-app-password
   MAIL_FROM=noreply@hubz.com

   # Google OAuth2 (Optional)
   GOOGLE_CLIENT_ID=your-google-client-id
   GOOGLE_CLIENT_SECRET=your-google-client-secret
   GOOGLE_REDIRECT_URI=https://hubz-backend.onrender.com/api/auth/oauth2/google/callback
   ```

### Option 2: Manual Deployment

1. **Create Web Service**
   - Go to Render Dashboard > New > Web Service
   - Connect GitHub repository
   - Configure:
     - **Name**: hubz-backend
     - **Environment**: Java
     - **Build Command**: `./mvnw clean package -DskipTests`
     - **Start Command**: `java -jar target/*.jar`
     - **Health Check Path**: `/api/health`

2. **Create PostgreSQL Database**
   - Go to Render Dashboard > New > PostgreSQL
   - Configure:
     - **Name**: hubz-db
     - **Database**: hubz
     - **User**: hubz
   - Copy the connection string for the web service

### Auto-Deploy Configuration

Render automatically deploys on push to the configured branch. To configure:

1. Go to Service Settings > Build & Deploy
2. Enable "Auto-Deploy" for the selected branch
3. Optionally configure deploy hooks for CI/CD

### Deploy Script

Use the provided script for manual deployments:

```bash
./scripts/deploy-backend.sh
```

---

## Frontend Deployment (Vercel)

### Option 1: Vercel Dashboard (Recommended)

1. **Import Project**
   - Go to [Vercel Dashboard](https://vercel.com/dashboard)
   - Click "Add New" > "Project"
   - Import your GitHub repository
   - Select `hubz-frontend` as the root directory

2. **Configure Build Settings**
   Vercel should auto-detect Vite, but verify:
   - **Framework Preset**: Vite
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
   - **Install Command**: `npm install`

3. **Configure Environment Variables**
   In Project Settings > Environment Variables, add:

   ```
   VITE_API_URL=https://hubz-backend.onrender.com
   ```

4. **Deploy**
   - Click "Deploy"
   - Vercel will build and deploy automatically

### Option 2: Vercel CLI

```bash
# Navigate to frontend directory
cd hubz-frontend

# Login to Vercel
vercel login

# Deploy to preview
vercel

# Deploy to production
vercel --prod
```

### Deploy Script

Use the provided script for deployments:

```bash
./scripts/deploy-frontend.sh
```

### Custom Domain Configuration

1. Go to Project Settings > Domains
2. Add your custom domain
3. Configure DNS records as instructed
4. Update `CORS_ALLOWED_ORIGINS` on Render to include the new domain

---

## Environment Variables

### Backend (Render)

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Yes | Active Spring profile | `prod` |
| `DATABASE_URL` | Yes | PostgreSQL connection string | Auto from Render |
| `JWT_SECRET` | Yes | JWT signing secret (min 256 bits) | Auto-generated |
| `CORS_ALLOWED_ORIGINS` | Yes | Allowed frontend origins | `https://hubz.vercel.app` |
| `FRONTEND_URL` | Yes | Frontend URL for email links | `https://hubz.vercel.app` |
| `MAIL_HOST` | No* | SMTP server host | `smtp.gmail.com` |
| `MAIL_PORT` | No* | SMTP server port | `587` |
| `MAIL_USERNAME` | No* | SMTP username | `email@gmail.com` |
| `MAIL_PASSWORD` | No* | SMTP password | App password |
| `MAIL_FROM` | No* | From email address | `noreply@hubz.com` |
| `GOOGLE_CLIENT_ID` | No | Google OAuth client ID | `xxx.apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | No | Google OAuth client secret | Secret string |
| `EMAIL_VERIFICATION_REQUIRED` | No | Require email verification | `false` |

*Required if email features are needed

### Frontend (Vercel)

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `VITE_API_URL` | Yes | Backend API URL | `https://hubz-backend.onrender.com` |

---

## Database Migration

### Initial Setup

The application uses `ddl-auto: update` by default, which will create tables automatically on first deployment.

### Production Recommendations

For production, it's recommended to:

1. **Change to `ddl-auto: validate`** after initial deployment
2. **Use Flyway or Liquibase** for managed migrations

### Manual Migration Steps

If you need to run manual migrations:

1. Connect to the Render PostgreSQL database:
   ```bash
   # Get connection string from Render Dashboard
   psql "postgres://user:password@host:port/database"
   ```

2. Run SQL migrations:
   ```sql
   -- Example: Add new column
   ALTER TABLE users ADD COLUMN new_field VARCHAR(255);
   ```

### Backup Database

```bash
# Export database
pg_dump "postgres://user:password@host:port/database" > backup.sql

# Import database
psql "postgres://user:password@host:port/database" < backup.sql
```

---

## Post-Deployment Checks

After deploying, verify the following:

### Backend Checks

1. **Health Check**
   ```bash
   curl https://hubz-backend.onrender.com/api/health
   # Expected: {"status": "UP"}
   ```

2. **Database Connection**
   ```bash
   curl https://hubz-backend.onrender.com/actuator/health
   # Check db.status is "UP"
   ```

3. **API Endpoint Test**
   ```bash
   curl https://hubz-backend.onrender.com/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email": "test@test.com", "password": "invalid"}'
   # Expected: 401 Unauthorized (confirms API is working)
   ```

### Frontend Checks

1. **Access Application**
   - Navigate to `https://hubz.vercel.app`
   - Verify the login page loads

2. **API Connection**
   - Open browser DevTools > Network
   - Verify API calls go to the correct backend URL
   - Check for CORS errors

3. **PWA Functionality**
   - Test "Install App" functionality
   - Verify service worker is registered

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| CORS errors | Frontend domain not in allowed origins | Update `CORS_ALLOWED_ORIGINS` |
| 502 Bad Gateway | Backend not started | Check Render logs |
| Database connection failed | Wrong DATABASE_URL | Verify connection string |
| JWT errors | Invalid or expired secret | Regenerate `JWT_SECRET` |

---

## Troubleshooting

### Render Backend Issues

1. **View Logs**
   - Go to Render Dashboard > Service > Logs
   - Check for startup errors

2. **Common Errors**
   - `Connection refused` - Database not ready, wait and retry
   - `OutOfMemoryError` - Upgrade plan or optimize memory usage
   - `Build failed` - Check Maven build locally first

3. **SSH into Instance** (Paid plans only)
   ```bash
   render ssh hubz-backend
   ```

### Vercel Frontend Issues

1. **View Build Logs**
   - Go to Vercel Dashboard > Deployments
   - Click on deployment to view logs

2. **Common Errors**
   - `Build failed` - Check TypeScript errors locally
   - `404 on refresh` - Verify `vercel.json` rewrites configuration
   - `API calls failing` - Check `VITE_API_URL` environment variable

3. **Preview Deployments**
   - Each PR creates a preview deployment
   - Use preview URL to test before merging

### Debug Checklist

- [ ] Backend health check returns 200
- [ ] Database connection is healthy
- [ ] Frontend loads without errors
- [ ] API calls succeed (no CORS issues)
- [ ] Environment variables are set correctly
- [ ] Logs show no critical errors

---

## CI/CD Pipeline

### GitHub Actions (Optional)

Create `.github/workflows/deploy.yml` for automated deployments:

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Deploy to Render
        run: |
          curl -X POST ${{ secrets.RENDER_DEPLOY_HOOK }}

  deploy-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - name: Install Vercel CLI
        run: npm install -g vercel
      - name: Deploy to Vercel
        run: |
          cd hubz-frontend
          vercel --prod --token=${{ secrets.VERCEL_TOKEN }}
```

### Required Secrets

Add these secrets to your GitHub repository:

- `RENDER_DEPLOY_HOOK` - Render deploy hook URL
- `VERCEL_TOKEN` - Vercel API token
- `VERCEL_ORG_ID` - Vercel organization ID
- `VERCEL_PROJECT_ID` - Vercel project ID

---

## Monitoring

### Recommended Tools

1. **Uptime Monitoring**
   - [UptimeRobot](https://uptimerobot.com) (free tier available)
   - Monitor `/api/health` endpoint

2. **Error Tracking**
   - [Sentry](https://sentry.io) for both frontend and backend
   - Add `SENTRY_DSN` environment variable

3. **Analytics**
   - [Vercel Analytics](https://vercel.com/analytics) for frontend
   - Add `VITE_GA_TRACKING_ID` for Google Analytics

---

## Security Checklist

Before going live, verify:

- [ ] JWT secret is strong and randomly generated
- [ ] CORS only allows your frontend domain
- [ ] HTTPS is enforced (automatic on Vercel/Render)
- [ ] Database credentials are not exposed
- [ ] API rate limiting is configured
- [ ] Sensitive endpoints require authentication
- [ ] Error messages don't leak internal details
- [ ] Swagger UI is disabled in production

---

## Cost Estimation

### Free Tier Limits

**Render (Free)**
- 750 hours/month compute
- PostgreSQL: 90 days, 1GB storage
- Spins down after 15 minutes of inactivity

**Vercel (Free - Hobby)**
- 100GB bandwidth/month
- Unlimited deployments
- Serverless function limits apply

### Production Recommendations

For production workloads, consider upgrading:

| Service | Plan | Cost | Benefits |
|---------|------|------|----------|
| Render Web | Starter | $7/month | Always-on, more memory |
| Render DB | Starter | $7/month | Persistent, backups |
| Vercel | Pro | $20/month | Higher limits, team features |

---

## Quick Reference

### Deploy Commands

```bash
# Frontend (Vercel)
./scripts/deploy-frontend.sh

# Backend (Render - via git push)
./scripts/deploy-backend.sh

# Or manually
cd hubz-frontend && vercel --prod
git push origin main  # Triggers Render auto-deploy
```

### Useful URLs

- **Frontend**: https://hubz.vercel.app
- **Backend API**: https://hubz-backend.onrender.com/api
- **Health Check**: https://hubz-backend.onrender.com/api/health
- **Vercel Dashboard**: https://vercel.com/dashboard
- **Render Dashboard**: https://dashboard.render.com

---

*Last updated: February 2025*
