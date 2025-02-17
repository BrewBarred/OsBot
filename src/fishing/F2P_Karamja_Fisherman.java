package fishing;

import com.sun.istack.internal.NotNull;
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.Entity;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.script.MethodProvider;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;
import utils.Tracker;
import utils.Utils;

import java.awt.*;
import java.time.Instant;
import java.util.Collections;

import static utils.Rand.*;

@ScriptManifest(
    name = "Karamja Fisherman (F2P)",
    author = "E.T.A.",
    version = 1.0, info = "Fishes, cooks and sells lobsters/swordfish in Karamja (Free-to-Play)",
    logo = ""
)
public class F2P_Karamja_Fisherman extends Script {
    private final String NAME = "Karamja Fisherman";
    private final double VERSION = 1.0;
    private final int MIN_DELAY = 750;
    private final Area DRAYNOR_VILLAGE = new Area(3072, 3254, 3102, 3222);
    private final Area EDGEVILLE_BANK = new Area(3091, 3497, 3094, 3488);
    private final Area KARAMJA_FISHING_DOCK = new Area(2919, 3183, 2928, 3173);
    //private final MethodProvider methodProvider = Utils.getMethodProvider();
    private Instant startTime;
    private Tracker tracker;
    private int delay;

    private String status;
    private double progress;

    private static final Area FISHING_SPOT = new Area(3105, 3430, 3101, 3426);
    private static final Area COOKING_FIRE = new Area(3098, 3427, 3096, 3425);
    private static final Area BANK_AREA = new Area(3092, 3490, 3094, 3496);

    @Override
    public void onStart() {
        log("Starting " + NAME + " script...");
        Utils.setMethodProvider(getBot().getMethods());
        tracker = new Tracker(
                NAME,
                Collections.singletonList(Skill.FISHING),
                Collections.singletonList("Lobster")
        );
    }

    @Override
    public int onLoop() throws InterruptedException {
        // set a random delay between each loop for increased randomization between actions
        int delay = getRand(5682, 12384);
        setStatus("Checking inventory...");
        if (getInventory().isFull()) {
            if (hasRawFood()) {
                setStatus("Inventory is full, attempting to cook raw food...");
                cookFish();
            }
        } else {
            setStatus("Inventory is not yet full!");
            setStatus("Validating player location...");
            if (KARAMJA_FISHING_DOCK.contains(myPlayer())) {
                setStatus("Fishing location has been validated!");
                fishLobbies();
                return delay;
            } else {
                setStatus("Travelling to Karamja fishing dock...");
                walkTo(KARAMJA_FISHING_DOCK);
            }
        }

        setStatus(String.format("Waiting %d seconds...", delay / 1000));
        return delay;
    }

    /**
     * Draw overlay to display character and script information over the client
     * @param g The graphics object to paint
     */
    @Override
    public void onPaint(Graphics2D g) {
        int width = 450;
        int height = 150;
        Color textColor = Color.WHITE;
        Font textFontTitle = new Font("Arial", Font.BOLD, 12);
        Font textFontNormal = new Font("Arial", Font.PLAIN, 12);

        // set antialiasing for smoother graphics
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw translucent background (black with 50% opacity)
        g.setColor(new Color(0, 0, 0, 128));
        g.fillRoundRect(0,  0, width, height, 15, 15);

        // set text properties to title font
        g.setColor(textColor);
        g.setFont(textFontTitle);

        // draw title
        g.drawString(NAME + " " + VERSION, 20, 50);

        // set text properties to normal font
        g.setFont(textFontNormal);

        // draw player position
        int x = myPlayer().getX();
        int y = myPlayer().getY();
        g.drawString("Current Position = X: " + x + ", Y: " + y, 20, 70);

        // draw status
        g.drawString("Status: " + status, 20, 140);

        // draw progress circle
        drawProgressCircle(g, 20, 250, 35, progress / 100);

        // update item tracker
        tracker.draw(g);
    }

//    /**
//     * Checks if there are any players nearby the rock.
//     *
//     * @param rock The RS2Object rock.
//     * @return True if no players are nearby; otherwise, false.
//     */
//    private boolean noPlayerNearby(RS2Object rock) {
//        return methodProvider.getPlayers().closest(player ->
//                player != null && !player.equals(methodProvider.myPlayer()) &&
//                        player.getPosition().distance(rock.getPosition()) <= 1
//        ) == null;
//    }

//    // Open bank if not already open
//        if (!methodProvider.getBank().isOpen()) {
//        // Interact with bank booth or banker
//        methodProvider.getBank().open();
//
//        // Wait until bank is open
//        if(!Sleep.sleep(5000, () -> methodProvider.getBank().isOpen())){
//            methodProvider.getBank().open();
//            return Sleep.sleep(5000, () -> methodProvider.getBank().isOpen());
//        }
//        else{
//            return true;
//        }
//    }

