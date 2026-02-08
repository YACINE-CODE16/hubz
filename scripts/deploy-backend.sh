#!/bin/bash

# =================================================
# Hubz Backend - Render Deployment Script
# =================================================
# This script triggers a deployment to Render by pushing
# to the configured branch (usually main).
#
# Render uses Git-based deployments, so this script:
# 1. Verifies the build works locally
# 2. Commits any pending changes (optional)
# 3. Pushes to the deployment branch
#
# Usage:
#   ./scripts/deploy-backend.sh [--skip-tests] [--skip-commit]
#
# Options:
#   --skip-tests   Skip running tests before deployment
#   --skip-commit  Skip the commit step (just push)
# =================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BACKEND_DIR="hubz-backend"
DEPLOY_BRANCH="main"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Parse arguments
SKIP_TESTS=false
SKIP_COMMIT=false

for arg in "$@"; do
    case $arg in
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --skip-commit)
            SKIP_COMMIT=true
            shift
            ;;
    esac
done

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Hubz Backend Deployment (Render)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Navigate to project root
cd "$PROJECT_ROOT"
echo -e "${GREEN}Working directory: $(pwd)${NC}"
echo ""

# Check if we're on the correct branch
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "$DEPLOY_BRANCH" ]; then
    echo -e "${YELLOW}Warning: Not on $DEPLOY_BRANCH branch (currently on $CURRENT_BRANCH)${NC}"
    read -p "Do you want to switch to $DEPLOY_BRANCH? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git checkout "$DEPLOY_BRANCH"
    else
        echo -e "${YELLOW}Continuing on $CURRENT_BRANCH branch...${NC}"
    fi
fi
echo ""

# Navigate to backend directory for build
cd "$BACKEND_DIR"

# Run tests (unless skipped)
if [ "$SKIP_TESTS" = false ]; then
    echo -e "${YELLOW}Running tests...${NC}"
    ./mvnw test

    if [ $? -ne 0 ]; then
        echo -e "${RED}Tests failed! Please fix failing tests before deploying.${NC}"
        exit 1
    fi
    echo -e "${GREEN}All tests passed!${NC}"
    echo ""
fi

# Build the application
echo -e "${YELLOW}Building application...${NC}"
./mvnw clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed! Please fix errors before deploying.${NC}"
    exit 1
fi
echo -e "${GREEN}Build successful!${NC}"
echo ""

# Go back to project root for git operations
cd "$PROJECT_ROOT"

# Check for uncommitted changes
if [ "$SKIP_COMMIT" = false ]; then
    if [ -n "$(git status --porcelain)" ]; then
        echo -e "${YELLOW}Uncommitted changes detected:${NC}"
        git status --short
        echo ""
        read -p "Do you want to commit these changes? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            read -p "Enter commit message: " COMMIT_MSG
            git add .
            git commit -m "$COMMIT_MSG"
        else
            echo -e "${YELLOW}Skipping commit...${NC}"
        fi
    fi
fi
echo ""

# Push to remote
echo -e "${YELLOW}Pushing to $DEPLOY_BRANCH branch...${NC}"
git push origin "$DEPLOY_BRANCH"

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  Push successful!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${BLUE}Render will automatically deploy the changes.${NC}"
    echo -e "${BLUE}Monitor the deployment at: https://dashboard.render.com${NC}"
    echo ""
    echo -e "${BLUE}Post-deployment checklist:${NC}"
    echo -e "  [ ] Monitor Render deployment logs"
    echo -e "  [ ] Wait for deployment to complete (~2-5 minutes)"
    echo -e "  [ ] Verify health check: curl https://hubz-backend.onrender.com/api/health"
    echo -e "  [ ] Test API endpoints"
    echo -e "  [ ] Check application logs for errors"
else
    echo -e "${RED}Push failed!${NC}"
    exit 1
fi
