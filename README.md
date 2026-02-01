![Header image](https://bramstout.nl/content/miex_main_artwork_logo.jpg)
MiEx is a modern Minecraft and Hytale World Exporter written in Java, originally created for use by Bram Stout Productions to help create sets for animations. It has also been used for animations in Minecraft Live and Element Animation videos. MiEx exports out worlds into USD (Universal Scene Description).

# Features
* Uses Minecraft's Resource Pack system for an accurate export of the world, and allowing it to export with custom resource packs (including custom models).
* Support for modded worlds.
* Support for Minecraft Bedrock Edition worlds and resource packs.
* Support for Hytale worlds and resource packs / mods.
* Exports worlds out into big chunks (called export chunks) which each are payloaded into your scene, allowing you to only load in specific parts of the world when animating, so that the world won't slow down your computer.
* Level-of-Detail system to reduce polygon count in the far background, making it feasable to have massive sets.
* Remove caves feature to reduce polygon count.
* Ability to export certain blocks as a new instance for each occurence of the block, making it easy to replace those block (like a chest or a door) with a rig of the block.
* Up to 3x faster rendering with ray tracers (like Renderman, Arnold, Redshift, and Cycles) due to a custom made raytracing optimiser.
* Face optimiser that combines faces into larger faces, whenever possible, in order to reduce polycount.
* Powerful material templating system.
* Support for pipeline integration.
* Option to separate the world out into a foreground section and a background section.
* Custom random noise generator for block model selection to make randomly rotated textures appear nicer to the eye.
* Support for maps in item frames.
* Support for biome colours, including biome blending.
* Support for animated textures.
* Support for random frame offsets on animated textures.
* Support for grouping textures together into atlases in order to reduce material count.
* Support for adding a random offset to certain blocks (like vegetation) to reproduce what happens in Minecraft.
* Support for connected textures using Optifine's CTM format.
* Support for a modifier system that runs per block or per face that can modify biome colours, vertex colour sets, and face normals.
And many more features!

MiEx currently supports worlds from Minecraft Java Edition 1.2.1 and later, Minecraft Bedrock Edition 1.17 and later, and all versions of Hytale.

# Installing and Using MiEx
MiEx is released as a stand-alone Jar file which contains all that it needs. For more information on how to use MiEx, please visit [our wiki!](https://github.com/BramStoutProductions/MiEx/wiki)

# Forum
Talk about MiEx, ask for help, help others, and share your creations on our [GitHub Discussions page!](https://github.com/BramStoutProductions/MiEx/discussions)

