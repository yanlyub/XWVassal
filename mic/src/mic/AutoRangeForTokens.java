
package mic;

import VASSAL.build.module.documentation.HelpFile;
import VASSAL.command.Command;
import VASSAL.configure.HotKeyConfigurer;
import VASSAL.counters.*;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static mic.Util.*;

/**
 * Created by Mic on 23/03/2017.
 */

enum ImagesUsedForRanges {
    ProbeDroid("Probe-range1","Probe-range2","Probe-range");

    private final String r1Img;
    private final String r2Img;
    private final String r3Img;

    ImagesUsedForRanges(String r1, String r2, String r3){
        r1Img = r1;
        r2Img = r2;
        r3Img = r3;
    }

    public String getR1Img(){ return r1Img+".png"; }
    public String getR2Img(){ return r2Img+".png"; }
    public String getR3Img(){ return r3Img+".png"; }
}

public class AutoRangeForTokens extends Decorator implements EditablePiece {

    protected VASSAL.build.module.Map map;
    private static final int findObstaclesShips = 1;
    private static final int findShips = 2;
    private static final int findObstacles = 3;

    private Shape shapeForRange1;
    private Shape shapeForRange2;
    private Shape shapeForRange3;

    String bigAnnounce = "";
    private final FreeRotator testRotator;

    int whichOption = -1; //which autorange option. See the Map above. -1 if none is selected.

    public static final String ID = "RangeForObstacles";

    private static Map<String, Integer> keyStrokeToOptions = ImmutableMap.<String, Integer>builder()
            .put("CTRL SHIFT L", findObstaclesShips) //find obstacles and ships
            .put("CTRL SHIFT S", findShips) //find only ships
            .put("CTRL SHIFT O", findObstacles) //find only obstacles
            .build();
    private static Map<String, ImagesUsedForRanges> pieceNameToImgs = ImmutableMap.<String, ImagesUsedForRanges>builder()
            .put("DRK-1 Probe Droid (this_is_a_remote)", ImagesUsedForRanges.ProbeDroid)
            .build();

    public AutoRangeForTokens() {
        this(null);
    }
    public AutoRangeForTokens(GamePiece piece) {
        setInner(piece);
        this.testRotator = new FreeRotator("rotate;360;;;;;;;", null);
        // launch();
        map = VASSAL.build.module.Map.getMapById("Map0");
    }

    public void mySetState(String newState) {

    }

    public String myGetState() {
        return "";
    }

    public String myGetType() {
        return ID;
    }

    private int getKeystrokeToOptions(KeyStroke keyStroke) {
        String hotKey = HotKeyConfigurer.getString(keyStroke);
        if (keyStrokeToOptions.containsKey(hotKey)) {
            return keyStrokeToOptions.get(hotKey).intValue();
        }
        return -1;
    }
    private String getAssociatedImage(String name, int range){
        if(pieceNameToImgs.containsKey(name)){
            switch(range){
                case 1:
                    return pieceNameToImgs.get(name).getR1Img();
                case 2:
                    return pieceNameToImgs.get(name).getR2Img();
                case 3:
                default:
                    return pieceNameToImgs.get(name).getR3Img();
            }
        }
        return "";
    }

    protected KeyCommand[] myGetKeyCommands() {
        return new KeyCommand[0];
    }

    public Command myKeyEvent(KeyStroke stroke) {
        return null;
    }

    public Command keyEvent(KeyStroke stroke) {

        ArrayList<RangeFindings> rfindings = new ArrayList<RangeFindings>(); //findings compiled here

        // check to see if the this code needs to respond to the event
        //identify which autorange option was used by using the static Map defined above in the globals, store it in an int
        whichOption = getKeystrokeToOptions(stroke);

        //we have a valid keystroke option, let's deal with it
        if(whichOption != -1 && stroke.isOnKeyRelease()==false){
            verifyBasicShapes();
            prepAnnouncementStart();
            Shape shapeOfPieceItself = getRawShape(this);;
            //logToChat("ARFT line 132 piece name " + this.piece.getName());

            //add the ships to the detection
            if(whichOption == findShips || whichOption == findObstaclesShips) {

                Shape tShapeR0 = getTransformedShape(shapeOfPieceItself);
                Shape tShapeR1 = getTransformedShape(shapeForRange1);
                Shape tShapeR2 = getTransformedShape(shapeForRange2);
                Shape tShapeR3 = getTransformedShape(shapeForRange3);

                //Loop over every other target ship
                List<BumpableWithShape> BWS = getOtherShipsOnMap();
                for (BumpableWithShape b : BWS) {
                    //Range 0 check
                    if (shapesOverlap(tShapeR0, b.shape)) {
                        rfindings.add(new RangeFindings(0, b.pilotName));
                        continue;
                    }
                    //Range 1 check
                    if(shapeForRange1 !=null && shapesOverlap(tShapeR1, b.shape)){
                        rfindings.add(new RangeFindings(1, b.pilotName));
                        continue;
                    }
                    //Range 2 check
                    if(shapeForRange2 !=null && shapesOverlap(tShapeR2,  b.shape)){
                        rfindings.add(new RangeFindings(2, b.pilotName));
                        continue;
                    }
                    //Range 3 check
                    if(shapeForRange3 !=null && shapesOverlap(tShapeR3, b.shape)){
                        rfindings.add(new RangeFindings(3, b.pilotName));
                    }
                }
            }

            //add the obstacles to the detection
            if(whichOption == findObstacles || whichOption == findObstaclesShips) {
            }

            makeBigAnnounceCommand(bigAnnounce, rfindings);
        }

        return piece.keyEvent(stroke); //did not find anything worth react to, so send back the key for others to deal with it
    }

