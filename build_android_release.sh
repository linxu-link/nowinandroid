#!/usr/bin/env bash

#
# Copyright 2022 The Android Open Source Project
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       https://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#

# 请忽略此文件，它仅用于 Google 内部发布流程

# 获取脚本所在目录
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_OUT=$DIR/app/build/outputs

# 设置 JAVA_HOME 指向预构建的 JDK 17
export JAVA_HOME="$(cd $DIR/../nowinandroid-prebuilts/jdk17/linux && pwd )"
echo "JAVA_HOME=$JAVA_HOME"

# 设置 ANDROID_HOME 指向预构建的完整 SDK
export ANDROID_HOME="$(cd $DIR/../../../prebuilts/fullsdk/linux && pwd )"
echo "ANDROID_HOME=$ANDROID_HOME"

# 复制 google-services.json 文件
echo "正在复制 google-services.json"
cp $DIR/../nowinandroid-prebuilts/google-services.json $DIR/app

# 复制 local.properties 文件
echo "正在复制 local.properties"
cp $DIR/../nowinandroid-prebuilts/local.properties $DIR

cd $DIR

# 构建 prodRelease 变体
GRADLE_PARAMS=" --stacktrace -Puse-google-services"
$DIR/gradlew :app:clean :app:assembleProdRelease :app:bundleProdRelease ${GRADLE_PARAMS}
BUILD_RESULT=$?

# 复制生产发布版 APK
cp $APP_OUT/apk/prod/release/app-prod-release.apk $DIST_DIR/app-prod-release.apk
# 复制生产发布版 Bundle
cp $APP_OUT/bundle/prodRelease/app-prod-release.aab $DIST_DIR/app-prod-release.aab
# 复制生产发布版 Bundle 映射文件
cp $APP_OUT/mapping/prodRelease/mapping.txt $DIST_DIR/mobile-release-aab-mapping.txt

exit $BUILD_RESULT
