########### BEFORE RUNNING ############


It might be necessery to rename some files before running analysis, use bash command:

for filename in *foo*; do echo mv \"$filename\" \"${filename//foo/bar}\"; done > rename.txt

Check rename.txt and then pipe to bin bash

for filename in *foo*; do echo mv \"$filename\" \"${filename//foo/bar}\"; done | /bin/bash





########### ON WINDOWS  ############

Command line arguments:
<path to the folder with mp3 music files> <path to the settings> <path to the folder where json should be created>

The default settings are already in the project, so if you don't want to use your custome ones, then just write "settings.xml". An example of my run configuration:

C:\RWTH\AndroidLab\xml\mp3 settings.xml C:\RWTH\AndroidLab\xml


########### ON LINUX  ############


sudo java -jar noscientificnotation.jar ../mp3/tracks settings.xml ../audio_features/ features.xml


to produce with reduced feature set of 19:

sudo java -jar noscientificnotation.jar ../mp3/tracks settings_reducedscaled.xml ../audio_features/ features.xml

