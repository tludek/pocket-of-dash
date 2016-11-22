package pl.ludex.smartdashwallet.dash;

import org.bitcoinj.core.Transaction;

public interface DashKitServiceListener {

    void loadingBlockchainProgress(int percent);

    void onCoinsReceived(Transaction tx);

    void onCoinsSent(Transaction tx);
}
