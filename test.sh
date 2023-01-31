#!/bin/bash -eu

: ${NUM_MSGS:=50}

~/repos/support-tools/test_container.sh --env MAX_UNREAD_TO_RETURN=$NUM_MSGS com.flowmsp/api-server:latest

exit 0
