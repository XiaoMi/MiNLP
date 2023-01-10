#!/usr/bin/env sh

set -ex

if [ $# -ne 1 ] ; then
  echo "version is required"
  exit 1
fi

git checkout main
git pull

version="$1"
tag="v${version}D"

git checkout -b "duckling-$version"

echo "ThisBuild / version := \"$version\"" > version.sbt

git add version.sbt
git commit -m "set duckling version $version"

git tag -a "$tag" -m "duckling $version"
git push origin $tag
