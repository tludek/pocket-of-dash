package pl.ludex.smartdashwallet;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import pl.ludex.smartdashwallet.dash.DashKitService;
import pl.ludex.smartdashwallet.dash.DashKitServiceListener;

public class BaseActivity extends AppCompatActivity implements DashKitServiceListener {

    private DashKitService dashKitService;
    private boolean dashKitServiceBound = false;

    public boolean isDashKitServiceBound() {
        return dashKitServiceBound;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, DashKitService.class);
        bindService(intent, dashKitServiceConnection, Context.BIND_AUTO_CREATE);
//        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
//        EventBus.getDefault().unregister(this);
        if (dashKitServiceBound) {
            unbindService(dashKitServiceConnection);
        }
        super.onStop();
    }

    private ServiceConnection dashKitServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            DashKitService.Binder binder = (DashKitService.Binder) service;
            dashKitService = binder.getService();
            if (dashKitService.isReady()) {
                Address receiveAddress = dashKitService.freshReceiveAddress();
//                descriptionView.setText(receiveAddress.toString());
            }
            dashKitServiceBound = true;
            dashKitService.setDashKitServiceListener(BaseActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            dashKitServiceBound = false;
            dashKitService = null;
        }
    };

    @Override
    public void loadingBlockchainProgress(final int percent) {
        Toast.makeText(BaseActivity.this, "loading " + percent + "%", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCoinsReceived(Transaction tx) {
        Toast.makeText(BaseActivity.this, "onCoinsReceived " + dashKitService.getBalance(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCoinsSent(Transaction tx) {
        Toast.makeText(BaseActivity.this, "onCoinsSent " + dashKitService.getBalance(), Toast.LENGTH_SHORT).show();
    }
}
