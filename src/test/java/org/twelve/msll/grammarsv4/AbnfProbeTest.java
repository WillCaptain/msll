package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.tools.G4GrammarLoader;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AbnfProbeTest {
    @Test
    void parses_minimal_abnf_rule() throws Exception {
        Path root = Path.of(getClass().getClassLoader()
                .getResource("grammars-v4/abnf").toURI());
        String g4 = Files.readString(root.resolve("Abnf.g4"));
        var loaded = G4GrammarLoader.loadG4String(null, g4);
        MyParser p = loaded.builder.createParser(new StringReader("ALPHA = \"A\"\n"));
        assertNotNull(p.parse());
    }
}
