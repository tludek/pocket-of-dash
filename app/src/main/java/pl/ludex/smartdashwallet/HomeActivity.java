package pl.ludex.smartdashwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pl.ludex.smartdashwallet.dash.DashKitService;

public class HomeActivity extends BaseActivity {

    @BindView(R.id.progress_bar)
    ProgressBar progressBarView;

    @BindView(R.id.balance)
    TextView balanceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);
    }

    @Override
    public void onDashServiceConnected() {
        super.onDashServiceConnected();
        DashKitService dashKitService = getDashKitService();
        balanceView.setText(dashKitService.getBalance().toFriendlyString());
    }

    @OnClick(R.id.start_main_activity_button)
    void onStartMainActivityButtonClick() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void loadingBlockchainProgress(int percent) {
        super.loadingBlockchainProgress(percent);
        progressBarView.setProgress(percent);
    }
}
