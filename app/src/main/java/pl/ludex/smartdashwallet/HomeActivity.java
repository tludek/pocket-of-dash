package pl.ludex.smartdashwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.CoinDefinition;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.CoinSelector;
import org.bitcoinj.wallet.DefaultCoinSelector;
import org.bitcoinj.wallet.InstantXCoinSelector;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pl.ludex.smartdashwallet.dash.DashKitService;

public class HomeActivity extends BaseActivity {

    @BindView(R.id.progress_bar)
    ProgressBar progressBarView;

    @BindView(R.id.balanceText)
    TextView balanceText;

    @BindView(R.id.inAddressEdit)
    TextView inAddressEdit;

    @BindView(R.id.outAddressEdit)
    TextView outAddressEdit;

    @BindView(R.id.amountEdit)
    TextView amountEdit;

    @BindView(R.id.instantSendCheck)
    CheckBox instantSendCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);
    }

    @Override
    public void onDashServiceConnected(DashKitService dashKitService) {
        super.onDashServiceConnected(dashKitService);
        if (dashKitService.isReady()) {
            refreshBalance();
        }
    }

    @Override
    public void onCoinsReceived(Transaction tx) {
        super.onCoinsReceived(tx);
        refreshBalance();
    }

    private void refreshBalance() {
        DashKitService dashKitService = getDashKitService();
        Coin balance = dashKitService.getBalance();
        balanceText.setText(balance.toFriendlyString());
        Address inAddress = dashKitService.currentReceiveAddress();
        inAddressEdit.setText(inAddress.toString());
        Log.d("Wallet", "currentReceiveAddress: " + inAddress.toString());
    }

    @OnClick(R.id.start_main_activity_button)
    void onStartMainActivityButtonClick() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.sendButton)
    void onSendCoinsClick() {

    }

//    private static Output[] buildSimplePayTo(final Coin amount, final Address address) {
//        return new Output[] { new Output(amount, ScriptBuilder.createOutputScript(address)) };
//    }

    @Override
    public void loadingBlockchainProgress(int percent) {
        super.loadingBlockchainProgress(percent);
        progressBarView.setProgress(percent);
    }
}
