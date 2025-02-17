package fishing;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.event.ScriptExecutor;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;
import utils.Tracker;
import utils.Utils;

import javax.swing.*;
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
public class F2P_Karamja_Fisherman extends Script implements FishingBotInterface {
    private final String NAME = "Karamja Fisherman";
    private final double VERSION = 1.0;
    private final int MIN_DELAY = 750;
    private final Area DRAYNOR_VILLAGE = new Area(3072, 3254, 3102, 3222);
    private final Area EDGEVILLE_BANK = new Area(3091, 3497, 3094, 3488);
    private final Area KARAMJA_FISHING_DOCK = new Area(2919, 3183, 2928, 3173);
    private Instant startTime;
    private Tracker tracker;
    private int delay;

    private String status;
    private double progress;
    private boolean settingsMode = true;

    private static final Area FISHING_SPOT = new Area(3105, 3430, 3101, 3426);
    private static final Area EDGEVILLE_COOKING_FIRE = new Area(3098, 3427, 3096, 3425);
    private static final Area PORT_SARIM_COOKING_RANGE = new Area(3015, 3240, 3019, 3236);
    private static final Area PORT_SARIM_BATTLEAXE_SHOP = new Area(3023, 3253, 3030, 3245);
    private static final Area PORT_SARIM_RUNE_SHOP = new Area(3011, 3261, 3016, 3256);
    private static final Area PORT_SARIM_BAR = new Area(3044, 3259, 3055, 3255);
    private static final Area PORT_SARIM_SPIRIT_TREE_PATCH = new Area(3056, 3259, 3064, 3255);
    private static final Area BANK_AREA = new Area(3092, 3490, 3094, 3496);
    private static final Area PORT_SARIM_KARAMJA_CHARTER = new Area(3026, 3221, 3029, 3216);
    private static final Area PORT_SARIM_ENTRANA_CHARTER = new Area(3044, 3237, 3049, 3234);
    private static final Area PORT_SARIM_JEWELERY_SHOP = new Area(3011, 3249, 3015, 3244);
    private static final Area PORT_SARIM_PRISON = new Area(3010, 3195, 3011, 3188);
    private static final Area PORT_SARIM_CHURCH = new Area(2996, 3179, 2998, 3176);
    private static final Area PORT_SARIM_GOBLINS = new Area(2996, 3179, 2998, 3176);
    private static final Area MUDSKIPPER_POINT = new Area(2999, 3133, 3002, 3129);
    private static final Area SKILLCAPE_MASTER_SMITHING = new Area(2994, 3146, 3002, 3141);
    private static final Area ASGARNIAN_ICE_DUNGEON_ENTRANCE = new Area(3006, 3150, 3009, 3148);
    private static final Area QUEST_START_PIRATES_TREASURE = new Area(3050, 3252, 3054, 3254);

    @Override
    public void onStart() {
        setStatus("Initializing " + NAME + "...");
        checkInv();

        setStatus("Starting " + NAME + " script...");
        SwingUtilities.invokeLater(() -> new FishingGUI(this));
        Utils.setMethodProvider(getBot().getMethods());
        tracker = new Tracker(
                NAME,
                Collections.singletonList(Skill.FISHING),
                Collections.singletonList("Lobster")
        );
    }

    private void checkInv() {
        try {
            setStatus("Checking inventory...");

            // required equipment
            Item coins = inventory.getItem("Coins");
            Item tinderbox = inventory.getItem("Tinderbox");
            Item lobsterPot = inventory.getItem("Lobster pot");
            Item harpoon = inventory.getItem("Harpoon");

            // raw foods
            Item rawLobster = inventory.getItem("Raw lobster");
            Item rawTuna = inventory.getItem("Raw tuna");
            Item rawSwordfish = inventory.getItem("Raw swordfish");

            // clue bottles
            Item clueBottle = inventory.getItem("Clue bottle (beginner)");

            if (coins != null && tinderbox != null) {
                if (coins.getAmount() > 30) {
                    setStatus("All required items found!");
                    if (lobsterPot != null) {
                        setStatus("Cage fishing detected!");
                    } else if (harpoon != null) {
                        setStatus("Harpoon fishing detected!");
                    } else {
                        setStatus("No suitable fishing equipment was found!");
                        //TODO: Write logic to fetch suitable fishing equipment based on player fishing level
                    }
                } else {
                    setStatus("Not enough coins found!");
                    // TODO: Write logic to fetch coins from bank, and if not enough coins in bank, write logic to obtain
                }

            } else {
                if (tinderbox == null) {
                    // TODO: Replace stop with logic to fetch a tinderbox
                    String todo = "Replace stop with logic to fetch a tinderbox";
                    log(todo);
                    stop(false);
                } else if (coins.getAmount() > 30 && !KARAMJA_FISHING_DOCK.contains(myPlayer())) {
                    setStatus("Not enough coins to charter!");
                    // TODO: Replace stop with logic to fetch some coins
                    String todo = "Replace stop with logic to fetch some coins";
                    log(todo);
                    stop(false);
                }
            }
        } catch (Exception e) {
            setStatus("Error: " + e.getMessage());
        }

    }

