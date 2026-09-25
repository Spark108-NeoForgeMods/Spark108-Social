package ru.spark108.social;

import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class SocialConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Spark108-Social/Config");
    private static final Path FILE = FMLPaths.CONFIGDIR.get()
            .resolve("spark108").resolve("social").resolve("config.yml");
    private static boolean spectatorInteraction;
    private static final Map<HaloPosition, HaloAlignment> ALIGNMENTS = new EnumMap<>(HaloPosition.class);

    private SocialConfig() {}

    static void load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (!Files.exists(FILE)) Files.writeString(FILE,
                    "# Allow player interactions and item trades while in spectator mode.\n"
                            + "allow_spectator_interaction: false\n\n"
                            + "# contour, center, left or right; configured independently for each body area.\n"
                            + "halo_labels:\n"
                            + "  head_left: contour\n"
                            + "  head_right: contour\n"
                            + "  body_left: contour\n"
                            + "  body_right: contour\n"
                            + "  legs_left: contour\n"
                            + "  legs_right: contour\n", StandardCharsets.UTF_8);
            ALIGNMENTS.clear();
            for (HaloPosition position : HaloPosition.values()) ALIGNMENTS.put(position, HaloAlignment.CONTOUR);
            List<String> lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
            boolean inHaloLabels = false;
            boolean hasHaloLabels = false;
            java.util.Set<HaloPosition> configured = java.util.EnumSet.noneOf(HaloPosition.class);
            for (String raw : lines) {
                String line = raw.split("#", 2)[0].strip();
                if (line.isEmpty()) continue;
                int indent = raw.length() - raw.stripLeading().length();
                if (indent == 0) {
                    inHaloLabels = line.equals("halo_labels:");
                    if (inHaloLabels) hasHaloLabels = true;
                    if (line.startsWith("allow_spectator_interaction:"))
                        spectatorInteraction = Boolean.parseBoolean(line.substring(line.indexOf(':') + 1).strip());
                } else if (inHaloLabels && indent == 2) {
                    for (HaloPosition position : HaloPosition.values()) {
                        if (line.startsWith(position.configKey() + ":")) {
                            ALIGNMENTS.put(position, HaloAlignment.parse(line.substring(line.indexOf(':') + 1)));
                            configured.add(position);
                        }
                    }
                }
            }
            if (!hasHaloLabels) {
                String original = Files.readString(FILE, StandardCharsets.UTF_8);
                StringBuilder added = new StringBuilder(original.endsWith("\n") ? "\n" : "\n\n");
                added.append("# contour, center, left or right; configured independently for each body area.\n")
                        .append("halo_labels:\n");
                for (HaloPosition position : HaloPosition.values())
                    added.append("  ").append(position.configKey()).append(": contour\n");
                Files.writeString(FILE, added.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            } else if (configured.size() < HaloPosition.values().length) {
                List<String> updated = new java.util.ArrayList<>(lines);
                int section = -1;
                int end = lines.size();
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).strip().equals("halo_labels:")) { section = i; continue; }
                    if (section >= 0 && !lines.get(i).isBlank()
                            && !lines.get(i).stripLeading().startsWith("#")
                            && !Character.isWhitespace(lines.get(i).charAt(0))) { end = i; break; }
                }
                for (HaloPosition position : HaloPosition.values())
                    if (!configured.contains(position)) updated.add(end++, "  " + position.configKey() + ": contour");
                String original = Files.readString(FILE, StandardCharsets.UTF_8);
                Files.writeString(FILE, String.join(original.contains("\r\n") ? "\r\n" : "\n", updated),
                        StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not load {}", FILE, exception);
            spectatorInteraction = false;
        }
    }

    static boolean allows(Player player) {
        return !player.isSpectator() || spectatorInteraction;
    }

    static boolean spectatorInteraction() { return spectatorInteraction; }
    static HaloAlignment alignment(HaloPosition position) {
        return ALIGNMENTS.getOrDefault(position, HaloAlignment.CONTOUR);
    }

    static List<HaloAlignment> alignments() {
        List<HaloAlignment> values = new java.util.ArrayList<>(HaloPosition.values().length);
        for (HaloPosition position : HaloPosition.values()) values.add(alignment(position));
        return List.copyOf(values);
    }

}
