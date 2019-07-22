#!/usr/bin/env sh
#cp app/build/outputs/apk/release/app-release.apk Nano_Updater-$TRAVIS_BUILD_NUMBER.apk
curl -F chat_id="-1001242182605" -F document=@"Nano_Updater-$TRAVIS_BUILD_NUMBER.apk" https://api.telegram.org/bot$TELEGRAM_TOKEN/sendDocument
