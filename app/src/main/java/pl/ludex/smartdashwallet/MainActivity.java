package pl.ludex.smartdashwallet;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pl.ludex.smartdashwallet.dash.DashKitService;
import pl.ludex.smartdashwallet.event.MainEventBus;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    @Bind(R.id.toolbar)
    Toolbar toolbarView;
    @Bind(R.id.toolbar_progress_bar)
    ProgressBar toolbarProgressBarView;
    @Bind(R.id.balance_panel)
    View balancePanelView;
    @Bind(R.id.balance_dash)
    TextView balanceDashView;
    @Bind(R.id.balance_usd)
    TextView balanceUsdView;
    @Bind(R.id.description)
    TextView descriptionView;

    private DashKitService dashKitService;
    private boolean dashKitServiceBound = false;

    private EventBus eventBus = MainEventBus.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbarView, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initView();
    }

    private void initView() {
        setSupportActionBar(toolbarView);
        setTitle("");
        balancePanelView.setVisibility(View.INVISIBLE);
        toolbarProgressBarView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, DashKitService.class);
        bindService(intent, dashKitServiceConnection, Context.BIND_AUTO_CREATE);
        eventBus.register(this);
    }

    @Override
    protected void onStop() {
        eventBus.unregister(this);
        if (dashKitServiceBound) {
            unbindService(dashKitServiceConnection);
        }
        super.onStop();
    }

    @OnClick(R.id.balance_panel)
    public void onBalancePanelClick(View view) {
        if (dashKitServiceBound) {
            balanceDashView.setText(dashKitService.getBalance().toString());
        } else {
            Snackbar.make(view, "Dash service not bound", Snackbar.LENGTH_LONG)
                    .setAction("Action", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    }).show();
        }
    }

    @Subscribe
    public void balanceChangeEvent(final Coin newBalance) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                balancePanelView.setVisibility(View.VISIBLE);
                toolbarProgressBarView.setVisibility(View.GONE);
                balanceDashView.setText(newBalance.toString());
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private ServiceConnection dashKitServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DashKitService.Binder binder = (DashKitService.Binder) service;
            dashKitService = binder.getService();
            Address receiveAddress = dashKitService.freshReceiveAddress();
            descriptionView.setText(receiveAddress.toString());
            dashKitServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            dashKitServiceBound = false;
            dashKitService = null;
        }
    };
}