    private void verifyBasicShapes() {
        if(shapeForRange1==null || shapeForRange2 ==null || shapeForRange3 ==null) logToChat("* -- Generating shapes for the first range check of this token, this will take a few seconds (and will only be done the first time).");

        if(shapeForRange1==null){
            shapeForRange1 = getRawShapeFromFile(getAssociatedImage(this.piece.getName(),1));
        }
        if(shapeForRange2==null){
            shapeForRange2 = getRawShapeFromFile(getAssociatedImage(this.piece.getName(),2));
        }
        if(shapeForRange3==null){
            shapeForRange3 = getRawShapeFromFile(getAssociatedImage(this.piece.getName(),3));
        }

    }

    private Shape getRawShapeFromFile(String fileName){
        Shape finalShape;
        if(fileName.equals("")) return null;
        finalShape = Util.getImageShape(fileName);

        return finalShape;
    }

    private Shape getTransformedShape(Shape rawShape){
        //Info Gathering: gets the angle from repoTemplate which deals with degrees, local space with ship at 0,0, pointing up
        double globalShipAngle = this.testRotator.getAngle(); //ship angle

        //Step 1: Translation
        double offx = this.getPosition().getX();
        double offy = this.getPosition().getY();

        //STEP 2: translation into place
        Shape transformedShape = AffineTransform.
                getTranslateInstance((int)offx, (int)offy).
                createTransformedShape(rawShape);
        double roundedAngle = convertAngleToGameLimits(globalShipAngle);
        transformedShape = AffineTransform
                .getRotateInstance(Math.toRadians(-roundedAngle), (int)offx, (int)offy)
                .createTransformedShape(transformedShape);

        return transformedShape;
    }

    private double convertAngleToGameLimits(double angle) {
        this.testRotator.setAngle(angle);
        return this.testRotator.getAngle();
    }

    private void prepAnnouncementStart() {
        //prepares the initial part of the chat window string that announces the firing options
        //bigAnnounce = "*** Calculating ranges from " + this.piece.getName() + " to ";
        String originPieceName = this.piece.getName();
        String[] parts = originPieceName.split("\\(");
        String finalName = (parts.length>1?parts[0]:originPieceName);
        String firstAnnounce = "*** Calculating ranges to " + finalName;

        switch (whichOption) {
            case findObstaclesShips:
                firstAnnounce += "ships and obstacles.";
                break;
            case findShips:
                firstAnnounce += "ships.";
                break;
            case findObstacles:
                firstAnnounce += "obstacles.";
                break;
        }
        logToChat(firstAnnounce);
    }
    private void makeBigAnnounceCommand(String bigAnnounce, ArrayList<RangeFindings> rfindings) {
        String range1String = "";
        String range2String = "";
        String range3String = "";
        String range0String = "";

        boolean hasR1 = false;
        boolean hasR2 = false;
        boolean hasR3 = false;
        boolean hasR0 = false;

        for (RangeFindings rf : rfindings) {
            if (rf.range == 0) {
                hasR0 = true;
                range0String += rf.fullName + " | ";
            }
            if (rf.range == 1) {
                hasR1 = true;
                range1String += rf.fullName + " | ";
            }
            if (rf.range == 2) {
                hasR2 = true;
                range2String += rf.fullName + " | ";
            }
            if (rf.range == 3) {
                hasR3 = true;
                range3String += rf.fullName + " | ";
            }
        }

        String result = bigAnnounce + (hasR0 ? "*** Range 0: " + range0String + "\n" : "") +
                (hasR1 ? "*** Range 1: " + range1String + "\n" : "") +
                (hasR2 ? "*** Range 2: " + range2String + "\n" : "") +
                (hasR3 ? "*** Range 3: " + range3String + "\n" : "");
        if (hasR0 == false && hasR1 == false && hasR2 == false && hasR3 == false) result = "Nothing in range.";

        logToChat(result);
    }
    private List<BumpableWithShape> getOtherShipsOnMap() {
        List<BumpableWithShape> ships = Lists.newArrayList();

        GamePiece[] pieces = getMap().getAllPieces();
        for (GamePiece piece : pieces) {
            if (piece.getState().contains("this_is_a_ship") && piece.getId() != this.piece.getId()) {
                ships.add(new BumpableWithShape((Decorator)piece, "Ship",
                        piece.getProperty("Pilot Name").toString(), piece.getProperty("Craft ID #").toString(),
                        piece.getState().contains("this_is_2pointoh")));
            }
        }
        return ships;
    }


    public String getDescription() {
        return "Custom range detection for tokens (mic.AutoRangeForTokens)";
    }

    public void mySetType(String type) {

    }

    public HelpFile getHelpFile() {
        return null;
    }

    public void draw(Graphics graphics, int i, int i1, Component component, double v) {
        this.piece.draw(graphics, i, i1, component, v);
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

    public static class RangeFindings {
        public int range=0;
        public String fullName="";
        public boolean isObstructed=false;

        public RangeFindings(){ /* To allow serizliation */ }
        public RangeFindings(int range, String fullName){
            this.fullName = fullName;
            this.range = range;
            this.isObstructed = false;
        }
    }


}