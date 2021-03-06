package au.com.codeka.warworlds.client.game.solarsystem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.squareup.picasso.Picasso;

import au.com.codeka.warworlds.client.R;
import au.com.codeka.warworlds.client.util.ViewBackgroundGenerator;
import au.com.codeka.warworlds.client.game.world.ImageHelper;
import au.com.codeka.warworlds.common.Log;
import au.com.codeka.warworlds.common.Vector2;
import au.com.codeka.warworlds.common.proto.Building;
import au.com.codeka.warworlds.common.proto.Planet;
import au.com.codeka.warworlds.common.proto.Star;

/**
 * The view which displays the star and planets.
 */
public class SolarSystemView extends RelativeLayout {
  public interface PlanetSelectedHandler {
    void onPlanetSelected(Planet planet);
  }

  private static final Log log = new Log("SolarSystemView");
  private final Paint orbitPaint;
  private Star star;
  private PlanetInfo[] planetInfos;
  private ImageView selectionIndicator;
  @Nullable private Planet selectedPlanet;
  @Nullable private PlanetSelectedHandler planetSelectedHandler;

  public SolarSystemView(Context context, AttributeSet attrs) {
    super(context, attrs);
    orbitPaint = new Paint();
    orbitPaint.setARGB(255, 255, 255, 255);
    orbitPaint.setStyle(Paint.Style.STROKE);

    selectionIndicator = new ImageView(context);
    selectionIndicator.setImageResource(R.drawable.planet_selection);
    selectionIndicator.setVisibility(View.GONE);
  }

  public void setPlanetSelectedHandler(@Nullable PlanetSelectedHandler handler) {
    planetSelectedHandler = handler;
  }

  public Vector2 getPlanetCentre(Planet planet) {
    return planetInfos[planet.index].centre;
  }

  /** Gets the {@link ImageView} that displays the given planet's icon. */
  public ImageView getPlanetView(Planet planet) {
    return planetInfos[planet.index].imageView;
  }

  public void setStar(Star star) {
    ViewBackgroundGenerator.setBackground(this, onBackgroundDrawHandler, star.id);
    removeAllViews();
    addView(selectionIndicator);

    this.star = star;
    planetInfos = new PlanetInfo[star.planets.size()];
    for (int i = 0; i < star.planets.size(); i++) {
      PlanetInfo planetInfo = new PlanetInfo();
      planetInfo.planet = star.planets.get(i);
      planetInfo.centre = new Vector2(0, 0);
      planetInfo.distanceFromSun = 0.0f;
      planetInfos[i] = planetInfo;
    }

    ImageView sunImageView = new ImageView(getContext());

    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
        (int)(256 * getContext().getResources().getDisplayMetrics().density),
        (int)(256 * getContext().getResources().getDisplayMetrics().density));
    int yOffset = (int)(20 * getContext().getResources().getDisplayMetrics().density);
    lp.topMargin = -lp.height / 2 + + yOffset;
    lp.leftMargin = -lp.width / 2;
    sunImageView.setLayoutParams(lp);
    addView(sunImageView);
    Picasso.with(getContext())
        .load(ImageHelper.getStarImageUrl(getContext(), star, 256, 256))
        .into(sunImageView);

