package taxi;

import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.ui.Tab;
import org.osbot.rs07.event.WebWalkEvent;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

@ScriptManifest(author = "E.T.A.", name = "City Walker", version = 1.0, info = "Walks to the selected location", logo = "")
public class Cities extends Script {

    private boolean guiDone = false;
    private String selectedCity = null;

    private final Map<String, Area> cityLocations = new HashMap<String, Area>() {{
        put("Varrock", new Area(3210, 3424, 3220, 3434));
        put("Lumbridge", new Area(3222, 3218, 3232, 3228));
        put("Falador", new Area(2964, 3377, 2974, 3387));
        put("Ardougne", new Area(2661, 3305, 2671, 3315));
        put("Camelot", new Area(2757, 3477, 2767, 3487));
    }};

    @Override
    public void onStart() {
        showGUI();
    }

    @Override
    public int onLoop() throws InterruptedException {
        if (!guiDone || selectedCity == null) {
            return 100;
        }

        if (cityLocations.containsKey(selectedCity)) {
            log("Walking to: " + selectedCity);
            WebWalkEvent walkEvent = new WebWalkEvent(cityLocations.get(selectedCity));
            execute(walkEvent);
            onExit();
        } else {
            log("Invalid city selected.");
        }

        return 100;
    }

    private void showGUI() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("City Walker");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(300, 200);
            JPanel panel = new JPanel();

            for (String city : cityLocations.keySet()) {
                JButton button = new JButton(city);
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        selectedCity = city;
                        guiDone = true;
                        frame.dispose();
                    }
                });
                panel.add(button);
            }

            frame.add(panel);
            frame.setVisible(true);
        });
    }

    @Override
    public void onExit() {
        log("Exiting cities script...");
        stop(false);
    }
}
