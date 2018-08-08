package mic;

import VASSAL.build.GameModule;
import VASSAL.build.widget.PieceSlot;
import VASSAL.counters.GamePiece;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static mic.Util.logToChat;

/*
 * created by mjuneau on 6/8/18
 * This class dynamically generates GamePieces during AutoSquadSpawn
 */
public class GamePieceGenerator2e
{
    private static final String SMALL_STEM_SHIP_SLOT_NAME = "ship -- 2e Stem Small Ship";
    private static final String MEDIUM_STEM_SHIP_SLOT_NAME = "ship -- 2e Stem Medium Ship";
    private static final String LARGE_STEM_SHIP_SLOT_NAME = "ship -- 2e Stem Large Ship";

    private static final String SMALL_STEM_SHIP_SINGLE_TURRET_SLOT_NAME = "ship -- 2e Stem Small Single Turret Ship";
    private static final String MEDIUM_STEM_SHIP_SINGLE_TURRET_SLOT_NAME = "ship -- 2e Stem Medium Single Turret Ship";
    private static final String LARGE_STEM_SHIP_SINGLE_TURRET_SLOT_NAME = "ship -- 2e Stem Large Single Turret Ship";

    private static final String MEDIUM_STEM_SHIP_DOUBLE_TURRET_SLOT_NAME = "ship -- 2e Stem Medium Double Turret Ship";
    private static final String LARGE_STEM_SHIP_DOUBLE_TURRET_SLOT_NAME = "ship -- 2e Stem Large Double Turret Ship";

    private static final String SHIP_BASE_SIZE_SMALL = "small";
    private static final String SHIP_BASE_SIZE_MEDIUM = "medium";
    private static final String SHIP_BASE_SIZE_LARGE = "large";

    // generate a ship GamePiece
    public static GamePiece generateShip(VassalXWSPilotPieces2e ship, List<XWS2Pilots> allShips)
    {
        XWS2Pilots.Pilot2e pilotData = ship.getPilotData();

        // get the master data for the ship
        XWS2Pilots shipData = XWS2Pilots.getSpecificShip(ship.toString(),allShips);
        String faction = ship.getShipData().getFaction();

        // generate the piece from the stem ships
        GamePiece newShip = null;
      //  boolean shipContainsMobileArc = containsMobileArc(shipData);
        if(shipData.getSize().contentEquals(SHIP_BASE_SIZE_SMALL))
        {
            newShip = Util.newPiece(getPieceSlotByName(SMALL_STEM_SHIP_SLOT_NAME));
        }else if(shipData.getSize().contentEquals(SHIP_BASE_SIZE_LARGE))
        {
            //TO DO deal with mobilearc detection
            /*
            if(containsMobileArc(shipData))
            {
                //newShip = Util.newPiece(getPieceSlotByName(LARGE_STEM_SHIP_MOBILE_ARC_SLOT_NAME));
            }else {
                newShip = Util.newPiece(getPieceSlotByName(LARGE_STEM_SHIP_SLOT_NAME));
            }
            */
            newShip = Util.newPiece(getPieceSlotByName(LARGE_STEM_SHIP_SLOT_NAME));
        }

        // determine if the ship needs bomb drop
       boolean needsBombCapability = determineIfShipNeedsBombCapability(ship, allShips);

        // execute the command to build the ship piece
        //TO DO deal with dual base detection and associated text
        //StemShip.ShipGenerateCommand myShipGen = new StemShip.ShipGenerateCommand(Canonicalizer.getCleanedName(ship.getShipData().getName()), newShip, faction, pilotData.getXWS2(),needsBombCapability, shipData.hasDualBase(), shipData.getDualBaseToggleMenuText(),shipData.getBaseReport1Identifier(),shipData.getBaseReport2Identifier());
        StemShip.ShipGenerateCommand myShipGen = new StemShip.ShipGenerateCommand(Canonicalizer.getCleanedName(ship.getShipData().getName()), newShip, faction, pilotData.getXWS2(),needsBombCapability, false, "","","");

        myShipGen.execute();

        // add the stats to the piece
        newShip = setShipProperties(newShip,ship.getUpgrades(), shipData, pilotData, ship);
        return newShip;
    }

