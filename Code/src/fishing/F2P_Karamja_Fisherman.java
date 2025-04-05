package Code.src.fishing;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.model.Item;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.model.RS2Object;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.event.ScriptExecutor;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;
import utils.InventoryListener;
import utils.Tracker;
import utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;

import static utils.Rand.*;

@ScriptManifest(
    name = "Karamja Fisherman (F2P)",
    author = "E.T.A.",
    version = 1.0,
    info = "Fishes, cooks and sells lobsters/swordfish in Karamja (Free-to-Play)",
    logo = ""
)
public class F2P_Karamja_Fisherman extends Script implements FishingBotInterface {
    private final String NAME = "Karamja Fisherman";
    private final double VERSION = 1.0;
    private final int MIN_DELAY = 750;
    /**
     * The amount of gold points required before the bot will head to karamja to start fishing
     */
    private final int FULL_GP_REQ = 60;
    private static final Area PORT_SARIM_COOKING_RANGE = new Area(3015, 3240, 3019, 3236);
    private static final Area PORT_SARIM_FISHING_SHOP = new Area(3011, 3225, 3016, 3222);
    private Instant endAFK = null;
    private Tracker tracker;
    private InventoryListener inventoryListener;
    private ScriptExecutor script;
    private String status;
    private double progress;
    private boolean isAFK = false;

    // GUI interface variables
    private FishingGUI menu;
    private boolean settingsMode = false;
    private boolean isCooking = true;
    private int sellQuantity = 50;

    private final Area KARAMJA_FISHING_DOCK = new Area(2919, 3183, 2928, 3173);
    private final Area PORT_SARIM_DEPOSIT_BOX_AREA = new Area(3043, 3237, 3049, 3234);

    private static final Area FISHING_SPOT = new Area(3105, 3430, 3101, 3426);
    private static final Area EDGEVILLE_COOKING_FIRE = new Area(3098, 3427, 3096, 3425);
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
    private final Area DRAYNOR_VILLAGE = new Area(3072, 3254, 3102, 3222);
    private final Area EDGEVILLE_BANK = new Area(3091, 3497, 3094, 3488);

    @Override
    public void onStart() {
        // welcome message
        setStatus("Starting " + NAME + " script...");
        // welcome message
        setStatus("Loading bot menu...");
        // invoke script menu
        SwingUtilities.invokeLater(() -> menu = new FishingGUI(this));
        // set method provider in order to utilize utils package
        Utils.setMethodProvider(getBot().getMethods());
        // initialize script executor to pause/play script (client-side) via code or GUI
        script = getBot().getScriptExecutor();
        // initialize inventory listener to track collected items
        //inventoryListener = new InventoryListener();
        // set tracker for informative onscreen overlay
        tracker = new Tracker(
            NAME,
            Arrays.asList(Skill.FISHING, Skill.COOKING),
            Arrays.asList("Swordfish", "Tuna")
        );
    }

    /**
     * Gets the remaining randomized AFK time to display on-screen for the user
     *
     * @Return A String value denoting the remaining randomized AFK time in seconds.
     */
    public String getRemainingAFK() {
        // calc and return remaining fake afk time as a string
        Duration d = Duration.between(Instant.now(), endAFK);
        // if the player is afk, display fake afk timer
        if (isAFK && d.getSeconds() > 0) {
            return "Waiting " + d.getSeconds() + "s...";
        }
        // else return nothing so no timer is displayed
        return "";
    }

    /**
     * Check if the player is currently within the preset "Karamja Fishing Dock" zone.
     *
     * @return True if the player is within the marked zone, else returns false.
     */
    private boolean isAtKaramjaDock() {
        return KARAMJA_FISHING_DOCK.contains(myPlayer());
    }

