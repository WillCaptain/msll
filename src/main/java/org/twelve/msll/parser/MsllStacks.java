package org.twelve.msll.parser;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A specialized ArrayList implementation for managing a list of `MsllStack` objects.
 *
 * This class is designed to store and manage multiple instances of `MsllStack` used during the
 * parsing process in the Multi-Stack LL (MSLL) parser. By extending ArrayList, it retains
 * the flexibility and performance of the standard ArrayList while providing more contextual clarity.
 *
 * In MSLL parsing, multiple stacks are needed to handle different parsing paths simultaneously, and
 * this class provides a convenient way to store and iterate over these stacks during parsing operations.
 *
 * @author huizi 2024
 */
public class MsllStacks extends ArrayList<MsllStack> {
    private AtomicInteger maxStackSize = new AtomicInteger(0);
    private AtomicInteger totalStackSize = new AtomicInteger(0);

    public Integer maxStackSize(){
        return this.maxStackSize.get();
    }
    public Integer totalStackSize(){
        return this.totalStackSize.get();
    }
    @Override
    public boolean add(MsllStack parseNodes) {
        boolean result =  super.add(parseNodes);
        this.totalStackSize.incrementAndGet();
        setMaxStackSize();
        return result;
    }

    @Override
    public boolean removeIf(Predicate<? super MsllStack> filter) {
        boolean result =   super.removeIf(filter);
        setMaxStackSize();
        return result;
    }

    @Override
    public MsllStack remove(int index) {
        MsllStack result = super.remove(index);
        setMaxStackSize();
        return result;
    }

    private void setMaxStackSize(){
        if(this.maxStackSize.get()<this.size()){
            this.maxStackSize.set(this.size());
        }
    }
}