    public static GamePiece setShipProperties(GamePiece piece, List<VassalXWSPilotPieces2e.Upgrade> upgrades,XWS2Pilots shipData,XWS2Pilots.Pilot2e pilotData,VassalXWSPilotPieces2e ship ) {
        //GamePiece piece = Util.newPiece(this.ship);

        int initiativeModifier = 0;
        int chargeModifier = 0;
        int shieldsModifier = 0;
        int hullModifier = 0;
        int forceModifier = 0;


        for (VassalXWSPilotPieces2e.Upgrade upgrade : upgrades) {

            MasterUpgradeData.UpgradeGrants doubleSideCardStats = DoubleSideCardPriorityPicker.getDoubleSideCardStats(upgrade.getXwsName());
            ArrayList<MasterUpgradeData.UpgradeGrants> grants = new ArrayList<MasterUpgradeData.UpgradeGrants>();
            if(grants!=null)
            {
                if (doubleSideCardStats != null) {
                    grants.add(doubleSideCardStats);
                } else {
                    ArrayList<MasterUpgradeData.UpgradeGrants> newGrants = new ArrayList<MasterUpgradeData.UpgradeGrants>();
                    try{
                        newGrants = upgrade.getUpgradeData().getGrants();
                    }catch(Exception e){

                    }
                    if(newGrants !=null) grants.addAll(newGrants);
                }
            }


            for (MasterUpgradeData.UpgradeGrants modifier : grants) {
                if (modifier.isStatsModifier()) {
                    String name = modifier.getName();
                    int value = modifier.getValue();

                    if (name.equals("hull")) hullModifier += value;
                    else if (name.equals("shields")) shieldsModifier += value;
                    else if (name.equals("initiative")) initiativeModifier += value;
                    else if (name.equals("force")) forceModifier += value;
                    else if (name.equals("charge")) chargeModifier += value;
                }
            }
        }

        if (shipData != null)
        {
            int hull = shipData.getHull();
            int shields = shipData.getShields();

            //TO DO overrides, ugh
            /*
            if (pilotData != null && pilotData.getShipOverrides() != null)
            {
                MasterPilotData.ShipOverrides shipOverrides = pilotData.getShipOverrides();
                hull = shipOverrides.getHull();
                shields = shipOverrides.getShields();
            }
            */

            piece.setProperty("Hull Rating", hull + hullModifier);
            piece.setProperty("Shield Rating", shields + shieldsModifier);

            /* use this example for force and charge
            if (shipData.getEnergy() > 0) {
                int energy = shipData.getEnergy();
                piece.setProperty("Energy Rating", energy + energyModifier);
            }
            */
        }

        if (pilotData != null) {
            int ps = pilotData.getInitiative() + initiativeModifier;
            piece.setProperty("Initiative", ps);
        }

        if (pilotData != null) {
            piece.setProperty("Pilot Name", getDisplayShipName(pilotData,shipData));
        }

        return piece;
    }



    private static boolean containsMobileArc(MasterShipData.ShipData shipData)
    {
        boolean foundMobileArc = false;
        List<String>arcs = shipData.getFiringArcs();
        Iterator<String> i = arcs.iterator();
        String arc = null;
        while(i.hasNext() && !foundMobileArc)
        {
            arc = i.next();
            if(arc.equals("Mobile"))
            {
                foundMobileArc = true;
            }
        }

        return foundMobileArc;
    }


