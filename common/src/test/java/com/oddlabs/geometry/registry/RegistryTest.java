package com.oddlabs.geometry.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryTest {
    @Test
    void roundTripsThroughTheFile(@TempDir Path dir) throws IOException {
        Registry registry = new Registry(
                List.of(new AttachmentEntry("natives", "peon", "hat", true, "peon_pumpkin_hat", 1, "halloween")),
                List.of(new SkinEntry("vikings", "pumpkin", "warrior", "warrior_pumpkin", 3, "halloween")),
                List.of(new DecorationEntry("misc", "mushroom", "grass,dirt,snow", 20, "halloween")),
                List.of(new LightEntry("natives", "armory_pumpkins", 0, "#ff7029", 8f, 1f, 1.25f, -3.5f, 0.75f)));
        Path file = dir.resolve(Registry.FILE);
        registry.write(file);
        try (InputStream in = Files.newInputStream(file)) {
            assertEquals(registry, Registry.read(in));
        }
        assertTrue(Files.readString(file).contains("\"defaultOn\" : true"));
    }

    @Test
    void defaultItemsSortBeforeTheRest() {
        AttachmentEntry on = new AttachmentEntry("natives", "peon", "hat", true, "zz_hat", 1, Registry.NO_EVENT);
        AttachmentEntry off = new AttachmentEntry("natives", "peon", "hat", false, "aa_hat", 1, Registry.NO_EVENT);
        assertTrue(AttachmentEntry.ORDER.compare(on, off) < 0);
    }
}
