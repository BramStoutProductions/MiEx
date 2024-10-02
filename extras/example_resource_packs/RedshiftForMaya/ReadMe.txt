




------------------------------------------------------------------------------------------------------------------------------------------------------------
-Put RedshiftForMaya file in \resources

Note: 
-You'll need to use "Maya_MiEx_Importer.py" to import .usd worlds into Maya!
or else you won't get any Materials/Textures

Questions:

"How do I use RedshiftForMaya?
after you put it in \resources, open MiEx, select your world and before you export press the + next to Redshift4Maya

"How do I use MiEx Importer?
in Maya go to "Windows > General settings > Script Editor"
in Script Editor click "File > Open script then choose "Maya_MiEx_Importer.py" and run the script

"Maya_MiEx_Importer.py didn't work with me!"

Solution: That's because your USD version is too old and it's not supported by MiEx Importer you'll have to download the latest version of USD
Download USD here: https://github.com/Autodesk/maya-usd/releases

"Some blocks appears with no textures in the Viewport!"
This is totally normal since the Redshift template is meant for render purposes

"How do I use other recourse packs with RedshiftForMaya?"
After you add the recourse pack file into \resources,
click the "+" next to your resource pack name, and click "+" next to "RedshiftForMaya", and make sure that "RedshiftForMaya" is on top of your resource pack 
------------------------------------------------------------------------------------------------------------------------------------------------------------