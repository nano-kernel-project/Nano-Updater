Nano Updater
============
Simple application to download and flash kernel packages without any hassle for users.

The app searches for installed kernel version in `/system/build.prop` file for supported devices/kernels. Since the idea of this updater seeded when I was using a Xiaomi device, It supports both MIUI and AOSP builds.

Features
--------

- Beautifully designed dark theme right out of the box.
- Hassle free experience for users.
- Download latest kernel builds right from the app.
- Supports:
    - Auto flashing kernels (Reboots the device to recovery and flashes kernel)
    - Manual flashing kernels (Flashing kernel within the app and then rebooting the device)
- Monitor changelogs within the app for every version.     

Currently, updater supports the JSON in the following format:

API Format
----------

```json
{  
   "AOSP":[  
      {  
         "filename":"package_aosp.zip",
         "size":"14.8 MB",
         "date":"20190626",
         "md5":"028e5c746a0f92704c92c719532a9bb6",
         "release_number":"v7",
         "url":"https://example.com/package_aosp.zip",
         "changelog_url":"https://raw.githubusercontent.com/nano-kernel-project/Nano_OTA_changelogs/master/test_changelogs.txt"
      }
   ],
   "MIUI":[  
      {  
         "filename":"package_miui.zip",
         "size":"14.8 MB",
         "date":"20190626",
         "md5":"028e5c746a0f92704c92c719532a9bb6",
         "release_number":"v7",
         "url":"https://example.com/package_miui.zip",
         "changelog_url":"https://raw.githubusercontent.com/nano-kernel-project/Nano_OTA_changelogs/master/test_changelogs.txt"
      }
   ]
}
```

The `filename` attribute is the name of the package.  
The `size` attribute is the length of the package in MB.  
The `date` attribute is the timestamp of the package built, expressed as yyyyMMdd.  
The `md5` attribute is the MD5 checksum of the package.  
The `release_number` attribute is the revision number (or the version) of the kernel.  
The `url` attribute is the URL of the file to be downloaded.  
The `changelog_url` attribute is the URL of the changelog, expressed in plain text.  

Changelogs are retrieved from a text file. The content of changelogs are accessed from `changelog_url` attribute of JSON.

Build with Android Studio
-------------------------

Updater doesn't need any extra work to be imported in Android Studio. 
Just clone the repo and import it to Android Studio. You can also do 
it this way, Import from VCS and paste this repo link: https://github.com/nano-kernel-project/Nano-Updater.git  