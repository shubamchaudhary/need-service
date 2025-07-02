#!/bin/bash

# Docker build and run script for Need Calculation Service

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="need-calculation-service"
CONTAINER_NAME="need-calc-app"
PORT=8081

echo -e "${GREEN}Building Need Calculation Service...${NC}"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check if Docker is installed
if ! command_exists docker; then
    echo -e "${RED}Docker is not installed. Please install Docker first.${NC}"
    exit 1
fi

# Parse command line arguments
ACTION=${1:-"build"}

case $ACTION in
    "build")
        echo -e "${YELLOW}Building Docker image...${NC}"
        docker build -t $IMAGE_NAME . || {
            echo -e "${RED}Build failed!${NC}"
            exit 1
        }
        echo -e "${GREEN}Build successful!${NC}"
        ;;

    "run")
        echo -e "${YELLOW}Starting container...${NC}"

        # Stop existing container if running
        if [ "$(docker ps -aq -f name=$CONTAINER_NAME)" ]; then
            echo -e "${YELLOW}Stopping existing container...${NC}"
            docker stop $CONTAINER_NAME
            docker rm $CONTAINER_NAME
        fi

        # Run new container
        docker run -d \
            --name $CONTAINER_NAME \
            -p $PORT:8081 \
            -e SPRING_PROFILES_ACTIVE=docker \
            -v $(pwd)/logs:/app/logs \
            $IMAGE_NAME

        echo -e "${GREEN}Container started!${NC}"
        echo -e "Access the service at: ${GREEN}http://localhost:$PORT${NC}"
        ;;

    "build-and-run")
        $0 build
        $0 run
        ;;

    "logs")
        docker logs -f $CONTAINER_NAME
        ;;

    "stop")
        echo -e "${YELLOW}Stopping container...${NC}"
        docker stop $CONTAINER_NAME
        docker rm $CONTAINER_NAME
        echo -e "${GREEN}Container stopped!${NC}"
        ;;

    "test")
        echo -e "${YELLOW}Testing endpoints...${NC}"

        # Wait for service to be ready
        echo -e "Waiting for service to start..."
        sleep 5

        # Test health endpoint
        echo -e "\n${YELLOW}Testing health endpoint:${NC}"
        curl -s http://localhost:$PORT/api/v1/need-calculation/health | jq '.' || echo "Service might not be ready yet"

        # Test info endpoint
        echo -e "\n${YELLOW}Testing info endpoint:${NC}"
        curl -s http://localhost:$PORT/api/v1/need-calculation/info | jq '.'

        # Test config endpoint
        echo -e "\n${YELLOW}Testing config endpoint:${NC}"
        curl -s http://localhost:$PORT/api/v1/need-calculation/config | jq '.'

        # Test calculation endpoint
        echo -e "\n${YELLOW}Testing calculation endpoint:${NC}"
        curl -s -X POST http://localhost:$PORT/api/v1/need-calculation/calculate \
            -H "Content-Type: application/json" \
            -d '{
                "productName": "Bisleri-1L",
                "month": "December",
                "stores": [
                    {"storeName": "str1", "region": "extreme_north"},
                    {"storeName": "str2", "region": "rajasthan"}
                ]
            }' | jq '.'
        ;;

    "push")
        if [ -z "$2" ]; then
            echo -e "${RED}Please provide Docker Hub username${NC}"
            echo "Usage: $0 push <username>"
            exit 1
        fi

        DOCKER_USERNAME=$2
        echo -e "${YELLOW}Tagging image...${NC}"
        docker tag $IMAGE_NAME $DOCKER_USERNAME/$IMAGE_NAME:latest

        echo -e "${YELLOW}Pushing to Docker Hub...${NC}"
        docker push $DOCKER_USERNAME/$IMAGE_NAME:latest

        echo -e "${GREEN}Push successful!${NC}"
        ;;

    "compose-up")
        echo -e "${YELLOW}Starting with docker-compose...${NC}"
        docker-compose up -d
        echo -e "${GREEN}Services started!${NC}"
        ;;

    "compose-down")
        echo -e "${YELLOW}Stopping docker-compose services...${NC}"
        docker-compose down
        echo -e "${GREEN}Services stopped!${NC}"
        ;;

    "clean")
        echo -e "${YELLOW}Cleaning up Docker resources...${NC}"

        # Stop and remove container
        if [ "$(docker ps -aq -f name=$CONTAINER_NAME)" ]; then
            docker stop $CONTAINER_NAME
            docker rm $CONTAINER_NAME
        fi

        # Remove image
        if [ "$(docker images -q $IMAGE_NAME)" ]; then
            docker rmi $IMAGE_NAME
        fi

        # Clean up volumes
        docker volume prune -f

        echo -e "${GREEN}Cleanup complete!${NC}"
        ;;

    *)
        echo -e "${YELLOW}Usage: $0 {build|run|build-and-run|logs|stop|test|push|compose-up|compose-down|clean}${NC}"
        echo ""
        echo "Commands:"
        echo "  build         - Build Docker image"
        echo "  run           - Run container"
        echo "  build-and-run - Build and run"
        echo "  logs          - View container logs"
        echo "  stop          - Stop container"
        echo "  test          - Test all endpoints"
        echo "  push <user>   - Push to Docker Hub"
        echo "  compose-up    - Start with docker-compose"
        echo "  compose-down  - Stop docker-compose"
        echo "  clean         - Clean up resources"
        exit 1