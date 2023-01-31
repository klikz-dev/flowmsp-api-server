#!/bin/bash -eu

get_parm ()
{
    parm=$1
    aws ssm get-parameter --name $parm --with-decryption --query 'Parameter.Value' --output text
}

: ${CLIENT_SECRET:=$(get_parm CLIENT_SECRET)}
: ${GOOGLE_NOTIFICATION_METHOD:=$(get_parm GOOGLE_NOTIFICATION_METHOD)}
: ${S3_REGION:=$(get_parm S3_REGION)}
: ${MONGO_DB:=$(get_parm MONGO_DB)}
: ${MONGO_URI:=$(get_parm MONGO_URI)}
: ${REFRESH_TOKEN:=$(get_parm REFRESH_TOKEN)}
: ${TOPIC_NAME:=$(get_parm TOPIC_NAME)}
: ${SNS_REGION:=$(get_parm SNS_REGION)}
: ${GOOGLE_MAP_API_KEY:=$(get_parm GOOGLE_MAP_API_KEY)}
: ${S3_URL_ROOT:=$(get_parm S3_URL_ROOT)}
: ${CLIENT_ID:=$(get_parm CLIENT_ID)}
: ${ENV_TYPE:=$(get_parm ENV_TYPE)}
: ${PULL_TIMER:=$(get_parm PULL_TIMER)}
: ${SES_SYSTEM_EMAIL:=$(get_parm SES_SYSTEM_EMAIL)}
: ${S3_IMAGE_BUCKET:=$(get_parm S3_IMAGE_BUCKET)}
: ${SIGNUP_ARN:=$(get_parm SIGNUP_ARN)}
: ${SES_REGION:=$(get_parm SES_REGION)}
: ${SKIP_DISPATCH_HANDLING:=N}
: ${FIREBASE_KEY:=$(mktemp)}

get_parm firebase_key > $FIREBASE_KEY

export CLIENT_SECRET \
       GOOGLE_NOTIFICATION_METHOD \
       S3_REGION \
       MONGO_DB \
       MONGO_URI \
       REFRESH_TOKEN \
       TOPIC_NAME \
       SNS_REGION \
       GOOGLE_MAP_API_KEY \
       S3_URL_ROOT \
       CLIENT_ID \
       ENV_TYPE \
       PULL_TIMER \
       SES_SYSTEM_EMAIL \
       SIGNUP_ARN \
       SES_REGION \
       S3_IMAGE_BUCKET \
       SKIP_DISPATCH_HANDLING \
       FIREBASE_KEY

echo Starting API server $API_VERSION environment
java -XX:+PrintFlagsFinal --show-version -jar /app/api-server-$API_VERSION.jar
