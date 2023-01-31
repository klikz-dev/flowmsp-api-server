#!/bin/bash -eu

# create repository if it doesn't exist
get_repository_url()
{
    profile=$1 repository=$2

    aws --profile=$profile ecr describe-repositories \
	--repository-name $repository \
	--query "repositories[].repositoryUri" \
	--output text ||
    aws --profile=$profile ecr create-repository \
	--repository-name $repository \
	--query "repository.repositoryUri" \
	--output text
}

if [ $# -lt 1 ]
then
    printf "Usage: %s [profile (dev or prod)][mvn-version]\n" $(basename $0) >&2
    exit 1
fi

PROFILE=$1 MVN_VERSION=$2

: ${IMAGE_NAME:="com.flowmsp/api-server"}
: ${URL:=$(get_repository_url $PROFILE $IMAGE_NAME)}
: ${TARGET_ACCOUNT:=$(aws --profile=$PROFILE sts get-caller-identity --query Account --output text)}

echo URL=$URL

# for now, follow established naming pattern
UNDIFFERENTIATED_TAG=$MVN_VERSION

# dev and prod will have different builds (eventually)
DEV_TAG=$MVN_VERSION-dev
PROD_TAG=$MVN_VERSION-prod

set -x
#rm -f $HOME/.docker/config.json
$(aws --profile=$PROFILE ecr get-login --no-include-email --registry-ids $TARGET_ACCOUNT)

docker tag ${IMAGE_NAME}:${DEV_TAG} ${URL}:${UNDIFFERENTIATED_TAG}
docker tag ${IMAGE_NAME}:${DEV_TAG} ${URL}:latest
docker push ${URL}:${UNDIFFERENTIATED_TAG}
docker push ${URL}:latest

exit 0
