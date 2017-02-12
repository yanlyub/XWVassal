package mic;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by guido on 11/02/17.
 */
public class MasterUpgradeData extends ArrayList<MasterUpgradeData.UpgradeData> {

  private static Map<String, UpgradeData> loadedData = null;

  public static Map<String, UpgradeData> getUpgradeDataByXWSId() {
      if (loadedData != null) {
          return loadedData;
      }

      loadedData = Maps.newHashMap();
      MasterUpgradeData data = Util.loadRemoteJson(
          "https://raw.githubusercontent.com/guidokessels/xwing-data/master/data/upgrades.js",
          MasterUpgradeData.class);

      for(UpgradeData upgrade : data) {
          loadedData.put(upgrade.getXws(), upgrade);
      }
      return loadedData;
  }

  public static class UpgradeData {

      @JsonProperty("name")
      private String name;

      @JsonProperty("xws")
      private String xws;

      @JsonProperty("grants")
      private ArrayList<UpgradeGrants> grants = Lists.newArrayList();

      public ArrayList<UpgradeGrants> getGrants() {
          return grants;
      }

      public String getXws() {
          return xws;
      }

      public String getName() {
          return name;
      }
  }

  public static class UpgradeGrants {

      @JsonProperty("name")
      private String name;

      @JsonProperty("type")
      private String type;

      @JsonProperty("value")
      private int value;

      public String getName() {
          return name;
      }

      public String getType() {
          return type;
      }

      public int getValue() {
          return value;
      }

      public boolean isStatsModifier() {
          return type.equals("stats");
      }
  };
}
