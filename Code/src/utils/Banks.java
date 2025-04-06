package Code.src.utils;

// Import necessary OSBot classes
import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.script.Script;

/**
 * Enum representing known web-walkable bank locations in OSRS.
 * This makes it easy to access their areas and calculate the closest one.
 */
public enum Banks {

    // Define known bank areas using OSBot's built-in Banks constants
    DRAYNOR(org.osbot.rs07.api.map.constants.Banks.DRAYNOR),
    AL_KHARID(org.osbot.rs07.api.map.constants.Banks.AL_KHARID),
    LUMBRIDGE(org.osbot.rs07.api.map.constants.Banks.LUMBRIDGE_UPPER),
    FALADOR_EAST(org.osbot.rs07.api.map.constants.Banks.FALADOR_EAST),
    FALADOR_WEST(org.osbot.rs07.api.map.constants.Banks.FALADOR_WEST),
    VARROCK_EAST(org.osbot.rs07.api.map.constants.Banks.VARROCK_EAST),
    VARROCK_WEST(org.osbot.rs07.api.map.constants.Banks.VARROCK_WEST),
    //SEERS(Banks.CAMELOT),
    //CATHERBY(Banks.CATHERBY),
    EDGEVILLE(org.osbot.rs07.api.map.constants.Banks.EDGEVILLE),
    //YANILLE(Banks.YANILLE),
    //GNOME_STRONGHOLD(Banks.GNOME_STRONGHOLD),
    //ARDOUNGE_NORTH(Banks.ARDOUGNE_NORTH),
    //ARDOUNE_SOUTH(Banks.ARDOUGNE_SOUTH),
    //CASTLE_WARS(Banks.CASTLE_WARS),
    DUEL_ARENA(org.osbot.rs07.api.map.constants.Banks.DUEL_ARENA);
    //PEST_CONTROL(Banks.PEST_CONTROL),
    //CANIFIS(Banks.CANIFIS),
    //TZHAAR(Banks.TZHAAR);

    // The actual Area object that defines where the bank is on the map
    public final Area area;

    // Constructor for each enum constant
    Banks(Area area) {
        this.area = area;
    }

    /**
     * Returns the Bank closest to the player based on distance.
     *
     * @return The closest Bank enum value
     */
    public static Area getNearest(Position position) {
        Banks closest = null; // Store the closest bank found so far
        int shortest = Integer.MAX_VALUE; // Keep track of the smallest distance

        //TODO: Need to eventually work out a formula or pathfinding function to calculate this with actual pathing
        // get the player's current position to calculate closest bank

        // for each bank listed in the enum
        for (Banks bank : Banks.values()) {
            // Calculate distance from player to the center of the bank area
            int dist = bank.area.getRandomPosition().distance(position);

            // if this bank is closer than the previous closest, update the closest bank
            if (dist < shortest) {
                closest = bank;
                shortest = dist;
            }
        }

        // if the closest bank is not null, return its location as an Area object, else return null
        return closest == null ? null : closest.getArea();
    }

    /**
     * Commands the bot to walk to this bank
     *
     * @param script The current Script instance (provides walking API).
     */
    public void walkTo(Script script) {
        // Use webWalker to navigate to the center of this bank's area.
        script.getWalking().webWalk(this.area.getRandomPosition());
    }

    /**
     * Checks if the player is currently within this bank's area.
     *
     * @param script The current Script instance (provides player position).
     * @return True if the player's position is within this bank's area.
     */
    public boolean hasPlayer(Script script) {
        return this.area.contains(script.myPlayer().getPosition());
    }

    /**
     * Get the full Area object for the bank.
     * This can be used to check if the player is inside the bank, etc.
     *
     * @return The Area that defines the bank location
     */
    public Area getArea() {
        return area;
    }
}
