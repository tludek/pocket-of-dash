package pl.ludex.smartdashwallet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.CoinDefinition;
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
    public void onDashServiceConnected() {
        super.onDashServiceConnected();
        refreshBalance();
    }

    @Override
    public void onCoinsReceived(Transaction tx) {
        super.onCoinsReceived(tx);
        refreshBalance();
    }

    private void refreshBalance() {
        DashKitService dashKitService = getDashKitService();
        Coin balance = dashKitService.getBalance();
        if (balance != null) {
            balanceText.setText(balance.toFriendlyString());
        }
    }

    @OnClick(R.id.start_main_activity_button)
    void onStartMainActivityButtonClick() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.sendButton)
    void onSendCoinsClick() {
        String amountTxt = "0.019";//amountEdit.getText().toString();
        Coin amount;
        try {
            amount = Coin.parseCoin(amountTxt);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Incorrect amount: " + amountTxt, Toast.LENGTH_LONG).show();
            return;
        }
        String toAddressTxt = "Xxstct1gPdddnJHeVxsLSFnuQT1YAyPecY";//outAddressEdit.getText().toString();
        Address toAddress;
        try {
            toAddress = new Address(Constants.NETWORK_PARAMETERS, toAddressTxt);
        } catch (AddressFormatException e) {
            Toast.makeText(this, "Incorrect address: " + toAddressTxt, Toast.LENGTH_LONG).show();
            return;
        }

        boolean useInstantSend = instantSendCheck.isSelected();

        Coin txFee;
        CoinSelector coinSelector;

        Wallet.SendRequest request = Wallet.SendRequest.to(toAddress, amount);

        if (useInstantSend) {
            txFee = Coin.valueOf(CoinDefinition.INSTANTX_FEE);
            coinSelector = new InstantXCoinSelector();
        } else {
            txFee = Coin.valueOf(CoinDefinition.DEFAULT_MIN_TX_FEE);
            coinSelector = new DefaultCoinSelector();
        }

        DashKitService dashKitService = getDashKitService();
        Coin balance = dashKitService.getBalance();

        //if (service.kit.wallet().getBalance(coinSelector).subtract(minFee).isPositive()) {
        if (!amount.isGreaterThan(balance.subtract(txFee))) {


            WalletAppKit walletKit = dashKitService.getWalletKit();
            Wallet wallet = walletKit.wallet();
            try {
                wallet.sendCoins(walletKit.peerGroup(), toAddress, amount);
//                wallet.sendCoins(request);
            } catch (InsufficientMoneyException e) {
                Toast.makeText(this, "Insufficient money", Toast.LENGTH_LONG).show();
            }

//            final Transaction transaction = useInstantSend ? new TransactionLockRequest(Constants.NETWORK_PARAMETERS) : new Transaction(Constants.NETWORK_PARAMETERS);
//            for (final PaymentIntent.Output output : outputs)
//                transaction.addOutput(output.amount, output.script);
//            return Wallet.SendRequest.forTx(transaction);
//
//
//
//            final Transaction transaction = useInstantSend ? new TransactionLockRequest(Constants.NETWORK_PARAMETERS) : new Transaction(Constants.NETWORK_PARAMETERS);
//            for (final PaymentIntent.Output output : outputs)
//                transaction.addOutput(output.amount, output.script);
//            return Wallet.SendRequest.forTx(transaction);
//
//            final Wallet.SendRequest sendRequest = finalPaymentIntent.toSendRequest();
//            sendRequest.useInstantX = usingInstantX;
//            sendRequest.emptyWallet = paymentIntent.mayEditAmount() && finalAmount.equals(wallet.getBalance(Wallet.BalanceType.AVAILABLE));
//            sendRequest.feePerKb = feeCategory.feePerKb;
//            sendRequest.feePerKb = sendRequest.useInstantX ? Coin.valueOf(CoinDefinition.INSTANTX_FEE) : sendRequest.feePerKb;
//
//
//            sendRequest.memo = paymentIntent.memo;
//            sendRequest.exchangeRate = amountCalculatorLink.getExchangeRate();
//            sendRequest.aesKey = encryptionKey;
        }
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
