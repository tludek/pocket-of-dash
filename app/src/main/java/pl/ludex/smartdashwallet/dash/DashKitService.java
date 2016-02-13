package pl.ludex.smartdashwallet.dash;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletEventListener;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import pl.ludex.smartdashwallet.event.BalanceChangeEvent;

/**
 * Created by Tomasz Ludek on 13/02/2016.
 */
public class DashKitService extends Service {

    private static final String BIP39_WORDLIST_FILENAME = "bip39-wordlist.txt";

    private static final Logger log = LoggerFactory.getLogger(DashKitService.class);

    final static String defaultWalletAndChainPrefix = "checkpoint";
    final static String defaultWalletExt = ".wallet";
    final static String defaultChainExt = ".spvchain";

    private WalletAppKit walletKit;

    private final IBinder binder = new Binder();
    private DashKitStatus dashKitStatus = DashKitStatus.IDLE;

    public DashKitService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        buildKit();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public boolean isReady() {
        return dashKitStatus == DashKitStatus.READY;
    }

    public Coin getBalance() {
        if (walletKit != null) {
            return walletKit.wallet().getBalance();
        } else {
            return null;
        }
    }

    public Address freshReceiveAddress() {
        return walletKit.wallet().freshReceiveAddress();
    }

    private void buildKit() {
        dashKitStatus = DashKitStatus.INITIALIZATION;
        log.info("DashKit building...");
        createCheckpoint(false);
        initMnemonicCode();
        NetworkParameters params = MainNetParams.get();
        walletKit = new WalletAppKit(params, getFilesDir(), defaultWalletAndChainPrefix) {

            @Override
            protected void onSetupCompleted() {
                dashKitStatus = DashKitStatus.READY;
                // This is called in a background thread after startAndWait is called, as setting up various objects
                // can do disk and network IO that may cause UI jank/stuttering in wallet apps if it were to be done
                // on the main thread.
                Coin balance = wallet().getBalance();
                log.info("dashj successfully started (balance " + balance + ")");

                wallet().addEventListener(mWalletEventListener);
                EventBus.getDefault().post(new BalanceChangeEvent(null, balance));
            }
        };

        walletKit.startAsync();
    }

    private void createCheckpoint(boolean rebuild) {
        File chain = new File(getFilesDir(), defaultWalletAndChainPrefix + ".spvchain");
        OutputStream output = null;
        if (!chain.exists() || rebuild) {
            try {
                log.info("restoring checkpoint");
                boolean created = chain.createNewFile();
                if (!created) {
                    log.info("chain file already existis");
                }
                output = new FileOutputStream(chain);
                InputStream input = getAssets().open("checkpoint.spvchain");
                try {
                    byte[] buffer = new byte[4 * 1024]; // or other buffer size
                    int read;

                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                    output.flush();
                } finally {
                    output.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (output != null) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void initMnemonicCode() {
        try {
            final long start = System.currentTimeMillis();
            MnemonicCode.INSTANCE = new MnemonicCode(getAssets().open(BIP39_WORDLIST_FILENAME), null);
            log.info("BIP39 wordlist loaded from: '" + BIP39_WORDLIST_FILENAME + "', took " +
                    (System.currentTimeMillis() - start) + "ms");
        } catch (final IOException x) {
            throw new Error(x);
        }
    }

    private WalletEventListener mWalletEventListener = new WalletEventListener() {

        @Override
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            log.info("-----> coins resceived: " + tx.getHashAsString());
            log.info("received: " + tx.getValue(wallet));
            EventBus.getDefault().post(new BalanceChangeEvent(prevBalance, newBalance));
        }

        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            log.info("coins sent");
        }

        @Override
        public void onReorganize(Wallet wallet) {

        }

        @Override
        public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
            log.info("-----> confidence changed: " + tx.getHashAsString());
            TransactionConfidence confidence = tx.getConfidence();
            log.info("new block depth: " + confidence.getDepthInBlocks());
        }

        @Override
        public void onWalletChanged(Wallet wallet) {

        }

        @Override
        public void onScriptsChanged(Wallet wallet, List<Script> scripts, boolean isAddingScripts) {
            log.info("new script added");
        }

        @Override
        public void onKeysAdded(List<ECKey> keys) {
            log.info("new key added");
        }
    };

    public class Binder extends android.os.Binder {
        public DashKitService getService() {
            return DashKitService.this;
        }
    }

    public enum DashKitStatus {
        IDLE,
        INITIALIZATION,
        READY
    }
}
