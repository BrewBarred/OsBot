package taxi;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

@ScriptManifest(author = "E.T.A.", name = "City Walker", version = 1.2, info = "Walks to popular locations around Gielinor.", logo = "")
public class Cities3 extends Script {

    private boolean guiDone = false;
    private String selectedLocation = null;
    private boolean isMemberWorld = false;
    private int walkSpeed = 300;
    private final LinkedList<String> recentLocations = new LinkedList<>();
    private JPanel recentLocationsPanel;

    @Override
    public void onStart() {
        isMemberWorld = getWorlds().isMembersWorld();
        showGUI();
    }

    @Override
    public int onLoop() throws InterruptedException {
        if (!guiDone || selectedLocation == null) {
            return 100;
        }

        if (allLocations.containsKey(selectedLocation)) {
            log("Walking to: " + selectedLocation);
            if (!getWalking().webWalk(allLocations.get(selectedLocation))) {
                log("Failed to navigate to: " + selectedLocation);
            }
            addRecentLocation(selectedLocation);
            updateRecentLocationsPanel(recentLocationsPanel);
            guiDone = false;
        } else {
            log("Invalid location selected.");
        }

        return walkSpeed;
    }

    private void showGUI() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("City Walker");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(600, 450);
            JTabbedPane tabbedPane = new JTabbedPane();

            tabbedPane.add("Cities", createLocationPanel(cities, f2pCities));
            tabbedPane.add("Banks", createLocationPanel(banks, f2pBanks));
            tabbedPane.add("Training Areas", createLocationPanel(trainingAreas, f2pTrainingAreas));
            tabbedPane.add("Minigames", createLocationPanel(minigames, f2pMinigames));

            recentLocationsPanel = createRecentLocationsPanel();
            tabbedPane.add("Recent", recentLocationsPanel);

            frame.add(tabbedPane);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private JPanel createLocationPanel(Map<String, Area> p2pLocations, Map<String, Area> f2pLocations) {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        Map<String, Area> relevantLocations = isMemberWorld ? p2pLocations : f2pLocations;

        for (String location : relevantLocations.keySet()) {
            JButton button = new JButton(location);
            button.addActionListener(e -> {
                selectedLocation = location;
                guiDone = true;
            });
            panel.add(button);
        }
        return panel;
    }

    private JPanel createRecentLocationsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.setBorder(BorderFactory.createTitledBorder("Recent Locations"));
        updateRecentLocationsPanel(panel);
        return panel;
    }

    private void updateRecentLocationsPanel(JPanel panel) {
        panel.removeAll();

        for (String loc : recentLocations) {
            JButton btn = new JButton(loc);
            btn.addActionListener(e -> {
                selectedLocation = loc;
                guiDone = true;
            });
            panel.add(btn);
        }

        panel.revalidate();
        panel.repaint();
    }

    private void addRecentLocation(String location) {
        recentLocations.remove(location);
        recentLocations.addFirst(location);

        if (recentLocations.size() > 5) {
            recentLocations.removeLast();
        }
    }

    private final Map<String, Area> cities = new HashMap<String, Area>() {{
        put("Varrock", new Area(3210, 3424, 3220, 3434));
        put("Lumbridge", new Area(3222, 3218, 3232, 3228));
        put("Falador", new Area(2964, 3377, 2974, 3387));
        put("Camelot", new Area(2757, 3477, 2767, 3487));
        put("Edgeville", new Area(3085, 3492, 3095, 3502));
    }};

    private final Map<String, Area> f2pCities = new HashMap<>(cities);
    {
        f2pCities.remove("Camelot");
    }

    private final Map<String, Area> banks = new HashMap<String, Area>() {{
        put("Grand Exchange", new Area(3161, 3483, 3171, 3493));
        put("Draynor Village Bank", new Area(3089, 3240, 3099, 3250));
    }};

    private final Map<String, Area> f2pBanks = new HashMap<>(banks);

    private final Map<String, Area> trainingAreas = new HashMap<String, Area>() {{
        put("Rock Crabs", new Area(2670, 3700, 2680, 3710));
        put("Al Kharid Warriors", new Area(3290, 3170, 3300, 3180));
    }};

    private final Map<String, Area> f2pTrainingAreas = new HashMap<>(trainingAreas);
    {
        f2pTrainingAreas.remove("Rock Crabs");
    }

    private final Map<String, Area> minigames = new HashMap<String, Area>() {{
        put("Wintertodt", new Area(1628, 3940, 1638, 3950));
        put("Pest Control", new Area(2656, 2635, 2666, 2645));
        put("Barbarian Assault", new Area(2520, 3570, 2530, 3580));
        put("Castle Wars", new Area(2440, 3085, 2450, 3095));
    }};

    private final Map<String, Area> f2pMinigames = new HashMap<String, Area>() {{
        put("Stronghold of Security", new Area(3080, 3420, 3090, 3430));
    }};

    private final Map<String, Area> allLocations;

    {
        allLocations = new HashMap<>();
        List<Map<String, Area>> locationLists = Arrays.asList(
                cities, f2pCities, banks, f2pBanks, trainingAreas, f2pTrainingAreas, minigames, f2pMinigames
        );

        for (Map<String, Area> locationList : locationLists) {
            allLocations.putAll(locationList);
        }
    }
}
