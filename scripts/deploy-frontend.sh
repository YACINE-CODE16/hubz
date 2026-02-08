#!/bin/bash

# =================================================
# Hubz Frontend - Vercel Deployment Script
# =================================================
# This script deploys the frontend to Vercel
#
# Usage:
#   ./scripts/deploy-frontend.sh [--preview]
#
# Options:
#   --preview    Deploy to preview environment (default: production)
# =================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
FRONTEND_DIR="hubz-frontend"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Parse arguments
PREVIEW_MODE=false
if [[ "$1" == "--preview" ]]; then
    PREVIEW_MODE=true
fi

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Hubz Frontend Deployment${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if Vercel CLI is installed
if ! command -v vercel &> /dev/null; then
    echo -e "${RED}Error: Vercel CLI is not installed.${NC}"
    echo -e "${YELLOW}Install it with: npm install -g vercel${NC}"
    exit 1
fi

# Navigate to frontend directory
cd "$PROJECT_ROOT/$FRONTEND_DIR"
echo -e "${GREEN}Working directory: $(pwd)${NC}"
echo ""

# Check if logged in to Vercel
echo -e "${YELLOW}Checking Vercel authentication...${NC}"
if ! vercel whoami &> /dev/null; then
    echo -e "${YELLOW}Not logged in to Vercel. Please login:${NC}"
    vercel login
fi
echo ""

# Run build locally to verify
echo -e "${YELLOW}Running local build to verify...${NC}"
npm run build

if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed! Please fix errors before deploying.${NC}"
    exit 1
fi
echo -e "${GREEN}Build successful!${NC}"
echo ""

# Deploy to Vercel
if [ "$PREVIEW_MODE" = true ]; then
    echo -e "${YELLOW}Deploying to preview environment...${NC}"
    vercel
else
    echo -e "${YELLOW}Deploying to production...${NC}"
    vercel --prod
fi

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  Deployment successful!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${BLUE}Post-deployment checklist:${NC}"
    echo -e "  [ ] Verify the application loads correctly"
    echo -e "  [ ] Check browser console for errors"
    echo -e "  [ ] Test API connectivity"
    echo -e "  [ ] Verify PWA functionality"
else
    echo -e "${RED}Deployment failed!${NC}"
    exit 1
fi
