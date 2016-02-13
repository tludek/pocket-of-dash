package pl.ludex.smartdashwallet.event;

import org.bitcoinj.core.Coin;

/**
 * Created by Tomasz Ludek on 13/02/2016.
 */
public class BalanceChangeEvent {

    private Coin newBalance;
    private Coin prevBalance;

    public BalanceChangeEvent(Coin prevBalance, Coin newBalance) {
        this.prevBalance = prevBalance;
        this.newBalance = newBalance;
    }

    public Coin getPrevBalance() {
        return prevBalance;
    }

    public Coin getNewBalance() {
        return newBalance;
    }
}
