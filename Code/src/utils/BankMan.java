package Code.src.utils;

import org.osbot.rs07.api.Bank;
import org.osbot.rs07.script.Script;

public class BankMan {
    Script script;

    public BankMan(Script script) {
        this.script = script;
    }

    public Banks getNearestBank() {
        Banks bank = null;
        int distance = Integer.MAX_VALUE;
        for (Banks b : Banks.values()) {
            final int bDistance = b.area.getRandomPosition().distance(script.myPosition());
            if (bDistance < distance) {
                distance = bDistance;
                bank = b;
            }
        }
        return bank;
    }

    /**
     * Searches for the best bank, based on type and distance in a straight line from the players current location.
     * Note: This method will only interact with RS2Objects.
     * @return True if bank was already open, or successfully opened
     */
    public boolean openNearestBank() throws InterruptedException {
        Bank bank = script.getBank();

        // Try walking if we’re not near a bank
        if (bank == null) {
            script.log("No nearby bank, walking to nearest.");
            boolean walked = script.getWalking().webWalk(Banks.getNearest(script.myPosition()));
            if (!walked) {
                script.log("Failed to walk to a bank.");
                return false;
            }

            // Update bank reference after walking
            bank = script.getBank();
            if (bank == null) {
                script.log("Still no bank nearby after walking.");
                return false;
            }
        }

        // Try to open the bank interface
        boolean opened = bank.open();
        if (!opened) {
            script.log("Failed to open bank.");
        }
        return opened;
    }

    /**
     * Closes any open bank interfaces
     * @return True if the close button was found and clicked
     */
    public boolean close() {
        try {
            return script.getBank().close();
        } catch (Exception e) {
            script.log("Failed to close bank interface. Interface = " + script.getBank()
                    + "\n" + e.getMessage());
            return false;
        }
    }
}