    /**
     * Checks if the player has the correct amount of coins to charter the karamja boat based on the players location
     * and their current inventory. This includes use cases such as the player being on karamja without coins, or with
     * coins and no fishing equipment, etc.
     *
     * @return True if the player has enough coins to afford all necessary charters, else returns false.
     */
    private boolean hasCharterFare() throws InterruptedException {
        // check if player has any coins in inventory
        Item coins = inventory.getItem("Coins");
        if (coins == null)
            return false;

        // fetch players current coin amount
        int currentCoinAmount = inventory.getItem("Coins").getAmount();
        // if the player is already in karamja
        if (isAtKaramjaDock()) {
            // and they have the fishing gear required for this task
            if (hasFishingGear())
                // and they also have enough coins for 1 boat ride back
                return currentCoinAmount >= FULL_GP_REQ / 2;
            else
                // else if they need fishing gear and have enough coins for 3 boat rides
                return currentCoinAmount >= FULL_GP_REQ * 1.5;
        } else {
            // else if the player is not in karamja but has enough coins for 2 boat rides
            return currentCoinAmount >= FULL_GP_REQ;
        }
    }

    /**
     * Check if the player has all the required fishing gear in their inventory for the current task.
     *
     * @return True if the player currently has all the required fishing gear in their inventory, else returns false.
     */
    private boolean hasFishingGear() throws InterruptedException {
        setStatus("Checking fishing equipment...", false);
        // get required fishing equipment
        HashMap<String, Integer> requiredFishingGear = new HashMap<>();
                //TODO: Consider revising this code into an enum and linking with GUI toggles to change fishing preference
                //requiredFishingGear.put("Lobster pot", 301); // Lobster pot
                requiredFishingGear.put("Harpoon", 311);

        // for each required item in the requiredFishingGear hashmap
        for (Map.Entry<String, Integer> entry : requiredFishingGear.entrySet()) {
            // get each items name and id
            String name = entry.getKey();
            int id = entry.getValue();

            // ensure player has this required item equipped or in their inventory
            if (!inventory.contains(id) && !equipment.contains(id)) {
                setStatus("Unable to find " + name + " in players inventory.");
                onExit();
                return false;
            }

            //TODO: Consider adding logic to ensure wearable items are equipped
        }
        return true;
    }

    /**
     * Enables/disables 'Settings Mode', pausing the script and allowing the user
     * to adjust some of the script settings before resuming.
     */
    @Override
    public void toggleSettingsMode() throws InterruptedException {
        // toggle settings mode and update user
        this.settingsMode = !this.settingsMode;
        setStatus("Settings mode " + (settingsMode ? "enabled" : "disabled") + "! Pausing on next iteration...");

        // pause/resume script based on whether settings mode is enabled
        if (!settingsMode) {
            // changes status to explain delay
            setStatus("Script will automatically continue on next random tick, please wait...");
            script.resume();
            onLoop();
        } else {
            script.pause();
            //TODO: consider pausing trackers here, xp rate and time keep ticking
        }
    }
    //TODO: Fix bug with being in mems world on f2p and logged out cancelling script


