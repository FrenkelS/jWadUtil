Utility to process Doom IWAD files for [Doom8088](https://github.com/FrenkelS/Doom8088), [Doom8088: Motorola 68000 Edition](https://github.com/FrenkelS/Doom8088ST), [doomtd3](https://github.com/FrenkelS/doomtd3) and [ELKSDOOM](https://github.com/FrenkelS/elksdoom).

It writes a new WAD file.

This is required to reduce the memory footprint of some of the data structures in Doom to get it to fit in 64 kB.

We will pre-calculate more fields so that the lumps stored in the WAD can be used directly from the WAD rather than having to load and convert them in memory at runtime.

For example:

By storing vertexes in LINEDEFS and SEGS the VERTEXES lump can be removed.

Usage: `mvn clean verify`

This tool requires Windows and Java, and uses [wadptr](https://soulsphere.org/projects/wadptr) and [ZenNode](https://www.mrousseau.org/programs/ZenNode).
