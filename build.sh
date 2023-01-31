#!/bin/bash -e

if [ $# -lt 1 ]
then
    printf "Usage: %s [dev or prod]\n" $(basename $0) >&2
    exit 1
fi

PUSH=$1

mvn clean package assembly:single -DskipTests=true -Dhttps.protocols=TLSv1.2

# get the version of the api from the pom file
MVN_VERSION=$(mvn -q \
	-Dexec.executable=echo \
	-Dexec.args='${project.version}' \
	--non-recursive \
	exec:exec)

echo Building containers for version $MVN_VERSION

# image name from old gradle script
: ${IMAGE_NAME:="com.flowmsp/api-server"}

# for now, follow established naming pattern
UNDIFFERENTIATED_TAG=$MVN_VERSION

# dev and prod will have different builds (eventually)
DEV_TAG=$MVN_VERSION-dev
PROD_TAG=$MVN_VERSION-prod

# dev
# always build locally
echo Building docker image ${IMAGE_NAME}:${DEV_TAG}
docker build .\
       --tag ${IMAGE_NAME}:${DEV_TAG} \
       --tag ${IMAGE_NAME}:latest \
       --build-arg API_VERSION=$MVN_VERSION

# AWS ECR registries
DEV_REGISTRY="954397200061.dkr.ecr.us-west-2.amazonaws.com"
# uncomment when dev works and we won't destroy anything
#PROD_REGISTRY="840255817862.dkr.ecr.us-east-1.amazonaws.com"

# create repository if it doesn't exist
aws ecr describe-repositories --repository-name $IMAGE_NAME > /dev/null || \
    aws ecr create-repository --repository-name $IMAGE_NAME

: ${URL:=$(aws ecr describe-repositories --repository-name $IMAGE_NAME --query "repositories[].repositoryUri" --output text)}

echo URL=$URL

if [[ $PUSH == dev ]]; then
    # necessary in order to push images to ECR
    $(aws ecr get-login --no-include-email)

    # retag and push to registry as 'latest'
    docker tag ${IMAGE_NAME}:${DEV_TAG} ${URL}:latest

    # retag as current version
    docker tag ${IMAGE_NAME}:${DEV_TAG} ${URL}:${UNDIFFERENTIATED_TAG}
    docker tag ${IMAGE_NAME}:${DEV_TAG} ${URL}:latest

    # push retags to registry
    docker push ${URL}:${UNDIFFERENTIATED_TAG}
    docker push ${URL}:latest
fi

if [[ $PUSH == prod ]]; then
    # TODO: Repeat/modify for prod
    echo NOTE: Prod pushing not implemented yet
fi
