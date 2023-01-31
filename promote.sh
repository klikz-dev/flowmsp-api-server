#!/bin/bash -eu

if [ $# -lt 2 ]
then
    printf "Usage: %s [ver]\n" $(basename $0) >&2
    exit 1
fi

VER=$1
shift
COMMENT="$@"

git add .
git commit -m "$COMMENT"
git tag -a $VER -m "$COMMENT"
git push
git push --tags

./build.sh dev

./push.sh prod $VER

exit 0

