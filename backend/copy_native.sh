#!/bin/sh
set -e

TERRAFORM_PATH="$HOME/dev/terraform/home_terraform/modules/raspberry_lambda"
BUILD_DIR="target"
mvn clean install -Pnative -Dnative-image.docker-build=true -Dquarkus.native.enable-jni=true 
cp $BUILD_DIR/function.zip $TERRAFORM_PATH/raspberry_backend.zip
