# XWVassal 
(X-Wing Vassal module and Web page for the X-Wing Vassal league in /docs/)

## Distance and geometry specs:

* Small base ship: 113 px² 
* Large base ship: 226 px²
* GR-75 / Gozanti / C-ROC Huge ship:  226 px by 551 px
* CR90 / Raider / ? Huge ship: 226 px by 635 px

* Range 1 =  282.5 px
* Range 2 =  565.0 px
* Range 3 = 847.5 px
* Range 4 = 1130.0 px
* Range 5 = 1412.5 px

**1st edition**
* Small ship forward arc: 80.90 degrees
* Large ship forward arc: 84.05 degrees
* Large ship stardboard/port mobile turret arc: 95.95 degrees

(to help locate the coordinates of the forward arc's intersection to the forward edge)
The arc lines didn't quite go to the cardboard chit corner
* Small ship corner to intersection of forward arc to front edge: 8.3296 px
* Large ship corner to intersection of forward arc to front edge: 11.1650 px

**2nd edition**
*basic assumption: plastic bases are perfect squares. Small=113x113, Medium=169x169 and Large=226x226*

* All Chit Height = Plastic Base Height
* Small Chit Width = ??% of Plastic Width / ?? mm / ?? px
* Medium Chit Width = 88.16 % of Plastic Width / 52.90 mm / 149 px
* Large Chit Width = ??% of Plastic Width / ?? mm / ?? px

* Small Bullseye Arc = 24.65% of Plastic Width / 14.79 mm / 41.78 px
* Medium Bullseye Arc = 24.65% of Plastic Width / 14.79 mm / 41.78 px
* Large Bullseye Arc = 24.65% of Plastic Width / 14.79 mm / 41.78 px

* Small Chit Corner to Bullseye Arc Start: ??% of Plastic Width / ?? mm / ?? px
* Medium Chit Corner to Bullseye Arc Start: ??% of Plastic Width / 18.64 mm / 52.64 px
* Large Chit Corner to Bullseye Arc Start: ??% of Plastic Width / ?? mm / ?? px

* Small ship forward arc: ?? degrees
* Medium ship forward arc: 82.80 degrees
* Large ship forward arc: ?? degrees
* Large ship stardboard/port mobile turret arc: ?? degrees

The arc lines definitely go to the cardboard chit corner
* Small ship cardboard chit width: 98.670 px
* Small ship corner of plastic to corner of cardboard: 7.165 px
* Medium ship cardboard chit width: 150.918 px
* Medium ship corner of plastic to corner of cardboard: ?? px
* Large ship cardboard chit width: ?? px
* Large ship corner of plastic to corner of cardboard: ?? px

## Updating the module
*The current way via the autoupdater is using this repository:*
https://github.com/Mu0n/XWVassalOTA

*The following is via using the vassal editor*
### Adding pilot and upgrade cards
1. Vassal editor
2. gradlew unpackVmod
3. gradlew downloadXwingData
4. push

### Adding new ships with dials
1. Vassal editor, create a ship-specific Protytpe for its actions, await Radarman5's ship art and combine it in a deep-layered photoshop file  (save as png), create a new ship-specific tab in the Pieces window, create its dial
2. create ordered, open dial+strip for the new ship here: http://xwvassal.info/dialgen/dialgen
3. gradlew unpackVmod
4. gradlew downloadXwingData
5. push

*The following is via using intellij*
### Adding code
1. Your IDE of choice
2. gradlew downloadXwingData
3. gradlew buildVmod
4. push

### Adding an importable class
1. Add it to the XWCounterFactory
2. gradlew buildVmod
3. Import through the Vassal editor
4. gradlew unpackVmod
