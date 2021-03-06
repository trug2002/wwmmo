package au.com.codeka.warworlds.server.admin.handlers;

import java.util.ArrayList;
import java.util.List;

import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.proto.Empire;
import au.com.codeka.warworlds.common.proto.SectorCoord;
import au.com.codeka.warworlds.server.admin.RequestException;
import au.com.codeka.warworlds.server.store.DataStore;
import au.com.codeka.warworlds.server.world.EmpireManager;
import au.com.codeka.warworlds.server.world.WatchableObject;
import au.com.codeka.warworlds.server.world.generator.NewStarFinder;

/** Handler for /admin/ajax/sectors requests. */
public class AjaxSectorsHandler extends AjaxHandler {
  private static final Log log = new Log("AjaxSectorsHandler");

  @Override
  public void get() throws RequestException {
    switch (getRequest().getParameter("action")) {
      case "find-empty":
        handleFindEmptyRequest();
        break;
      case "create-empire":
        handleCreateEmpireRequest();
        break;
      default:
        throw new RequestException(400, "Unknown action: " + getRequest().getParameter("action"));
    }
  }

  private void handleFindEmptyRequest() {
    setResponseJson(DataStore.i.sectors().getEmptySector());
  }

  private void handleCreateEmpireRequest() {
    String name = getRequest().getParameter("name");
    String xs = getRequest().getParameter("x");
    String ys = getRequest().getParameter("y");

    CreateEmpireResponse resp = new CreateEmpireResponse();
    resp.empireName = name;

    SectorCoord coord;
    if (xs != null && ys != null) {
      coord = new SectorCoord.Builder().x(Long.parseLong(xs)).y(Long.parseLong(ys)).build();
    } else {
      coord = DataStore.i.sectors().getEmptySector();
      if (coord == null) {
        resp.log("No empty sector found.");
        setResponseGson(resp);
        return;
      }
    }
    resp.sectorX = coord.x;
    resp.sectorY = coord.y;

    NewStarFinder newStarFinder = new NewStarFinder(new Log(resp::log), coord);
    if (!newStarFinder.findStarForNewEmpire()) {
      resp.log("No star found.");
      setResponseGson(resp);
      return;
    }

    WatchableObject<Empire> empire = EmpireManager.i.createEmpire(name, newStarFinder);
    if (empire == null) {
      resp.log("Failed to create empire.");
    } else {
      resp.empire = empire.get();
    }

    setResponseGson(resp);
  }

  /** Class that's sent to the client via Gson-encoder. */
  private static class CreateEmpireResponse {
    String empireName;
    long sectorX;
    long sectorY;
    List<String> logs = new ArrayList<>();
    Empire empire;

    public void log(String msg) {
      logs.add(msg);
    }
  }
}
