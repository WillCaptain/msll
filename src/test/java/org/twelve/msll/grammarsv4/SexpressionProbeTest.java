package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.tools.G4GrammarLoader;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SexpressionProbeTest {
    @Test
    void parses_minimal_sexpr() throws Exception {
        Path root = Path.of(getClass().getClassLoader()
                .getResource("grammars-v4/sexpression").toURI());
        String g4 = Files.readString(root.resolve("sexpression.g4"));
        var loaded = G4GrammarLoader.loadG4String(null, g4);
        MyParser p = loaded.builder.createParser(new StringReader("(a 1)"));
        assertNotNull(p.parse());
    }
}
