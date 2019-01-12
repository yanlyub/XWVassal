package mic;

import VASSAL.build.GameModule;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.command.ChangeTracker;
import VASSAL.command.Command;
import VASSAL.command.CommandEncoder;
import VASSAL.configure.HotKeyConfigurer;
import VASSAL.counters.*;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static mic.Util.deserializeBase64Obj;
import static mic.Util.logToChat;
import static mic.Util.serializeToBase64;

/**
 * Created by Mic on 04/12/2018.
 *
 * this file is for all programmatic reactions to ',', '.' and 'ctrl-r' commands issued to this new style of dial
 * New style of dial:
 *
 * 1) looks like the open face 2nd edition dial instead of the 1st edition
 * 2) no more additional graphics due to the masked/hidden side - a simple eye-with-slash icon will be used to indicate the hidden mode of the dial (icon at center)
 * 3) when in reveal mode, the selected move (at the top) will be copied, larger, in the center and rotations can't be done at all
 * 4) when in hidden mode, dial rotation commands will only tweak the rotation of the faceplate for the owner of the dial, not for any other player
 * 5) The pilot name in text (white text over black background) above the dial; the ship name in text (same colors) under the dial, no more icon gfx to manage
 * 6) a player can't cheat anymore by swapping mask gfx by a transparent empty png
 * 7) the open face dial has to be kept in OTA2 - no mistakes are allowed because patches can't happen, unless a download all is forced in the content checker
 * 8) File name should be: D2e_'ship name from contracted manifest file names'.jpg
 * 9) Dial graphics should be generated by Mike's tool when he has time to implement it, otherwise generated by me in an outside program or from dialgen + photoshop
 * 10) new dial graphics added when a ship graphic is added in the best case scenario as info pours in from previews.
 */

public class StemNuDial2e extends Decorator implements EditablePiece, Serializable {
    public static final String ID = "StemNuDial2e";

    public StemNuDial2e()  {
        this(null);
    }

    public StemNuDial2e(GamePiece piece)
        {setInner(piece);
        }

    @Override
    public void mySetState(String newState) {

    }
    @Override
    public String myGetState() {
        return "";
    }
    @Override
    public String myGetType() {
        return ID;
    }
    @Override
    protected KeyCommand[] myGetKeyCommands() {
        return new KeyCommand[0];
    }

    @Override
    public Command myKeyEvent(KeyStroke stroke) { return null; }


    public String buildStateString(int moveModification){
        /*
         * dialString, like: 1BW,1FB,1NW,2TW,2BB,2FB,2NB,2YW,3LR,3TW,3BW,3FW,3NW,3YW,3PR,4FR
         * values, like: [1BW,1FB,1NW,2TW,2BB,2FB,2NB,2YW,3LR,3TW,3BW,3FW,3NW,3YW,3PR,4FR]
         * nbOfMoves, like: 15
         *
         * saveMoveString, like: "4" (out of 15)
         * savedMoveStringInt, like: 4
         *
         * access the move in values by using savedMoveStringInt - 1
         * newMove, like: "1TR"
         * newRawSpeed, like 1
         * newMoveSpeed, like 3 (layer 0 = empty, layer 1 = '0', layer 2 = '1', layer 6 = '5')
         * moveWithoutSpeed, like: "TR"
         * moveImage, like: "1TR.png"
         *
         * stateString, like: "emb2...." which is fed moveImage, moveName
         * moveName, like: "Hard Left 1"
         */

        // Fetch the string of movement from the dynamic property and chop it up in an array
        String dialString = piece.getProperty("dialstring").toString();
        String[] values = dialString.split(",");
        int nbOfMoves = values.length;

        // Fetch the saved move from the dynamic property of the dial piece
        String savedMoveString = piece.getProperty("selectedMove").toString();
        int savedMoveStringInt = Integer.parseInt(savedMoveString);

        if(moveModification == 1){ //if you want to shift the selected move 1 up.
            if(savedMoveStringInt == nbOfMoves) savedMoveStringInt = 1; //loop
            else savedMoveStringInt++;
        } else if(moveModification == -1) //if you want to shift the selected move 1 down
        {
            if (savedMoveStringInt == 1) savedMoveStringInt = nbOfMoves; //loop
            else savedMoveStringInt--;
        }

        String moveCode = values[savedMoveStringInt-1];
        int rawSpeed = getRawSpeedFromMoveCode(moveCode);

        //attempt to seed the move layer with the right image just like at spawn time
        StringBuilder stateString = new StringBuilder();
        StringBuilder moveNamesString = new StringBuilder();
        stateString.append("emb2;Activate;2;;;2;;;2;;;;1;false;0;-24;,");

        String moveImage;
        String moveWithoutSpeed = getMoveCodeWithoutSpeed(moveCode);
        String moveName = StemDial2e.maneuverNames.get(getMoveRaw(moveCode));
        moveNamesString.append(moveName).append(" ").append(rawSpeed);

        moveImage = StemDial2e.dialHeadingImages.get(moveWithoutSpeed);
        stateString.append(moveImage);
        // add in move names
        stateString.append(";empty,"+moveNamesString);
        stateString.append(";false;Chosen Move;;;false;;1;1;true;65,130");

        return stateString.toString();
    }

