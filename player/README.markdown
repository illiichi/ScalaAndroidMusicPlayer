# ScalaMusicPlayer

[Scaloid](https://github.com/pocorall/scaloid) 

Prerequisites
-------------
* Maven 3
* Android SDK

Build
-----
You can build using Maven:

    $ mvn clean package

This will compile the project and generate an APK. The generated APK is
signed with the Android debug certificate. To generate a zip-aligned APK
that is signed with an actual certificate, use:

    $ mvn clean package -Prelease

The configuration for which certificate to use is in pom.xml.

Run
---
Deploy to an Android virtual device (AVD):

    $ mvn android:deploy

Using an IDE
------------
You can use Maven to generate project files for Eclipse or IDEA:

    $ mvn eclipse:eclipse
    $ mvn idea:idea