    //TO DO not sure where the bomb information will be polled from
    private static boolean determineIfShipNeedsBombCapability(VassalXWSPilotPieces2e ship, List<XWS2Pilots> allPilots)
    {
        boolean needsBomb = false;
        // if the pilot has a bomb slot
        /*
        MasterPilotData.PilotData pilotData = ship.getPilotData();
        List<String> slots = pilotData.getSlots();
        Iterator<String> slotIterator = slots.iterator();
        String slotName = null;
        while(slotIterator.hasNext() && !needsBomb)
        {
            slotName = slotIterator.next();
            if(slotName.equalsIgnoreCase("Bomb"))
            {
                needsBomb = true;
            }
        }

        // if an upgrade has a grant of bomb slot
        if(!needsBomb)
        {
            List<VassalXWSPilotPieces2e.Upgrade> upgrades = ship.getUpgrades();
            Iterator<VassalXWSPilotPieces2e.Upgrade> upgradeIterator = upgrades.iterator();
            VassalXWSPilotPieces2e.Upgrade upgrade = null;
            Iterator<MasterUpgradeData.UpgradeGrants> grantIterator = null;
            while(upgradeIterator.hasNext() && !needsBomb)
            {
                upgrade = upgradeIterator.next();
                ArrayList<MasterUpgradeData.UpgradeGrants> upgradeGrants;
                try {
                    upgradeGrants = upgrade.getUpgradeData().getGrants();
                }catch(Exception e){
                    logToChat("found the grants null exception.");
                    return false;
                }
                grantIterator = upgradeGrants.iterator();
                MasterUpgradeData.UpgradeGrants grant = null;
                while(grantIterator.hasNext() && !needsBomb)
                {
                    grant = grantIterator.next();
                    if(grant.getType().equalsIgnoreCase("slot") && grant.getName().equalsIgnoreCase("Bomb"))
                    {
                        needsBomb = true;
                    }
                }
            }
        }
*/
        return needsBomb;
    }

    private static PieceSlot getPieceSlotByName(String name)
    {

        List<PieceSlot> pieceSlots = GameModule.getGameModule().getAllDescendantComponentsOf(PieceSlot.class);
        PieceSlot targetPieceSlot = null;
        boolean found = false;

        PieceSlot pieceSlot = null;
        Iterator<PieceSlot> slotIterator = pieceSlots.iterator();
        while(slotIterator.hasNext() && !found)
        {
            pieceSlot = slotIterator.next();

            if (pieceSlot.getConfigureName().startsWith(name)) {
                targetPieceSlot = pieceSlot;
                found = true;
            }
        }
        return targetPieceSlot;
    }

    public static GamePiece generateDial(VassalXWSPilotPieces2e ship, List<XWS2Pilots> allShips)
    {
        XWS2Pilots shipData = null;
        try {
            Util.logToChat("dial creation, must find xwspilotpieces2e ship config name:" + ship.getShip().getConfigureName());
            shipData = XWS2Pilots.getSpecificShip(ship.getShip().getConfigureName(), allShips);
        } catch(Exception e)
        {
            Util.logToChat("couldn't find a ship dial reference for generating its dial");
            return null;
        }
        if(shipData==null) return null;

        String faction = ship.getShipData().getFaction();

        PieceSlot rebelDialSlot = null;
        PieceSlot imperialDialSlot = null;
        PieceSlot scumDialSlot = null;
        PieceSlot firstOrderDialSlot = null;
        PieceSlot resistanceDialSlot = null;

        // find the 3 slots for the auto-gen dials
        List<PieceSlot> pieceSlots = GameModule.getGameModule().getAllDescendantComponentsOf(PieceSlot.class);

        for (PieceSlot pieceSlot : pieceSlots) {
            String slotName = pieceSlot.getConfigureName();
            if (slotName.startsWith("Rebel Stem2e Dial") && rebelDialSlot == null) {
                rebelDialSlot = pieceSlot;
                continue;
            } else if (slotName.startsWith("Imperial Stem2e Dial") && imperialDialSlot == null) {
                imperialDialSlot = pieceSlot;
                continue;
            } else if (slotName.startsWith("Scum Stem2e Dial") && scumDialSlot == null) {
                scumDialSlot = pieceSlot;
                continue;
            } else if (slotName.startsWith("Resistance Stem2e Dial") && resistanceDialSlot == null) {
                resistanceDialSlot = pieceSlot;
                continue;
            } else if (slotName.startsWith("FirstOrder Stem2e Dial") && firstOrderDialSlot == null) {
                firstOrderDialSlot = pieceSlot;
                continue;
            }
        }

        // grab the correct dial for the faction
        GamePiece dial = null;
        if(faction.contentEquals("Rebel Alliance") || faction.contentEquals("Resistance")) {
            dial = Util.newPiece(rebelDialSlot);
        }else if(faction.contentEquals("Galactic Empire") || faction.contentEquals("First Order")) {
            dial = Util.newPiece(imperialDialSlot);
        }else if(faction.contentEquals("Scum and Villainy")) {
            dial = Util.newPiece(scumDialSlot);
        }

        // execute the command
        StemDial.DialGenerateCommand myDialGen = new StemDial.DialGenerateCommand(Canonicalizer.getCleanedName(ship.getShipData().getName()), dial, faction);

        myDialGen.execute();

        dial.setProperty("ShipXwsId",Canonicalizer.getCleanedName(ship.getShipData().getName()));
        return dial;
    }

