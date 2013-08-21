ScalaPlayer
===========

To build with Eclipse,

1 git clone https://github.com/illi-ichi/ScalaMusicPlayer.git <br>
2 import MusicPlayer <br>
3 Clean the workspace <br>


MusicPlayer is the main project.
player is an experimental project with Scaloid.

Problems:

1 Android, Scala, Maven and Test don't get along well. <br>
2 Official Android test system doesn't work with Scala. 
The test works well when it's written in Java but doesn't run when written in Scala.<br>
3 Robolectric works with Scala but it runs on a regular JVM 
so I have to figure out how to mock the content provider (DB). <br>

The most promising tutorial is https://bitbucket.org/loyolachicagocs_plsystems/clickcounter-android-scala.
However, the tutorial doesn't provide a project template. They just put their whole project there.