    /**
     * Updates the overlay status for the users information and optionally logs the status update to the client too
     * @param status The current status of the bot i.e., "Checking inventory space..."
     * @param log True if the status update should be logged to the client logger, else false.
     */
    private void setStatus(String status, boolean log) {
        // update status
        status = status;

        // only log status if passed boolean is true
        if (log)
            log(status);
    }

    /**
     * Walks the player to the passed area using the web-walk function in conjunction with a random position function
     * which for more human-like behaviour.
     *
     * @param area The area in which the player should walk toward.
     */
    public void walkTo(Area area) {
        if (area.contains(myPlayer()))
            return;

        if (getWalking().webWalk(area)) {
            new ConditionalSleep(getRand(1500, 2500), getRand(5000)) {
                @Override
                public boolean condition() throws InterruptedException {
                    System.out.println("Assessing condition... " + area.contains(myPlayer()));
                    // walk until player reaches edgeville bank
                    return !area.contains(myPlayer());
                }
            }.sleep();
        }
    }

    private void fishLobbies() throws InterruptedException {
        // TODO: remove setStatus here
        // prevent action cancelling
        if (myPlayer().isAnimating())
            setStatus("Unable to fish, player is busy...");

        // fetch nearest cage/harpoon fishing spot
        NPC fishingSpot = getNpcs().closest(1522);
        if (fishingSpot == null) {
            setStatus("Unable to find a valid fishing spot.");
            stop(false);
        }

        // ensure lobster pot is still in player inventory
        if (!getInventory().contains("Lobster pot")) {
            setStatus("Unable to find lobster pot! Exiting script...");
            stop(false);
        }

        // start fishing
        setStatus("Attempting to cage lobsters...");
        fishingSpot.interact("Cage");

        // start a timer to measure sleep length
        long startTime = System.currentTimeMillis();

        setStatus("Fishing, please wait...");
        // start sleep
        new ConditionalSleep(getRandMax(), getRandMin()) {
            @Override
            public boolean condition() throws InterruptedException {
                // stop sleeping early if player is not animating during check
                return !myPlayer().isAnimating();
            }
        }.sleep();
        // report sleep time
        setStatus(String.format("Player stopped fishing after %2d milliseconds.", System.currentTimeMillis() - startTime));
    }

/**
 * Check if the players inventory contains any raw food.
 *
 * @return True if the players inventory contains anything beginning with the string "Raw ", else returns false.
 */
private boolean hasRawFood() {
        return getInventory().contains(item -> item.getName().startsWith("Raw "));
    }


    /**
     * Updates the status of the script for the users information
     * and logs any updates to the console for debugging purposes
     *
     * @param update The text used to describe the updated process of the script
     */
    private void setStatus(String update) {
        status = update;
        log(status);
    }

    private void cookFish() throws InterruptedException {
        Item fishy = getInventory().getItem(item -> item.getName().startsWith("Raw "));

        if (fishy == null) {
            setStatus("Failed to cook food! Unable to find valid inventory ids...");
            onExit();
            return;
        }

        setStatus(String.format("Attempting to cook %s...", fishy.getName()));

        if (myPlayer().isAnimating())
            return;

        if (COOKING_FIRE.contains(myPlayer())) {
            // sleep for a random time or until the player stops animating
            new ConditionalSleep(getRand(529, 1253)) {
                @Override
                public boolean condition() throws InterruptedException {
                    // if the player to stops cooking, break the sleep early
                    return !myPlayer().isAnimating();
                }
            }.sleep();

            // use the raw food on the nearby fire
            fishy.interact("Use");
            getObjects().closest("Fire").interact("Use");
            sleep(random(5000, 7000));
            dropBurntFish();

        } else {
            getWalking().webWalk(COOKING_FIRE);
        }
    }

    private void dropBurntFish() {
        if (getInventory().contains("Burnt fish")) {
            getInventory().dropAll("Burnt fish");
        }

    }

    private void bankCookedFish() throws InterruptedException {
        status = "Banking fish...";
        log(status);
        if (getBank().isOpen()) {
            getBank().depositAll("Trout");
            getBank().depositAll("Salmon");
            getBank().close();
        } else {
            getBank().open();
            sleep(random(1000, 2000));
        }
    }

    // Helper method to draw a circular progress bar
    private void drawProgressCircle(Graphics2D g, int x, int y, int radius, double progress) {
        // Background circle
        g.setColor(new Color(255, 255, 255, 50));
        g.fillOval(Math.min(0, x - radius), y - radius, radius * 2, radius * 2);

        // Progress arc
        g.setColor(Color.GREEN);
        int angle = (int) (360 * progress);
        g.fillArc(x - radius, y - radius, radius * 2, radius * 2, 90, -angle);
        // Outline
        g.setColor(Color.WHITE);
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2);

    }
}
