package org.twelve.msll.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * all grmmars in one particular language
 *
 * @author huizi 2024
 */
public class Grammars {
    /**
     * grammar list with non-terminal as the name
     */
    private final Map<String, Grammar> grammars;

    /**
     * root grammar
     * all grammars is constructed  in a tree format
     */
    private final Grammar start;

    /**
     * constructor from grammar list
     *
     * @param grammars grammar list
     */
    public Grammars(Map<String, Grammar> grammars) {
        this.grammars = grammars;
        this.grammars.values().forEach(grammar -> grammar.setGrammars(this));
        this.start = this.grammars.values().stream().filter(g -> g.type().isStart()).findFirst().get();
        this.start.first();
        this.start.follow();
    }

    /**
     * get tht root grammar
     *
     * @return a grammar
     */
    public Grammar getStart() {
        return this.start;
    }

    /**
     * get grammar from non terminal name
     *
     * @param name non terminal name
     * @return a grammar, return null if not found
     */
    public Grammar get(String name) {
        return grammars.get(name);
    }

    /**
     * get all grammars in the language
     *
     * @return whole grammar list
     */
    public List<Grammar> grammars() {
        return new ArrayList<>(grammars.values());
    }
}