    @Override
    public int onLoop() throws InterruptedException {
        if (settingsMode)
            setStatus("Settings mode has been " + (settingsMode ? "enabled" : "disabled") + "! Pausing script...");
        else setStatus("Working...",false);

        //TODO: Fix/implement inventory tracker
        // track inventory changes
        //inventoryListener.checkInventoryChanges(this);

        // if the player has no fishing gear
        if (!hasFishingGear()) {
            //TODO: Implement logic to determine and fetch required fishing gear based on GUI settings
            log("Checking fishing equipment...");
        }

        if (!hasCharterFare()) {
            //TODO: Implement logic to fetch/collect enough coins for the required charter (e.g., bananas or bank)
        }

        // if the player currently has a full inventory
        if (isFullInv()) {
            // check if there is any food to cook
            if(hasRawFood()) {
                // if the player has chosen to cook their catch
                if (isCooking) {
                    // and the player is near the port sarim cooking range
                    if (PORT_SARIM_COOKING_RANGE.contains(myPlayer())) {
                        // cook food
                        cookFish();
                    } else {
                        // else walk to port sarim cooking range
                        walkTo(PORT_SARIM_COOKING_RANGE, "Port Sarim cooking range");
                        // return short delay to prevent player getting stuck in random afk at each door
                        return getRand(231, 925);
                    }
                // else if player does not wish to cook their catch
                } else {
                    // sell food
                    sellFood();
                }
            } else {
                depositFood();
            }
        } else {
            setStatus("Checking valid fishing location...", false);
            if (isAtKaramjaDock()) {
                //TODO: Revise logic to reference hash map in hasFishingGear function + add GUI functionality
                if (hasHarpoon())
                    fishHarpoon();
                if (hasCage())
                    fishCage();
            } else {
                walkTo(KARAMJA_FISHING_DOCK, "Karamja fishing dock");
                // set a max afk time of 5 seconds
                return(getRand(5));
            }
        }

        // if the user wants to change the script settings, pause the script until it is manually resumed
        if (settingsMode) {
            setStatus("Script has been paused!");
            // TODO: Remove this reference at the end of the string, just there for testing purposes
            // return 0 second delay to ensure instant pause
            return 0;
        }

        // 50% chance to start fake AFK
        if (getRand(1) == 1) {
            isAFK = true;
            // set a random fake AFK time
            int delay = getRand(29893);
            // set the AFK timer
            endAFK = Instant.now().plusMillis(delay);
            // wait for fake AFK delay to end
            return delay;
        }

        // else skip fakeAFK this iteration
        isAFK = false;
        //endAFK = null;
        return getRandShortDelayInt();
    }

