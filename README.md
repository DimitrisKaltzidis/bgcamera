# Camera Service on Background 

Sample code for test capture camera frames on background service, and process it frames with C++ methods. It support Camera API 1 on old devices and newer and Camera API 2 for **Android Things** and newer devices. For Camera API 2 app, you can stream frames to PC via vlc.

______

### Camera API 1 (Old API)

``` bash
./gradlew app:assembleDebug
./gradlew app:installDebug
adb shell am start -n com.admobilize.bgtest/com.admobilize.bgtest.MainActivity
```
______

### Camera API 2 (Android Things and newer devices)

``` bash
./gradlew apicamera2:assembleDebug
./gradlew apicamera2:installDebug
adb shell am start -n com.admobilize.bgapi2/com.admobilize.bgapi2.MainActivity
```

For stream frames (only api camera 2): 
``` javascript
vlc http://192.168.1.x:8080/
```



