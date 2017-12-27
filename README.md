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
2. When store log file to DropBox:
   ```java
   TBPLogHelper.getInstance().storeDropBox()
                        .subscribe(path -> <<STORE DropBox IS SUCCESS>>,
                                throwable -> <<STORE DropBox IS FAILED>>);
   ```
Download
--------
1. Implementation all library below:

    ```groovy
    implementation "com.jakewharton.timber:timber:4.5.1"
    implementation "io.reactivex:rxandroid:1.1.0"
    implementation "io.reactivex:rxjava:1.3.2"
    ```
    
Check out the sample app in `sample/` to see it in action.

