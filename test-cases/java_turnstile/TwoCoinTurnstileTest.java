import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TwoCoinTurnstileTest {
    private String output = "";
    private TwoCoinTurnstile sm = new TwoCoinTurnstileImpl();

    private class TwoCoinTurnstileImpl extends TwoCoinTurnstile {
        @Override
        public void unhandledTransition(String state, String event) {
            output += String.format("X(%s,%s)", state, event);
        }

        @Override
        public void unlock() {
            output += "U";
        }

        @Override
        public void alarmOn() {
            output += "A";
        }

        @Override
        public void thankyou() {
            output += "T";
        }

        @Override
        public void lock() {
            output += "L";
        }

        @Override
        public void alarmOff() {
            output += "O";
        }
    }

    @Test
    public void normal() {
        sm.Coin();
        sm.Coin();
        sm.Pass();

        assertThat(output, is("UL"));
    }

    @Test
    public void oneCoinAttempt() {
        sm.Coin();
        sm.Pass();

        assertThat(output, is("A"));
    }

    @Test
    public void alarmReset() {
        sm.Pass();
        sm.Reset();

        assertThat(output, is("AOL"));
    }

    @Test
    public void extraCoins() {
        sm.Coin();
        sm.Coin();
        sm.Coin();
        sm.Coin();
        sm.Pass();

        assertThat(output, is("UTTL"));
    }
}
