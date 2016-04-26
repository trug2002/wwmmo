package au.com.codeka.warworlds.client;

import android.os.Bundle;

import com.google.common.base.Preconditions;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;

import au.com.codeka.warworlds.client.activity.BaseFragmentActivity;
import au.com.codeka.warworlds.client.opengl.RenderSurfaceView;
import au.com.codeka.warworlds.client.welcome.WarmWelcomeFragment;
import au.com.codeka.warworlds.common.Log;

public class MainActivity extends BaseFragmentActivity {
  private static final Log log = new Log("MainActivity");

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    createFragmentTransitionManager(R.id.fragment_container);

    RenderSurfaceView renderSurfaceView =
        (RenderSurfaceView) Preconditions.checkNotNull(findViewById(R.id.render_surface));
    renderSurfaceView.setRenderer();

    if (savedInstanceState == null) {
     // if (!GameSettings.i.getBoolean(GameSettings.Key.WARM_WELCOME_SEEN)) {
        getFragmentTransitionManager().replaceFragment(WarmWelcomeFragment.class);
     // } else {
     //   getFragmentTransitionManager().replaceFragment(WelcomeFragment.class);
     // }
    }

    WebSocketFactory factory = new WebSocketFactory();
    try {
      WebSocket ws = factory.createSocket("ws://192.168.1.3:8080/conn");
      ws.addListener(new ServerWebSocketListener());
      //ws.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
      ws.connectAsynchronously();
    } catch (IOException e) {
      //tv.setText(e.getMessage());
    }
  }
}
