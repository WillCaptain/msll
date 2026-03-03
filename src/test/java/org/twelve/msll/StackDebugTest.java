package org.twelve.msll;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.twelve.msll.parser.MsllParser;
import org.twelve.msll.parserbuilder.MyParserBuilder;

public class StackDebugTest {
    @Test
    @SneakyThrows
    void debug_single_parse() {
        MyParserBuilder builder = new MyParserBuilder("outlineParser.gm", "outlineLexer.gm");
        String code = """
                let me = {
                let age = 40;
                {{{{{
                    age: age,
                    name: { first: "Will", last: "Zhang" },
                    friends: ["Evan": {
                        age: 20,
                        name: { first: "Evan", last: "Zhang" }
                    }],
                    make_friend: friend -> this.friends.put(friend.name[0], friend)
                }}}}}
            };
            me.make_friend({
                name: ("Noble", "Zhang"),
                age: 10
            });
            {{{{
                let more = 100;
                me.friends.get("Noble").age + me.age + more
            }}}}""";
        // 447k token single parse
        StringBuilder longCode = new StringBuilder();
        for (int i = 0; i < 1000; i++) longCode.append(code);
        System.out.println("Building 447k parser...");
        MsllParser<?> parser = builder.createParser(longCode.toString());
        System.out.println("Parsing 447k tokens...");
        long t0 = System.currentTimeMillis();
        parser.parse();
        System.out.println("447k parse done: " + (System.currentTimeMillis()-t0) + "ms, maxStack=" + parser.maxStackSize() + ", totalStack=" + parser.totalStackSize());
    }
}
