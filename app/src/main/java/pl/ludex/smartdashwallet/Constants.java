package pl.ludex.smartdashwallet;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import pl.ludex.smartdashwallet.BuildConfig;

/**
 * Created by Tomasz Ludek on 17/02/2016.
 */
public class Constants {

    public static final boolean TEST_MODE = BuildConfig.TEST_MODE;

    /** Network this wallet is on (e.g. testnet or mainnet). */
    public static final NetworkParameters NETWORK_PARAMETERS = TEST_MODE ? TestNet3Params.get() : MainNetParams.get();
}