    public static GamePiece generateUpgrade(VassalXWSPilotPieces2e.Upgrade upgrade)
    {

        GamePiece newUpgrade = Util.newPiece(upgrade.getPieceSlot());
        boolean isDualSided = (upgrade.getUpgradeData().getDualCard() != null);
        StemUpgrade2e.UpgradeGenerateCommand myUpgradeGen = new StemUpgrade2e.UpgradeGenerateCommand(newUpgrade, upgrade, isDualSided);

        myUpgradeGen.execute();

        return newUpgrade;
    }

    public static GamePiece generateCondition(VassalXWSPilotPieces2e.Condition condition)
    {

        GamePiece newCondition = Util.newPiece(condition.getPieceSlot());

        // build the condition card
        StemCondition.ConditionGenerateCommand myConditionGen = new StemCondition.ConditionGenerateCommand(condition.getConditionData().getXws(), newCondition, condition.getConditionData().getName());
        myConditionGen.execute();

        return newCondition;
    }

    public static GamePiece generateConditionToken(VassalXWSPilotPieces2e.Condition condition)
    {
        // get the pieceslot for the StemConditionToken
        List<PieceSlot> pieceSlots = GameModule.getGameModule().getAllDescendantComponentsOf(PieceSlot.class);
        PieceSlot stemConditionTokenPieceSlot = null;
        for (PieceSlot pieceSlot : pieceSlots)
        {
            String slotName = pieceSlot.getConfigureName();
            if(slotName.equals("Stem Condition Token")) {

                stemConditionTokenPieceSlot = pieceSlot;
                break;
            }

        }

        // get a copy of the stem token game piece
        GamePiece conditionTokenPiece = Util.newPiece(stemConditionTokenPieceSlot);



        // build the condition card
        StemConditionToken.TokenGenerateCommand myTokenGen = new StemConditionToken.TokenGenerateCommand(condition.getConditionData().getXws(), conditionTokenPiece);
        myTokenGen.execute();

        return conditionTokenPiece;
    }

    public static GamePiece generatePilot(VassalXWSPilotPieces2e ship, List<XWS2Pilots> allShips) {

        GamePiece newPilot = Util.newPiece(ship.getPilotCard());
        if (ship.getShipNumber() != null && ship.getShipNumber() > 0) {
            newPilot.setProperty("Pilot ID #", ship.getShipNumber());
        } else {
            newPilot.setProperty("Pilot ID #", "");
        }

        // this is a stem card = fill it in

        XWS2Pilots shipData = XWS2Pilots.getSpecificShip(ship.getShip().getConfigureName(), allShips);
        XWS2Pilots.Pilot2e pilotData = XWS2Pilots.getSpecificPilot(ship.getPilotData().getXWS2(), allShips);
        //    newPilot.setProperty("Ship Type",shipData.getName());
        //    newPilot.setProperty("Pilot Name",pilotData.getName());

        StemPilot2e.PilotGenerateCommand myShipGen = new StemPilot2e.PilotGenerateCommand(newPilot, shipData, pilotData);

        myShipGen.execute();

        return newPilot;
    }

    private static String getDisplayPilotName(XWS2Pilots.Pilot2e pilotData, XWS2Pilots shipData, Integer shipNumber )
    {
        String pilotName = "";

        if (pilotData != null) {
            pilotName = Acronymizer.acronymizer(
                    pilotData.getName(),
                    pilotData.isUnique(),
                    shipData.hasSmallBase());
        }

        if (shipNumber != null && shipNumber > 0) {
            pilotName += " " + shipNumber;
        }
        return pilotName;
    }

    private static String getDisplayShipName(XWS2Pilots.Pilot2e pilotData, XWS2Pilots shipData) {
        String shipName = "";

        if (pilotData != null) {
            shipName = Acronymizer.acronymizer(
                    shipData.getName(),
                    pilotData.isUnique(),
                    shipData.hasSmallBase());
        }

        return shipName;
    }
}
