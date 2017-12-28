# RxAndroidLogger
RxAndroidLogger is Android Logcat library. I using with Timber, RxJava and Dopbox Api library. It can save logcat to file, then store to dropbox.

Timber: https://github.com/JakeWharton/timber

Dropbox: https://github.com/dropbox/dropbox-sdk-java

This repository is developing

Usage
-----
1. In the `onCreate` of your application class:
   ```java
      LogSetup.Builder options = new LogSetup.Builder();
      options.setDropboxAccessToken("<YOUR DROPBOX ACCESS TOKEN>"); // example: "JM5srPDuSpMAAAAAAAAUmLljWVbGxYfdy7T3W_Dsh1Sv8B7-7H87QTgdWghuQAR-"
      options.setDropboxPath("<YOUR DROPBOX PATH>"); // example: "/TPBLog"
      TBPLogHelper.create(this, options.build());

      Timber.plant(TBPLogHelper.getInstance().getTBPDebugTree());
   ```
   (Tip: You can [generate an access token](https://blogs.dropbox.com/developers/2014/05/generate-an-access-token-for-your-own-account/) 
   for your own account through the [App Console](https://www.dropbox.com/developers/apps)).
 
2. Call Timber.v(...), Timber.d(...), Timber.i(...), Timber.e(...) for logcat in your app

3. When store log file to DropBox:
   ```java
   TBPLogHelper.getInstance().storeDropBox()
                        .subscribe(path -> <<STORE DropBox IS SUCCESS>>,
                                throwable -> <<STORE DropBox IS FAILED>>);
   ```
   
   Check out the sample app in `sample/` to see it in action.
   
Install
-------
1. Add it in your root build.gradle at the end of repositories:
    ```groovy
    allprojects {
    repositories {
        jcenter()
        maven { url 'https://jitpack.io' }
       }
    }
    ```
2. Add the dependency
    ```groovy
    dependencies {
           implementation "com.github.hoanglm4:RxAndroidLogger:1.0.2"
           implementation "com.jakewharton.timber:timber:4.5.1"
           implementation "io.reactivex:rxandroid:1.1.0"
           implementation "io.reactivex:rxjava:1.3.2"
	 }
    ```
3. In proguard file:
    ```
    -dontwarn okio.**
    -dontwarn okhttp3.**
    -dontwarn com.squareup.okhttp.**
    -dontwarn com.google.appengine.**
    -dontwarn javax.servlet.**
    ```