    @Override
    public Command keyEvent(KeyStroke stroke) {
        boolean hasSomethingHappened = false;
        Integer isHiddenPropCheck;

        ChangeTracker changeTracker = new ChangeTracker(this);
        Command result = changeTracker.getChangeCommand();
        result.append(piece.keyEvent(stroke));

        isHiddenPropCheck = Integer.parseInt(piece.getProperty("isHidden").toString());

        Util.logToChat("STEP 0 - keyEvent=" + stroke.getKeyEventType());

        if (getOwnerOfThisDial() == Util.getCurrentPlayer().getSide()) {
            KeyStroke checkForCtrlRReleased = KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK, true);
            KeyStroke checkForCommaReleased = KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, 0, true);
            KeyStroke checkForPeriodReleased = KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, 0, true);

            boolean goingLeft = checkForCommaReleased.equals(stroke);
            boolean goingRight = checkForPeriodReleased.equals(stroke);

            Util.logToChat("STEP 1 - player side verified " + getOwnerOfThisDial());

            if(checkForCtrlRReleased.equals(stroke)) {

                Util.logToChat("STEP 2a - CTRL-R released");

                hasSomethingHappened = true;

                if(isHiddenPropCheck == 1) { // about to reveal the dial

                    //Construct the next build string
                    StringBuilder stateString = new StringBuilder();
                    stateString.append(buildStateString(0));

                    //get the speed layer to show
                    String moveSpeedLayerString = getLayerFromScratch(0);

                    dialRevealCommand revealNow = new dialRevealCommand(piece, stateString.toString(), moveSpeedLayerString);
                    result.append(revealNow);
                    revealNow.execute();

                } else if(isHiddenPropCheck == 0){ // about to hide the dial

                    //command shown to all players
                    dialHideCommand hideNow = new dialHideCommand(piece);
                    result.append(hideNow);
                    hideNow.execute();

                    //Stuff outside of a command, should only show for owner.
                    Embellishment chosenMoveEmb = (Embellishment)Util.getEmbellishment(piece,"Layer - Chosen Move");
                    Embellishment chosenSpeedEmb = (Embellishment)Util.getEmbellishment(piece, "Layer - Chosen Speed");
                    Embellishment sideHideEmb = (Embellishment)Util.getEmbellishment(piece,"Layer - Side Hide");
                    Embellishment centralHideEmb = (Embellishment)Util.getEmbellishment(piece, "Layer - Central Hide");

                    //Construct the next build string
                    StringBuilder stateString = new StringBuilder();
                    stateString.append(buildStateString(0));

                    chosenMoveEmb.mySetType(stateString.toString());
                    chosenMoveEmb.setValue(1);
                    sideHideEmb.setValue(1);
                    centralHideEmb.setValue(0);

                    //get the speed layer to show
                    String moveSpeedLayerString = getLayerFromScratch(0);
                    Integer newMoveSpeed = Integer.parseInt(moveSpeedLayerString);

                    chosenSpeedEmb.setValue(newMoveSpeed);
                }
            }
            else if(goingLeft || goingRight){ //rotate left, move-- or rotate right, move++

                Util.logToChat("STEP 2b - , or . released");


                hasSomethingHappened = true;
                int moveMod = 0;
                if(goingLeft) moveMod = -1;
                if(goingRight) moveMod = 1;

                //Construct the next build string
                StringBuilder stateString = new StringBuilder();
                stateString.append(buildStateString(moveMod));

                //Get the movement heading layer
                String moveDef = getNewMoveDefFromScratch(moveMod);
                //get the speed layer to show
                String moveSpeedLayerString = getLayerFromScratch(moveMod);
                Integer newMoveSpeed = Integer.parseInt(moveSpeedLayerString);

                if(piece.getMap().equals(VASSAL.build.module.Map.getMapById("Map0"))) logToChat("* DIAL WARNING - " + Util.getCurrentPlayer().getName() + " has rotated the " + piece.getProperty("Craft ID #").toString()
                        + " (" + piece.getProperty("Pilot Name").toString() + ") on the map. Please use your player window to do so instead.");
                if(isHiddenPropCheck == 1){ //encode only the modified selected move property


                    dialRotateCommand drc = new dialRotateCommand(piece, moveDef, false, stateString.toString(), moveSpeedLayerString);
                    drc.execute();

                    Embellishment chosenMoveEmb = (Embellishment)Util.getEmbellishment(piece,"Layer - Chosen Move");
                    Embellishment chosenSpeedEmb = (Embellishment)Util.getEmbellishment(piece, "Layer - Chosen Speed");
                    chosenMoveEmb.mySetType(stateString.toString());
                    chosenMoveEmb.setValue(1);
                    chosenSpeedEmb.setValue(newMoveSpeed);
                } else if(isHiddenPropCheck == 0) { //dial is revealed, show everything to all

                    dialRotateCommand drc = new dialRotateCommand(piece, moveDef, true, stateString.toString(), moveSpeedLayerString);
                    result.append(drc);
                    drc.execute();
                }
            }
        } else { // get scolded for not owning the dial that was manipulated
            Util.logToChatWithoutUndo("You (player " + Util.getCurrentPlayer().getSide() + ") are not the owner of this dial, player " + getOwnerOfThisDial() + " is.");
        }
        if(hasSomethingHappened) {
            final VASSAL.build.module.Map map = piece.getMap();
            map.repaint();
            return result;
        }
        Util.logToChat("STEP 2c - Not a keystroke worth reacting to.");
        return piece.keyEvent(stroke);
    }

    private String getNewMoveDefFromScratch(int moveMod) {
        String dialString = piece.getProperty("dialstring").toString();
        String[] values = dialString.split(",");
        int nbOfMoves = values.length;

        // Fetch the saved move from the dynamic property of the dial piece
        String savedMoveString = piece.getProperty("selectedMove").toString();
        int savedMoveStringInt = Integer.parseInt(savedMoveString);

        if(moveMod == 1){ //if you want to shift the selected move 1 up.
            if(savedMoveStringInt == nbOfMoves) savedMoveStringInt = 1; //loop
            else savedMoveStringInt++;
        } else if(moveMod == -1) //if you want to shift the selected move 1 down
        {
            if (savedMoveStringInt == 1) savedMoveStringInt = nbOfMoves; //loop
            else savedMoveStringInt--;
        }

        return ""+savedMoveStringInt;
    }


    public int getRawSpeedFromMoveCode(String code){
        return Integer.parseInt(code.substring(0,1));
    }
    public int getLayerFromMoveCode(String code){
        return Integer.parseInt(code.substring(0,1)) + 1;
    }
    public String getLayerFromScratch(int moveModification){
        String dialString = piece.getProperty("dialstring").toString();
        String[] values = dialString.split(",");
        int nbOfMoves = values.length;

        // Fetch the saved move from the dynamic property of the dial piece
        String savedMoveString = piece.getProperty("selectedMove").toString();
        int savedMoveStringInt = Integer.parseInt(savedMoveString);

        if(moveModification == 1){ //if you want to shift the selected move 1 up.
            if(savedMoveStringInt == nbOfMoves) savedMoveStringInt = 1; //loop
            else savedMoveStringInt++;
        } else if(moveModification == -1) //if you want to shift the selected move 1 down
        {
            if (savedMoveStringInt == 1) savedMoveStringInt = nbOfMoves; //loop
            else savedMoveStringInt--;
        }

        String moveCode = values[savedMoveStringInt-1];
        Integer moveSpeedLayerToUse = getLayerFromMoveCode(moveCode);
        String moveSpeedLayerString = moveSpeedLayerToUse.toString();

        return moveSpeedLayerString;
    }
    public String getMoveCodeWithoutSpeed(String code){
        return code.substring(1,3);
    }

    public String getMoveRaw(String code){
        return code.substring(1,2);
    }



    public int getOwnerOfThisDial(){
        GamePiece dialPiece = (GamePiece)this.piece;
        String ownerStr = dialPiece.getProperty("owner").toString();
        int ownerInt = Integer.parseInt(ownerStr);
        return ownerInt;
    }
    public String getDescription() {
        return "Custom StemNuDial (mic.StemNuDial2e)";
    }

    public void mySetType(String type) {

    }

    public HelpFile getHelpFile() {
        return null;
    }

    public void draw(Graphics g, int x, int y, Component obs, double zoom) {
        this.piece.draw(g, x, y, obs, zoom);
    }

    public Rectangle boundingBox() {
        return this.piece.boundingBox();
    }

    public Shape getShape() {
        return this.piece.getShape();
    }

    public String getName() {
        return this.piece.getName();
    }

    public static class dialRevealCommand extends Command {
        static GamePiece pieceInCommand;
        static String moveDef;
        static String speedLayer;

        dialRevealCommand(GamePiece piece, String requiredMoveDef, String requiredSpeedLayer){
            pieceInCommand = piece;
            moveDef = requiredMoveDef;
            speedLayer = requiredSpeedLayer;
        }

        protected void executeCommand() {
            Embellishment chosenMoveEmb = (Embellishment) Util.getEmbellishment(pieceInCommand, "Layer - Chosen Move");
            Embellishment chosenSpeedEmb = (Embellishment) Util.getEmbellishment(pieceInCommand, "Layer - Chosen Speed");
            Embellishment sideHideEmb = (Embellishment) Util.getEmbellishment(pieceInCommand, "Layer - Side Hide");
            Embellishment centralHideEmb = (Embellishment) Util.getEmbellishment(pieceInCommand, "Layer - Central Hide");
            chosenMoveEmb.mySetType(moveDef);
            chosenMoveEmb.setValue(1); // use the layer that shows the move
            sideHideEmb.setValue(0); //hide the small slashed eye icon
            centralHideEmb.setValue(0); //hide the central slashed eye icon
            chosenSpeedEmb.setValue(Integer.parseInt(speedLayer)); //use the right speed layer
            pieceInCommand.setProperty("isHidden", "0");

            if(pieceInCommand.getMap().equals(VASSAL.build.module.Map.getMapById("Map0"))) Util.logToChatCommand("* - "+ Util.getCurrentPlayer().getName()+ " reveals the dial for "
                    + pieceInCommand.getProperty("Craft ID #").toString() + " (" + pieceInCommand.getProperty("Pilot Name").toString() + ") = "+ chosenMoveEmb.getProperty("Chosen Move_Name") + "*");


            Util.logToChat("STEP 4a - Revealed the dial with " + chosenMoveEmb.getProperty("Chosen Move_Name"));
            final VASSAL.build.module.Map map = pieceInCommand.getMap();
            map.repaint();
        }
        protected Command myUndoCommand() {
            return null;
        }

        public static class Dial2eRevealEncoder implements CommandEncoder {
            private static final Logger logger = LoggerFactory.getLogger(StemNuDial2e.class);
            private static final String commandPrefix = "Dial2eRevealEncoder=";
            private static final String itemDelim = "\t";

            public static StemNuDial2e.dialRevealCommand.Dial2eRevealEncoder INSTANCE = new StemNuDial2e.dialRevealCommand.Dial2eRevealEncoder();

            public Command decode(String command){
                if(command == null || !command.contains(commandPrefix)) {
                    return null;
                }

                command = command.substring(commandPrefix.length());
                String[] parts = command.split(itemDelim);

                try{
                    Collection<GamePiece> pieces = GameModule.getGameModule().getGameState().getAllPieces();
                    for (GamePiece piece : pieces) {
                        if(piece.getId().equals(parts[0])) {

                            logToChat("Step 3a - Reveal Decoder " + pieceInCommand.getId() + " " + parts[1] + " " + parts[2]);
                            return new dialRevealCommand(piece, parts[1], parts[2]);
                        }
                    }

                }catch(Exception e){

                    return null;
                }


                return null;
            }

            public String encode(Command c){
                if (!(c instanceof StemNuDial2e.dialRevealCommand)) {
                    return null;
                }
                try{
                    return commandPrefix + Joiner.on(itemDelim).join(pieceInCommand.getId(), moveDef,speedLayer);
                }catch(Exception e) {
                    logger.error("Error encoding dialRevealCommand", e);
                    return null;
                }

            }
        }
    }
    public static class dialHideCommand extends Command {
        static GamePiece pieceInCommand;

        dialHideCommand(GamePiece piece) {
            pieceInCommand = piece;
        }

        protected void executeCommand() {
            Embellishment chosenMoveEmb = (Embellishment)Util.getEmbellishment(pieceInCommand,"Layer - Chosen Move");
            Embellishment chosenSpeedEmb = (Embellishment)Util.getEmbellishment(pieceInCommand, "Layer - Chosen Speed");
            Embellishment centralHideEmb = (Embellishment)Util.getEmbellishment(pieceInCommand, "Layer - Central Hide");
            chosenMoveEmb.setValue(0);
            chosenSpeedEmb.setValue(0);
            centralHideEmb.setValue(1);
            pieceInCommand.setProperty("isHidden", "1");
            Util.logToChat("STEP 4b - Hid the dial");
            final VASSAL.build.module.Map map = pieceInCommand.getMap();
            map.repaint();
        }

        protected Command myUndoCommand() {
            return null;
        }

        //the following class is used to send the info to the other player whenever a dial generation command is issued, so it can be done locally on all machines playing/watching the game
        //only the ship XWS string is sent
        public static class Dial2eHideEncoder implements CommandEncoder {
            private static final Logger logger = LoggerFactory.getLogger(StemNuDial2e.class);
            private static final String commandPrefix = "Dial2eHideEncoder=";

            public static StemNuDial2e.dialHideCommand.Dial2eHideEncoder INSTANCE = new StemNuDial2e.dialHideCommand.Dial2eHideEncoder();

            public Command decode(String command) {
                if (command == null || !command.contains(commandPrefix)) {
                    return null;
                }
                logger.info("Decoding dialHideCommand");

                command = command.substring(commandPrefix.length());

                try{
                    Collection<GamePiece> pieces = GameModule.getGameModule().getGameState().getAllPieces();
                    logToChat("Step 3b prep - piece.getId " + pieceInCommand.getId() + " and command " + command);
                    for (GamePiece piece : pieces) {
                        if(piece.getId().equals(command)) {

                            logToChat("Step 3b - Hide Encoder " + pieceInCommand.getId());

                            return new dialHideCommand(piece);
                        }
                    }
                }catch(Exception e){
                    return null;
                }


                return null;
            }

            public String encode(Command c) {
                if (!(c instanceof StemNuDial2e.dialHideCommand)) {
                    return null;
                }
                logger.info("Encoding dialHideCommand");
                StemNuDial2e.dialHideCommand dhc = (StemNuDial2e.dialHideCommand) c;
                try {
                    return commandPrefix + pieceInCommand.getId();
                } catch(Exception e) {
                    logger.error("Error encoding dialHideCommand", e);
                    return null;
                }
            }
        }
    }

    public static class dialRotateCommand extends Command {
        static GamePiece pieceInCommand;
        static String moveDef;
        static boolean showEverything = false;
        static String stateString;
        static String moveSpeedLayerString;

        dialRotateCommand(GamePiece piece, String selectedMove, boolean wantShowEverything, String reqStateString, String reqMoveSpeedLayerString) {
            pieceInCommand = piece;
            moveDef = selectedMove;
            showEverything = wantShowEverything;
            stateString = reqStateString;
            moveSpeedLayerString = reqMoveSpeedLayerString;
        }

        protected void executeCommand() {
                pieceInCommand.setProperty("selectedMove", moveDef);

                if(showEverything == true){
                    Embellishment chosenMoveEmb = (Embellishment)Util.getEmbellishment(pieceInCommand,"Layer - Chosen Move");
                    Embellishment chosenSpeedEmb = (Embellishment)Util.getEmbellishment(pieceInCommand, "Layer - Chosen Speed");

                    chosenMoveEmb.mySetType(stateString);
                    chosenMoveEmb.setValue(1);
                    chosenSpeedEmb.setValue(Integer.parseInt(moveSpeedLayerString));


                    Util.logToChat("STEP 4c - Rotated the dial while revealed ");

                    final VASSAL.build.module.Map map = pieceInCommand.getMap();
                    map.repaint();
                }
                else {
                    Util.logToChat("STEP 4d - Rotated the dial while hidden");
                }

        }


        protected Command myUndoCommand() {
            return null;
        }

        //the following class is used to send the info to the other player whenever a dial generation command is issued, so it can be done locally on all machines playing/watching the game
        //only the ship XWS string is sent
        public static class Dial2eRotateEncoder implements CommandEncoder {
            private static final Logger logger = LoggerFactory.getLogger(StemNuDial2e.class);
            private static final String commandPrefix = "Dial2eRotateEncoder=";
            private static final String itemDelim = "\t";

            public static StemNuDial2e.dialRotateCommand.Dial2eRotateEncoder INSTANCE = new StemNuDial2e.dialRotateCommand.Dial2eRotateEncoder();

            public Command decode(String command){
                if(command == null || !command.contains(commandPrefix)) {
                    return null;
                }

                command = command.substring(commandPrefix.length());
                String[] parts = command.split(itemDelim);

                try{
                    Collection<GamePiece> pieces = GameModule.getGameModule().getGameState().getAllPieces();
                    for (GamePiece piece : pieces) {
                        if(piece.getId().equals(parts[0])) {

                            Util.logToChat("STEP 3c - Rotate decoder " + parts[1] + " " + Boolean.parseBoolean(parts[2]) +" " + parts[3] +" "+ parts[4]);
                            return new dialRotateCommand(piece, parts[1], Boolean.parseBoolean(parts[2]), parts[3], parts[4]);
                        }
                    }

                }catch(Exception e){
                    logger.error("Error decoding Dial2eRotateEncoder", e);
                    return null;
                }
                return null;
            }

            public String encode(Command c){
                if (!(c instanceof StemNuDial2e.dialRotateCommand)) {
                    return null;
                }
                try{

                    return commandPrefix + Joiner.on(itemDelim).join(pieceInCommand.getId(), moveDef, ""+showEverything, stateString, moveSpeedLayerString);
                }catch(Exception e) {
                    logger.error("Error encoding Dial2eRotateEncoder", e);
                    return null;
                }
            }
        }

    }




}