    public void sellFood() throws InterruptedException {
        setStatus("Checking player location...");
        if (!PORT_SARIM_FISHING_SHOP.contains(myPlayer())) {
            walkTo(PORT_SARIM_FISHING_SHOP, "Gerrant's shop");
        }

        //TODO: Make this it's own function at some point
        // return early if no raw food was found
        if (!hasRawFood()) {
            setStatus("Unable to find raw food!");
            return;
        }

        setStatus("Searching for Gerrant...");
        NPC gerrant = getNpcs().closest("Gerrant");
        if (gerrant != null && gerrant.interact("Trade")) {
            setStatus("Found Gerrant! Opening shop...");
            new ConditionalSleep(5000) {
                @Override
                public boolean condition() {
                    return getStore().isOpen();
                }
            }.sleep();

            List<String> soldItems = new ArrayList<>();

            setStatus("Attempting to sell raw food...");
            if (getStore().isOpen()) {
                // filter inventory for any raw food and sell it
                for (Item item : inventory.getItems()) {
                    // ignore empty inventory slots
                    if (item == null)
                        continue;

                    // get the name of this item
                    String name = item.getName();
                    // if this item is raw food and has not already been sold...
                    if (name.startsWith("Raw ") && !soldItems.contains(name)) {
                        setStatus("Selling " + name + "...");
                        // sell 50 of each raw food as it is found to speed up selling process
                        getStore().sell(name, 50);
                        // add this item
                        soldItems.add(name);
                        sleep(getRand(232, 4034));
                    }
                }
                getStore().close();
            }
        }
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
     * Check if the player has a harpoon in their inventory
     *
     * @return True if the player has a harpoon in their inventory, else returns false
     */
    private boolean hasHarpoon() {
        return getInventory().getItem("Harpoon") != null;
    }

    /**
     * Check if the player has a cage in their inventory
     *
     * @return True if the player has a cage in their inventory, else returns false
     */
    private boolean hasCage() {
        return getInventory().getItem("Cage") != null;
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

        // draw status + afk time
        String remainingAFK = getRemainingAFK();
        String broadcast = ("Status: " + (remainingAFK.isEmpty() ? status : remainingAFK));
        g.drawString(broadcast, 20, 140);

        // draw progress circle
        drawProgressCircle(g, 20, 250, 35, progress / 100);

        // update item tracker
        tracker.draw(g);
    }

    @Override
    public void onExit() throws InterruptedException {
        menu.close();
        setStatus("Exiting script...");
        stop(false);
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
    public void walkTo(Area area, String status) {
        // return early if the player is already at the destination
        if (area.contains(myPlayer()))
            return;

        // update the status if any status message was passed
        if (!status.isEmpty())
            setStatus(String.format("Travelling to %s...", status), false);

        // walk to the passed area
        if (getWalking().webWalk(area)) {
            // AFK for a random amount of time up to 5.2s, checking timeout & condition every 0.3-2.6s
            new ConditionalSleep(getRand(5231), getRand(324, 2685)) {
                @Override
                public boolean condition() {
                    // walk until player reaches edgeville bank
                    return !area.contains(myPlayer());
                }
            }.sleep();
        }
    }

    private void fishCage() throws InterruptedException {
        // TODO: remove setStatus debugging message here once it's served its purpose
        // prevent action cancelling
        if (myPlayer().isAnimating()) {
            setStatus("Fishing skipped! Player is still busy...", false);
        return;
    }

        // ensure lobster pot is still in player inventory
        if (!getInventory().contains("Lobster pot")) {
            setStatus("Attempting to find lost lobster pot...");
            // TODO: Write logic to find nearby items or search general stores etc for them
            setStatus("Unable to find lobster pot, fetching a new one...");
            // TODO: Write logic to search bank for pot or coins
            onExit();
        }

        // fetch nearest cage/harpoon fishing spot
        NPC fishingSpot = getNpcs().closest(1522);

        // validate fishing spot
        if (fishingSpot == null) {
            setStatus("Unable to find a valid fishing spot.");
            onExit();
        }

        // start fishing
        setStatus("Attempting to cage lobsters...");
        fishingSpot.interact("Cage");

        setStatus("Player is fishing...");
        // start randomized conditional sleep
        new ConditionalSleep(getRandLongDelayInt(), getRandShortDelayInt()) {
            @Override
            public boolean condition() throws InterruptedException {
                // stop sleeping early if player is not animating during check
                return !myPlayer().isAnimating();
            }
        }.sleep();
    }

    private void fishHarpoon() throws InterruptedException {
        // TODO: remove setStatus here
        // prevent action cancelling
        if (myPlayer().isAnimating()) {
            setStatus("Fishing skipped! Player is still busy...", false);
            return;
        }

        // ensure lobster pot is still in player inventory
        if (!getInventory().contains("Harpoon")) {
            setStatus("Attempting to find lost harpoon...");
            // TODO: Write logic to find nearby items or search general stores etc for them
            setStatus("Unable to find harpoon, fetching a new one...");
            // TODO: Write logic to search bank for pot or coins
            onExit();
        }

        // fetch nearest cage/harpoon fishing spot
        Optional<NPC> harpoonSpot = getNpcs().getAll().stream()
                .filter(Objects::nonNull) // ensure spot isn't null
                .filter(o -> o.hasAction("Harpoon")) // filter spots by action
                .min((a, b) -> getMap().distance(a.getPosition()) - getMap().distance(b.getPosition())); // get closest

        // validate fishing spot
        if (!harpoonSpot.isPresent()) {
            setStatus("Unable to find a valid fishing spot...");
            // TODO: Write logic to fix this use case
            onExit();
        }

        // start fishing
        setStatus("Attempting to harpoon...", false);
        harpoonSpot.ifPresent(npc -> npc.interact("Harpoon"));
        setStatus("Player is harpoon fishing...", false);
        // start randomized conditional sleep
        new ConditionalSleep(getRandLongDelayInt(), getRandShortDelayInt()) {
            @Override
            public boolean condition() {
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
        Item rawFishy = getInventory().getItem(item -> item.getName().startsWith("Raw "));
        Item log = getInventory().getItem(item -> item.getName().endsWith("Log"));
        Item tinderbox = getInventory().getItem("Tinderbox");

        // ensure there is raw food in the inventory before continuing
        if (rawFishy == null) {
            setStatus("Unable to find any raw food in inventory... Calling onExit()");
            onExit();
            return;
        }

//        setStatus("Checking for log and tinderbox...");
//        if (log == null) {
//            setStatus("No logs found!");
//            getLogs();
//        }

        setStatus("Checking location...");
        RS2Object range = getObjects().closest("Range");
        // use the raw food on the nearby fire
        if (range == null) {
            setStatus("Unable to find range! Check player is not lost...");
            onExit();
            return;
        }

        setStatus("Using food on range...");
        rawFishy.interact("Use");
        sleep(getRand(892));
        range.interact();

        setStatus("Waiting for interface...");
        // TODO: Implement anti-bot for this instant sleep cancellation after cooking has finished
        new ConditionalSleep(getRand(534, 1231)) {
            @Override
            public boolean condition() {
                return getDialogues().isPendingOption();
            }
        }.sleep();

        setStatus("Selecting chat option...");

        // TODO: Implement anti-bot for this instant sleep cancellation after cooking has finished
        new ConditionalSleep(getRandShortDelayInt()) {
            @Override
            public boolean condition() {
                // return only when the chat options widget appears
                return getWidgets().get(270, 14) != null;
            }
        }.sleep();

        // get the chat options widget
        RS2Widget chatOptions = getWidgets().get(270, 14);
        // TODO: Remove this debugging statement
        log("Chat Options: " + chatOptions);

        // if the "Cook" chat option is available
        if (chatOptions.isVisible()) {
            setStatus("Attempting to \"Cook\" raw food...");
            chatOptions.interact("Cook");
        } else {
            setStatus("Unable to detect the cooking options interface.");
        }

        setStatus("Player is cooking...");
        // Wait until the player finishes cooking
        new ConditionalSleep(getRandLongDelayInt(), getRand(2539, 4393)) {
            @Override
            public boolean condition() {
                // stop waiting when there is no raw food left
                return getInventory().contains(rawFishy.getName());
            }
        }.sleep();

        setStatus("Finished cooking!");
    }

    private void depositFood() throws InterruptedException {
        if (PORT_SARIM_DEPOSIT_BOX_AREA.contains(myPlayer())) {
            RS2Object depositBox = getObjects().closest("Bank deposit box");
            if (depositBox == null) {
                setStatus("Error locating deposit box!");
                onExit();
                return;
            }

            if (depositBox.isVisible()) {
                getDepositBox().open();
                // for each item that can be deposited
                for (Item item : getDepositBox().getItems()) {
                    // if current item slot is not empty, get the name of the item in lowercase
                    if (item != null) {
                        String itemName = item.getName().toLowerCase();

                        // TODO: Consider making a better data structure for this and allowing user to select 'banked items' using GUI later
                        // if this item is a banked item
                        if (itemName.contains("fish") || itemName.equals("lobster") ||
                                itemName.equals("shark") || itemName.equals("tuna") ||
                                itemName.equals("salmon") || itemName.equals("trout") ||
                                itemName.equals("bass") || itemName.equals("pike") ||
                                itemName.equals("herring")) {

                            // deposit all of this item type
                            getDepositBox().depositAll(item.getName());
                        }
                    }
                }

                getDepositBox().close();
                new ConditionalSleep(2000, 600) {
                    @Override
                    public boolean condition() {
                        return (!getDepositBox().isOpen());
                    }
                }.sleep();
            }
        } else {
            // travel to deposit box
            walkTo(PORT_SARIM_DEPOSIT_BOX_AREA, "Port Sarim deposit box");
        }
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
        // check inventory for any kind of axe
        Item axe = getInventory().getItem(item -> item.getName().endsWith(" axe"));

        // check worn equipment for any kind of axe
        if (axe == null)
            axe = getEquipment().getItem(item -> item.getName().endsWith(" axe"));

        return axe == null;
    }

    /**
     * Check if the player has a tinderbox in their inventory.
     *
     * @return A boolean value that is true if the player has a tinderbox in their inventory, else returns false.
     */
    private boolean hasTinderbox(Item log) {
        Item tinderbox = getInventory().getItem(item -> item.getName().equals("Tinderbox"));
        return tinderbox == null;
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

//NPC fishingSpot = getNpcs().closest(new Filter<NPC>() {
//    @Override
//    public boolean match(NPC n) {
//        return (n.hasAction("Cage") /* Other options if needed */);
//    }
//});
