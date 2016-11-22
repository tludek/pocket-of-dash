package pl.ludex.smartdashwallet.dash;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.DownloadProgressTracker;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletEventListener;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.script.Script;
import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import pl.ludex.smartdashwallet.Constants;
import pl.ludex.smartdashwallet.event.BalanceChangeEvent;

public class DashKitService extends Service {

    private static final String BIP39_WORDLIST_FILENAME = "bip39-wordlist.txt";

    private static final Logger log = LoggerFactory.getLogger(DashKitService.class);

    final static String defaultWalletAndChainPrefix = "wallet-protobuf";

    private WalletAppKit walletKit;

    private final IBinder binder = new Binder();
    private DashKitStatus dashKitStatus = DashKitStatus.IDLE;

    private int blockchainLoadingProgress;

    private DashKitServiceListener dashKitServiceListener;

    private Handler handler;

    public DashKitService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
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

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
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

//        final NetworkParameters params = Constants.NETWORK_PARAMETERS;
//        Context walletContext = new Context(params);
//        walletContext.initDash(false, true);

//        walletKit = new WalletAppKit(walletContext, getFilesDir(), defaultWalletAndChainPrefix) {
        walletKit = new WalletAppKit(Constants.NETWORK_PARAMETERS, getFilesDir(), defaultWalletAndChainPrefix) {

            @Override
            protected void onSetupCompleted() {
                dashKitStatus = DashKitStatus.READY;

                Context.propagate(context);

                // This is called in a background thread after startAndWait is called, as setting up various objects
                // can do disk and network IO that may cause UI jank/stuttering in wallet apps if it were to be done
                // on the main thread.
                Coin balance = wallet().getBalance();
                log.info("dashj successfully started (balance " + balance + ")");

                wallet().addEventListener(mWalletEventListener);
//                wallet().addEventListener(mWalletEventListener, Threading.USER_THREAD);
                EventBus.getDefault().post(new BalanceChangeEvent(null, balance));
            }
        };

        walletKit.setBlockingStartup(false);
//        walletKit.setDownloadListener(blockchainDownloadListener);
        walletKit.setDownloadListener(new DownloadProgressTracker() {
            @Override
            protected void progress(double pct, int blocksSoFar, Date date) {
                super.progress(pct, blocksSoFar, date);
                int percentage = (int) Math.round(pct);
                if (percentage != blockchainLoadingProgress) {
                    blockchainLoadingProgress = percentage;
                    publishBlockchainLoadingProgress();
                }
            }
        });

        walletKit.startAsync();
    }

    private void publishBlockchainLoadingProgress() {
        if (dashKitServiceListener != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dashKitServiceListener.loadingBlockchainProgress(blockchainLoadingProgress);
                }
            });
        }
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

            if (dashKitServiceListener != null) {
                dashKitServiceListener.onCoinsReceived(tx);
            }
        }

        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            log.info("coins sent");
            if (dashKitServiceListener != null) {
                dashKitServiceListener.onCoinsReceived(tx);
            }
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

    public void setDashKitServiceListener(DashKitServiceListener dashKitServiceListener) {
        this.dashKitServiceListener = dashKitServiceListener;
        publishBlockchainLoadingProgress();
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

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