    placePlanets();
  }

  /** Selects the planet at the given index. */
  public void selectPlanet(int planetIndex) {
    selectedPlanet = planetInfos[planetIndex].planet;
    updateSelection();
  }

  public int getSelectedPlanetIndex() {
    if (selectedPlanet == null) {
      return -1;
    }
    return star.planets.indexOf(selectedPlanet);
  }

  private float getDistanceFromSun(int planetIndex) {
    int width = getWidth();
    if (width == 0) {
      return 0.0f;
    }

    width -= (int)(16 * getContext().getResources().getDisplayMetrics().density);
    float planetStart = 150 * getContext().getResources().getDisplayMetrics().density;
    float distanceBetweenPlanets = width - planetStart;
    distanceBetweenPlanets /= planetInfos.length;
    return planetStart + (distanceBetweenPlanets * planetIndex) + (distanceBetweenPlanets / 2.0f);
  }

  private void placePlanets() {
    if (planetInfos == null) {
      return;
    }
    int width = getWidth();
    if (width == 0) {
      this.post(this::placePlanets);
      return;
    }

    for (int i = 0; i < planetInfos.length; i++) {
      PlanetInfo planetInfo = planetInfos[i];

      float distanceFromSun = getDistanceFromSun(i);
      float x = distanceFromSun;
      float y = 0;

      float angle = (0.5f/(planetInfos.length + 1));
      angle = (float) ((angle*(planetInfos.length - i - 1)*Math.PI) + angle*Math.PI);

      Vector2 centre = new Vector2(x, y);
      centre.rotate(angle);
      centre.y += (int)(20 * getContext().getResources().getDisplayMetrics().density);

      planetInfo.centre = centre;
      planetInfo.distanceFromSun = distanceFromSun;
      planetInfo.imageView = new ImageView(getContext());

      RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
          (int)(64 * getContext().getResources().getDisplayMetrics().density),
          (int)(64 * getContext().getResources().getDisplayMetrics().density));
      lp.topMargin = (int) centre.y - (lp.height / 2);
      lp.leftMargin = (int) centre.x - (lp.width / 2);
      planetInfo.imageView.setLayoutParams(lp);
      planetInfo.imageView.setTag(planetInfo);
      planetInfo.imageView.setOnClickListener(planetOnClickListener);
      ViewCompat.setTransitionName(planetInfo.imageView, "planet_icon_" + i);
      addView(planetInfo.imageView);

      Picasso.with(getContext())
          .load(ImageHelper.getPlanetImageUrl(getContext(), star, i, 64, 64))
          .into(planetInfo.imageView);

      if (planetInfo.planet.colony != null) {
        for (Building building : planetInfo.planet.colony.buildings) {
        //  BuildingDesign design = (BuildingDesign) DesignManager.i.getDesign(DesignKind.BUILDING, building.getDesignID());
        //  if (design.showInSolarSystem()) {
        //    planetInfo.buildings.add((Building) building);
        //  }
        }
      }

      //if (!planetInfo.buildings.isEmpty()) {
      //  Collections.sort(planetInfo.buildings, mBuildingDesignComparator);
      //}
    }

    updateSelection();
  }

  private void updateSelection() {
    if (selectedPlanet != null) {
      if (selectionIndicator.getWidth() == 0) {
        // If it doesn't have a width, make it visible then re-update the selection once it's width
        // has been calculated.
        selectionIndicator.setVisibility(View.VISIBLE);
        selectionIndicator.post(this::updateSelection);
        return;
      }

      RelativeLayout.LayoutParams params =
          (RelativeLayout.LayoutParams) selectionIndicator.getLayoutParams();
      params.leftMargin =
          (int) (planetInfos[selectedPlanet.index].centre.x - (selectionIndicator.getWidth() / 2));
      params.topMargin =
          (int) (planetInfos[selectedPlanet.index].centre.y - (selectionIndicator.getHeight() / 2));
      selectionIndicator.setLayoutParams(params);
      selectionIndicator.setVisibility(View.VISIBLE);
    } else {
      selectionIndicator.setVisibility(View.GONE);
    }

    if (planetSelectedHandler != null) {
      planetSelectedHandler.onPlanetSelected(selectedPlanet);
    }
  }

  private final View.OnClickListener planetOnClickListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      PlanetInfo planetInfo = (PlanetInfo) v.getTag();
      selectedPlanet = planetInfo.planet;
      updateSelection();
    }
  };

  private final ViewBackgroundGenerator.OnDrawHandler onBackgroundDrawHandler =
      new ViewBackgroundGenerator.OnDrawHandler() {
    @Override
    public void onDraw(Canvas canvas) {
      for (int i = 0; i < planetInfos.length; i++) {
        float radius = getDistanceFromSun(i);
        float y = 20.0f * getContext().getResources().getDisplayMetrics().density;
        canvas.drawCircle(0.0f, y, radius, orbitPaint);
      }
    }
  };

  /** This class contains info about the planets we need to render and interact with. */
  private static class PlanetInfo {
    public Planet planet;
    public Vector2 centre;
    public float distanceFromSun;
    public ImageView imageView;
  }
}
