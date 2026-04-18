package org.twelve.msll.grammarsv4;

import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MyParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Isolates the CSV parse failure so we can see the full error message and
 * iterate on fixes without the harness truncating or re-ordering tests.
 */
public class CsvProbeTest {

    @Test
    void csv_parses_multi_row_file() throws Exception {
        Path root = Path.of(getClass().getClassLoader()
                .getResource("grammars-v4/csv").toURI());
        String g4 = Files.readString(root.resolve("CSV.g4"));

        var loaded = org.twelve.msll.tools.G4GrammarLoader
                .loadG4String(null, g4);

        MyParser p = loaded.builder.createParser(
                new StringReader(Files.readString(root.resolve("samples/simple.csv"))));
        assertNotNull(p.parse());
    }
}
