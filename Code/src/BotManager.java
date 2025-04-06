package Code.src;

import Code.src.utils.BagMan;
import Code.src.utils.BankMan;
import Code.src.utils.EquipMan;
import Code.src.utils.TravelMan;
import org.osbot.rs07.script.Script;

/**
 * Main handler for botting scripts, designed to minimize repeated code between scripts for common tasks such as
 * walking, inventory checking & tracking, skill tracking, banking, teleporting and equipment management.
 */
public abstract class BotManager extends Script {
    protected BankMan bank;
    protected BagMan bag;
    protected TravelMan travel;
    protected EquipMan equipMan;

    /**
     * Function used to exe some code before the script starts running, useful for initializing variables and setting
     * the player up with the required equipment
     */
    @Override
    public void onStart() {
        this.bank = new BankMan(this);
        this.bag = new BagMan(this);
        this.travel = new TravelMan(this);
        this.equipMan = new EquipMan(this);
    }

    /**
     * Function used to execute some code before the script stops, useful for disposing, debriefing or chaining.
     */
    @Override
    public void onExit() {

    }

    /**
     * Ensure child classes implement an onLoop function which returns the delay time for the next action.
     * @return The delay time in milliseconds until the next action starts.
     */
    public abstract int onLoop();

    public void moveCameraRandomly() {
        if (random(0, 50) >= 43) {
            log("Perform random camera movement...");
            getCamera().moveYaw(random(0, 360));
            getCamera().movePitch(random(42, 67));
        }
    }

}
