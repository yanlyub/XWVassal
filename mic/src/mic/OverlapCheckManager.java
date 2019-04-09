package mic;

import VASSAL.build.AbstractConfigurable;
import VASSAL.build.Buildable;
import VASSAL.build.GameModule;
import VASSAL.build.module.Map;
import VASSAL.build.module.documentation.HelpFile;
import VASSAL.counters.Decorator;
import VASSAL.counters.GamePiece;
import com.google.common.collect.Lists;

import java.util.List;

import static VASSAL.build.AutoConfigurable.Util.*;
import static mic.Util.getTheMainMap;

/**
 * Created by mjuneau on 2019-04-09
 *
 * This is an attempt to centralize overlap checks, which have gone rampant in the code. Off the top of my head, here are
 * the files that need to verify overlaps with all sorts of things at some point in their processes:
 *
 * AutoBumpDecorator (which is now a misnomer, it manages movement, not just bump resolution)
 * ShipReposition
 * TemplateOverlapCheckDecorator
 * RemoteRelocation
 *
 */

public class OverlapCheckManager extends AbstractConfigurable {
    Map theMap;


    public List<BumpableWithShape> getBumpablesOnMap(Boolean wantShipsToo) {

        List<BumpableWithShape> bumpables = Lists.newArrayList();

        GamePiece[] pieces = theMap.getAllPieces();
        for (GamePiece piece : pieces) {
            if (piece.getState().contains("this_is_an_asteroid")) {
                // comment out this line and the next three that add to bumpables if bumps other than with ships shouldn't be detected yet
                String testFlipString = "";
                try{
                    testFlipString = ((Decorator) piece).getDecorator(piece,piece.getClass()).getProperty("whichShape").toString();
                } catch (Exception e) {}
                bumpables.add(new BumpableWithShape((Decorator)piece, "Asteroid", "2".equals(testFlipString), false));
            } else if (piece.getState().contains("this_is_a_debris")) {
                String testFlipString = "";
                try{
                    testFlipString = ((Decorator) piece).getDecorator(piece,piece.getClass()).getProperty("whichShape").toString();
                } catch (Exception e) {}
                bumpables.add(new BumpableWithShape((Decorator)piece,"Debris","2".equals(testFlipString), false));
            } else if (piece.getState().contains("this_is_a_bomb")) {
                bumpables.add(new BumpableWithShape((Decorator)piece, "Mine", false, false));
            } else if (piece.getState().contains("this_is_a_gascloud")) {
                String testFlipString = "";
                try{
                    testFlipString = ((Decorator) piece).getDecorator(piece,piece.getClass()).getProperty("whichShape").toString();
                } catch (Exception e) {}
                bumpables.add(new BumpableWithShape((Decorator)piece, "GasCloud", "2".equals(testFlipString), false));
            }else if (piece.getState().contains("this_is_a_remote")) {
                String testFlipString = "";
                try{
                    testFlipString = ((Decorator) piece).getDecorator(piece,piece.getClass()).getProperty("whichShape").toString();
                } catch (Exception e) {}
                bumpables.add(new BumpableWithShape((Decorator)piece, "Remote", "2".equals(testFlipString), false));
            }else if(wantShipsToo == true && piece.getState().contains("this_is_a_ship")){
                BumpableWithShape tentativeBumpable = new BumpableWithShape((Decorator)piece, "Ship",false,
                        this.getInner().getState().contains("this_is_2pointoh"));
                if (getId().equals(tentativeBumpable.bumpable.getId())) {
                    continue;
                }
                bumpables.add(tentativeBumpable);

            }
        }
        return bumpables;
    }



    public String[] getAttributeDescriptions() {
        return new String[0];
    }

    public Class<?>[] getAttributeTypes() {
        return new Class[0];
    }

    public String[] getAttributeNames() {
        return new String[0];
    }

    public void setAttribute(String key, Object value) {

    }

    public String getAttributeValueString(String key) {
        return null;
    }

    public void removeFrom(Buildable parent) {

    }

    public HelpFile getHelpFile() {
        return null;
    }

    public Class[] getAllowableConfigureComponents() {
        return new Class[0];
    }

    public void addTo(Buildable parent) {
        theMap = getTheMainMap();
    }
}