    /**
     * Enables/disables 'Settings Mode', pausing the script and allowing the user
     * to adjust some of the script settings before resuming.
     */
    @Override
    public void toggleSettingsMode() throws InterruptedException {
        // toggle settings mode and update user
        this.settingsMode = !this.settingsMode;
        setStatus("Settings mode has been " + (settingsMode ? "enabled." : "disabled."));

        // shorthand script executor for tidier condition statements below
        ScriptExecutor script = getBot().getScriptExecutor();
        // pause/resume script based on whether settings mode is enabled
        if (!settingsMode) {
            // changes status to explain delay
            setStatus("Script will automatically continue on next random tick, please wait...");
            script.resume();
            onLoop();
        } else {
            script.pause();
        }
    }
//TODO: Fix bug with being in mems world on f2p and logged out cancelling script

//    @Override
//    public void startFishing(String location, String method, boolean dropFish, boolean bankFish) throws InterruptedException {
//        log("Fishing at: " + location);
//        log("Using method: " + method);
//        log("Drop fish: " + dropFish);
//        log("Bank fish: " + bankFish);
//
//        // Implement the logic to move the player to the selected location and start fishing
//        if (location.equalsIgnoreCase("Karamja Fishing Dock")) {
//            walkTo(KARAMJA_FISHING_DOCK);
//            fishLobbies();
//        }
//    }

    @Override
    public int onLoop() throws InterruptedException {
        // set a random delay between each loop for increased randomization between actions
        int delay = getRand(32384);

        // if settings mode was enabled in gui, pause script until it is resumed
        if (settingsMode) {
            // TODO: Remove this reference at the end of the string, just there for testing purposes
            setStatus("Script paused! Settings mode: " + (settingsMode ? "enabled." : "disabled."));
            return delay;
        }

        // check inventory
        checkInv();
        if (isFullInv()) {
            setStatus("Inventory is full!");
            if (hasRawFood()) {
                cookFish();
            }
        } else {
            setStatus("Checking player location...");
            if (KARAMJA_FISHING_DOCK.contains(myPlayer())) {
                fishLobbies();
            } else {
                setStatus("Travelling to Karamja fishing dock...");
                walkTo(KARAMJA_FISHING_DOCK);
            }
        }

        if (settingsMode) {
            setStatus(String.format("Pausing script in %d seconds...", delay / 1000));
        } else {
            setStatus(String.format("Restarting loop in %d seconds...", delay / 1000));
        }

        return delay;
    }

    /**
     * Check if the players inventory is full. This function will update the script status about a full inventory.
     *
     * @return True if the players inventory is full, else returns false.
     */
    private boolean isFullInv() {
        // if inventory is not full, return false
        if (!getInventory().isFull())
            return false;

        // else update status and return true
        setStatus("Inventory is full!");
        return true;
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

    @Override
    public void onExit() {
        setStatus("Exiting script...");


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

        setStatus("Player is fishing...");
        // start randomized conditional sleep
        new ConditionalSleep(getRandMax(), getRandMin()) {
            @Override
            public boolean condition() throws InterruptedException {
                // stop sleeping early if player is not animating during check
                return !myPlayer().isAnimating();
            }
        }.sleep();
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
        setStatus("Attempting to cook raw food...");
        Item fishy = getInventory().getItem(item -> item.getName().startsWith("Raw "));
        Item log = getInventory().getItem(item -> item.getName().endsWith("Log"));
        Item tinderbox = getInventory().getItem("Tinderbox");

        // ensure there is raw food in the inventory before continuing
        if (fishy == null) {
            setStatus("Unable to find any raw food in inventory... Calling onExit()");
            onExit();
            return;
        }

//        setStatus("Checking for log and tinderbox...");
//        if (log == null) {
//            setStatus("No logs found!");
//            getLogs();
//        }

        if (PORT_SARIM_COOKING_RANGE.contains(myPlayer())) {
            // use the raw food on the nearby fire
            fishy.interact("Use");
            getObjects().closest("Range").interact("Use");
            keyboard.pressKey();


            // TODO: Implement anti-bot for this instant sleep cancellation after cooking has finished
            // pause for a random time or until player has stopped cooking
            new ConditionalSleep(getRandMax(), getRandMin()) {
                @Override
                public boolean condition() throws InterruptedException {
                    return !hasRawFood();
                }
            }.sleep();
            setStatus("Stage 13");

            // drop any burnt fish
            dropBurntFish();
            setStatus("Stage 14");
        } else {
            getWalking().webWalk(PORT_SARIM_COOKING_RANGE);
            setStatus("Stage 15");
        }
        setStatus("Stage 16");
    }

    private void getLogs() {
        setStatus("Fetching logs...");

    }

    /**
     * Check if the player possess any kind of axe for cutting trees down.
     *
     * @return A boolean value that is true if the player has an item ending with " axe",
     * equipped or in their inventory, else returns false.
     */
    private boolean hasAxe() {
        Item axe = getInventory().getItem(item -> item.getName().endsWith(" axe"));

        if (axe == null)
            axe = getEquipment().getItem(item -> item.getName().endsWith(" axe"));

        return axe == null;
    }

    /**
     * Check if the player possess any kind of axe for cutting trees down.
     *
     * @return A boolean value that is true if the player has an item ending with " axe",
     * equipped or in their inventory, else returns false.
     */
    private boolean useTinderbox() {
        Item axe = getInventory().getItem(item -> item.getName().endsWith(" axe"));
        return axe == null;
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
